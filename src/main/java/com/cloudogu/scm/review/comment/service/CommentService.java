package com.cloudogu.scm.review.comment.service;

import com.cloudogu.scm.review.PermissionCheck;
import com.cloudogu.scm.review.RepositoryResolver;
import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import com.cloudogu.scm.review.pullrequest.service.PullRequestService;
import com.github.legman.EventBus;
import org.apache.shiro.SecurityUtils;
import sonia.scm.HandlerEventType;
import sonia.scm.repository.NamespaceAndName;
import sonia.scm.repository.Repository;
import sonia.scm.security.KeyGenerator;

import javax.inject.Inject;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static sonia.scm.ContextEntry.ContextBuilder.entity;
import static sonia.scm.NotFoundException.notFound;
import static sonia.scm.ScmConstraintViolationException.Builder.doThrow;

public class CommentService {

  private final RepositoryResolver repositoryResolver;
  private final PullRequestService pullRequestService;
  private final CommentStoreFactory storeFactory;
  private final KeyGenerator keyGenerator;
  private final EventBus eventBus;
  private final Clock clock;

  @Inject
  public CommentService(RepositoryResolver repositoryResolver, PullRequestService pullRequestService, CommentStoreFactory storeFactory, KeyGenerator keyGenerator, EventBus eventBus) {
    this(repositoryResolver, pullRequestService, storeFactory, keyGenerator, eventBus, Clock.systemDefaultZone());
  }

  CommentService(RepositoryResolver repositoryResolver, PullRequestService pullRequestService, CommentStoreFactory storeFactory, KeyGenerator keyGenerator, EventBus eventBus, Clock clock) {
    this.repositoryResolver = repositoryResolver;
    this.pullRequestService = pullRequestService;
    this.storeFactory = storeFactory;
    this.keyGenerator = keyGenerator;
    this.eventBus = eventBus;
    this.clock = clock;
  }

  public String add(String namespace, String name, String pullRequestId, PullRequestRootComment pullRequestComment) {
    Repository repository = repositoryResolver.resolve(new NamespaceAndName(namespace, name));
    PermissionCheck.checkComment(repository);
    return addWithoutPermissionCheck(repository, pullRequestId, pullRequestComment);
  }

  private String addWithoutPermissionCheck(Repository repository, String pullRequestId, PullRequestRootComment pullRequestComment) {
    initializeNewComment(pullRequestComment);
    String newId = getCommentStore(repository).add(repository, pullRequestId, pullRequestComment);
    PullRequest pullRequest = pullRequestService.get(repository, pullRequestId);
    eventBus.post(new CommentEvent(repository, pullRequest, pullRequestComment, null, HandlerEventType.CREATE));
    return newId;
  }

  public String reply(String namespace, String name, String pullRequestId, String rootCommentId, PullRequestComment pullRequestComment) {
    Repository repository = repositoryResolver.resolve(new NamespaceAndName(namespace, name));
    PermissionCheck.checkComment(repository);
    PullRequest pullRequest = pullRequestService.get(repository, pullRequestId);
    PullRequestRootComment originalRootComment = get(repository, pullRequestId, rootCommentId);
    pullRequestComment.setId(keyGenerator.createKey());
    initializeNewComment(pullRequestComment);
    originalRootComment.addReply(pullRequestComment);

    getCommentStore(repository).update(pullRequestId, originalRootComment);

    eventBus.post(new CommentEvent(repository, pullRequest, pullRequestComment, null, HandlerEventType.CREATE));
    return pullRequestComment.getId();
  }

  public void modify(String namespace, String name, String pullRequestId, PullRequestComment changedComment) {
    Repository repository = repositoryResolver.resolve(new NamespaceAndName(namespace, name));
    PullRequest pullRequest = pullRequestService.get(repository, pullRequestId);

    doWithEitherRootCommentOrResponse(
      repository,
      pullRequestId,
      changedComment.getId(),
      rootComment -> {
        PermissionCheck.checkModifyComment(repository, rootComment);
        PullRequestRootComment clone = rootComment.clone();
        rootComment.setComment(changedComment.getComment());
        rootComment.setDone(changedComment.isDone());
        getCommentStore(repository).update(pullRequestId, rootComment);
        eventBus.post(new CommentEvent(repository, pullRequest, rootComment, clone, HandlerEventType.MODIFY));
      },
      responseWithParent -> {
        PullRequestComment response = responseWithParent.response;
        PermissionCheck.checkModifyComment(repository, response);
        PullRequestComment clone = response.clone();
        response.setComment(changedComment.getComment());
        response.setDone(changedComment.isDone());
        getCommentStore(repository).update(pullRequestId, responseWithParent.parent);
        eventBus.post(new CommentEvent(repository, pullRequest, response, clone, HandlerEventType.MODIFY));
      },
      () -> {
        throw notFound(entity(PullRequestComment.class, String.valueOf(changedComment.getId()))
          .in(PullRequest.class, pullRequestId)
          .in(repository.getNamespaceAndName()));
      }
    );
  }

  private void initializeNewComment(PullRequestComment pullRequestComment) {
    pullRequestComment.setDate(clock.instant());
    pullRequestComment.setAuthor(SecurityUtils.getSubject().getPrincipals().getPrimaryPrincipal().toString());
  }

  private void doWithEitherRootCommentOrResponse(
    Repository repository,
    String pullRequestId,
    String commentId,
    Consumer<PullRequestRootComment> rootCommentConsumer,
    Consumer<ResponseWithParent> responseConsumer,
    Runnable notFoundHandler
  ) {
    Optional<PullRequestRootComment> existingRootComment = findRootComment(repository, pullRequestId, commentId);
    if (existingRootComment.isPresent()) {
      rootCommentConsumer.accept(existingRootComment.get());
    } else {
      Optional<ResponseWithParent> existingResponse =
        findResponseWithParent(repository, pullRequestId, commentId);
      if (existingResponse.isPresent()) {
        responseConsumer.accept(existingResponse.get());
      } else {
        notFoundHandler.run();
      }
    }
  }

  public List<PullRequestRootComment> getAll(String namespace, String name, String pullRequestId) {
    Repository repository = repositoryResolver.resolve(new NamespaceAndName(namespace, name));
    return getCommentStore(repository).getAll(pullRequestId);
  }

  private static class ResponseWithParent {
    private final PullRequestRootComment parent;
    private final PullRequestComment response;

    public ResponseWithParent(PullRequestRootComment parent, PullRequestComment response) {
      this.parent = parent;
      this.response = response;
    }
  }

  public void delete(Repository repository, String pullRequestId, String commentId) {
    PullRequest pullRequest = pullRequestService.get(repository, pullRequestId);
    doWithEitherRootCommentOrResponse(
      repository,
      pullRequestId,
      commentId,
      rootComment -> {
        PermissionCheck.checkModifyComment(repository, rootComment);
        doThrow().violation("Must not delete root comment with existing response").when(!rootComment.getResponses().isEmpty());
        getCommentStore(repository).delete(pullRequestId, commentId);
        eventBus.post(new CommentEvent(repository, pullRequest, null, rootComment, HandlerEventType.DELETE));
      },
      responseWithParent -> {
        PermissionCheck.checkModifyComment(repository, responseWithParent.response);
        responseWithParent.parent.removeResponse(responseWithParent.response);
        getCommentStore(repository).update(pullRequestId, responseWithParent.parent);
        eventBus.post(new CommentEvent(repository, pullRequest, null, responseWithParent.response, HandlerEventType.DELETE));
      },
      () -> {}
    );
  }

  public PullRequestRootComment get(String namespace, String name, String pullRequestId, String commentId) {
    Repository repository = repositoryResolver.resolve(new NamespaceAndName(namespace, name));
    return get(repository, pullRequestId, commentId);
  }

  public PullRequestRootComment get(Repository repository, String pullRequestId, String commentId) {
    return findRootComment(repository, pullRequestId, commentId)
      .orElseThrow(() -> notFound(entity(PullRequestComment.class, String.valueOf(commentId))
        .in(PullRequest.class, pullRequestId)
        .in(repository.getNamespaceAndName())));
  }

  private CommentStore getCommentStore(Repository repository) {
    return storeFactory.create(repository);
  }

  public void addStatusChangedComment(Repository repository, String pullRequestId, SystemCommentType commentType) {
    PullRequestRootComment comment = new PullRequestRootComment();
    comment.setSystemComment(true);
    comment.setComment(commentType.getKey());
    addWithoutPermissionCheck(repository, pullRequestId, comment);
  }

  private Optional<PullRequestRootComment> findRootComment(Repository repository, String pullRequestId, String commentId) {
    return getCommentStore(repository)
      .getAll(pullRequestId)
      .stream()
      .filter(rootComment -> rootComment.getId().equals(commentId))
      .findFirst();
  }

  private Optional<ResponseWithParent> findResponseWithParent(Repository repository, String pullRequestId, String commentId) {
    return streamAllResponsesWithParents(repository, pullRequestId)
      .filter(responseWithParent -> responseWithParent.response.getId().equals(commentId))
      .findFirst();
  }

  private Stream<ResponseWithParent> streamAllResponsesWithParents(Repository repository, String pullRequestId) {
    return getCommentStore(repository)
      .getAll(pullRequestId)
      .stream()
      .flatMap(
        rootComment -> rootComment.getResponses().stream().map(response -> new ResponseWithParent(rootComment, response))
      );
  }
}
