package com.cloudogu.scm.review.comment.service;

import com.cloudogu.scm.review.PermissionCheck;
import com.cloudogu.scm.review.RepositoryResolver;
import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import com.cloudogu.scm.review.pullrequest.service.PullRequestService;
import org.apache.shiro.SecurityUtils;
import sonia.scm.HandlerEventType;
import sonia.scm.NotFoundException;
import sonia.scm.event.ScmEventBus;
import sonia.scm.repository.NamespaceAndName;
import sonia.scm.repository.Repository;
import sonia.scm.security.KeyGenerator;

import javax.inject.Inject;
import java.time.Clock;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

import static com.cloudogu.scm.review.comment.service.CommentTransition.MAKE_COMMENT;
import static com.cloudogu.scm.review.comment.service.CommentTransition.MAKE_TASK;
import static com.cloudogu.scm.review.comment.service.CommentTransition.REOPEN;
import static com.cloudogu.scm.review.comment.service.CommentTransition.SET_DONE;
import static com.cloudogu.scm.review.comment.service.TextTransition.CHANGE_TEXT;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static sonia.scm.ContextEntry.ContextBuilder.entity;
import static sonia.scm.NotFoundException.notFound;
import static sonia.scm.ScmConstraintViolationException.Builder.doThrow;

public class CommentService {

  private final RepositoryResolver repositoryResolver;
  private final PullRequestService pullRequestService;
  private final CommentStoreFactory storeFactory;
  private final KeyGenerator keyGenerator;
  private final ScmEventBus eventBus;
  private final Clock clock;

  @Inject
  public CommentService(RepositoryResolver repositoryResolver, PullRequestService pullRequestService, CommentStoreFactory storeFactory, KeyGenerator keyGenerator, ScmEventBus eventBus) {
    this(repositoryResolver, pullRequestService, storeFactory, keyGenerator, eventBus, Clock.systemDefaultZone());
  }

  CommentService(RepositoryResolver repositoryResolver, PullRequestService pullRequestService, CommentStoreFactory storeFactory, KeyGenerator keyGenerator, ScmEventBus eventBus, Clock clock) {
    this.repositoryResolver = repositoryResolver;
    this.pullRequestService = pullRequestService;
    this.storeFactory = storeFactory;
    this.keyGenerator = keyGenerator;
    this.eventBus = eventBus;
    this.clock = clock;
  }

  public String add(String namespace, String name, String pullRequestId, Comment pullRequestComment) {
    Repository repository = repositoryResolver.resolve(new NamespaceAndName(namespace, name));
    PermissionCheck.checkComment(repository);
    return addWithoutPermissionCheck(repository, pullRequestId, pullRequestComment);
  }

  private String addWithoutPermissionCheck(Repository repository, String pullRequestId, Comment pullRequestComment) {
    initializeNewComment(pullRequestComment);
    String newId = getCommentStore(repository).add(repository, pullRequestId, pullRequestComment);
    PullRequest pullRequest = pullRequestService.get(repository, pullRequestId);
    eventBus.post(new CommentEvent(repository, pullRequest, pullRequestComment, null, HandlerEventType.CREATE));
    return newId;
  }

  public String reply(String namespace, String name, String pullRequestId, String rootCommentId, Reply reply) {
    Repository repository = repositoryResolver.resolve(new NamespaceAndName(namespace, name));
    PermissionCheck.checkComment(repository);
    PullRequest pullRequest = pullRequestService.get(repository, pullRequestId);
    Comment originalRootComment = get(repository, pullRequestId, rootCommentId);
    reply.setId(keyGenerator.createKey());
    initializeNewComment(reply);
    originalRootComment.addReply(reply);

    getCommentStore(repository).update(pullRequestId, originalRootComment);

    eventBus.post(new ReplyEvent(repository, pullRequest, reply, null, HandlerEventType.CREATE));
    return reply.getId();
  }

  private void initializeNewComment(BasicComment pullRequestComment) {
    pullRequestComment.setDate(clock.instant());
    pullRequestComment.setAuthor(getCurrentUserId());
  }

  private String getCurrentUserId() {
    return SecurityUtils.getSubject().getPrincipals().getPrimaryPrincipal().toString();
  }

  public void modifyComment(String namespace, String name, String pullRequestId, String commentId, Comment changedComment) {
    Repository repository = repositoryResolver.resolve(new NamespaceAndName(namespace, name));
    PullRequest pullRequest = pullRequestService.get(repository, pullRequestId);

    Comment rootComment = findRootComment(repository, pullRequestId, commentId)
      .<NotFoundException>orElseThrow(() -> {
          throw notFound(entity(BasicComment.class, String.valueOf(changedComment.getId()))
            .in(PullRequest.class, pullRequestId)
            .in(repository.getNamespaceAndName()));
        }
      );
    PermissionCheck.checkModifyComment(repository, rootComment);
    Comment clone = rootComment.clone();
    rootComment.setComment(changedComment.getComment());
    rootComment.addTransition(CHANGE_TEXT, getCurrentUserId());
    getCommentStore(repository).update(pullRequestId, rootComment);
    eventBus.post(new CommentEvent(repository, pullRequest, rootComment, clone, HandlerEventType.MODIFY));
  }

  public Collection<CommentTransition> possibleTransitions(String namespace, String name, String pullRequestId, String commentId) {
    Repository repository = repositoryResolver.resolve(new NamespaceAndName(namespace, name));
    if (!PermissionCheck.mayComment(repository)) {
      return emptyList();
    }
    Comment comment = get(namespace, name, pullRequestId, commentId);
    switch (comment.getType()) {
      case COMMENT:
        return singleton(MAKE_TASK);
      case TASK_TODO:
        return asList(SET_DONE, MAKE_COMMENT);
      case TASK_DONE:
        return asList(REOPEN, MAKE_COMMENT);
      default:
        throw new IllegalStateException("unknown type in comment: " + comment.getType());
    }
  }

  public void transform(String namespace, String name, String pullRequestId, String commentId, CommentTransition transition) {
    Comment comment = get(namespace, name, pullRequestId, commentId);
    Repository repository = repositoryResolver.resolve(new NamespaceAndName(namespace, name));
    PullRequest pullRequest = pullRequestService.get(repository, pullRequestId);
    Comment clone = comment.clone();
    transition.accept(clone);
    clone.addTransition(transition, getCurrentUserId());
    getCommentStore(repository).update(pullRequestId, clone);
    eventBus.post(new CommentEvent(repository, pullRequest, comment, clone, HandlerEventType.MODIFY));
  }

  public void modifyReply(String namespace, String name, String pullRequestId, String replyId, Reply changedReply) {
    Repository repository = repositoryResolver.resolve(new NamespaceAndName(namespace, name));
    PullRequest pullRequest = pullRequestService.get(repository, pullRequestId);

    findReplyWithParent(repository, pullRequestId, replyId)
      .<NotFoundException>orElseThrow(
        () -> {
          throw notFound(entity(Reply.class, String.valueOf(changedReply.getId()))
            .in(PullRequest.class, pullRequestId)
            .in(repository.getNamespaceAndName()));
        }
      ).execute(
      (parent, reply) -> {
        PermissionCheck.checkModifyComment(repository, reply);
        Reply clone = reply.clone();
        reply.setComment(changedReply.getComment());
        reply.addTransition(CHANGE_TEXT, getCurrentUserId());
        getCommentStore(repository).update(pullRequestId, parent);
        eventBus.post(new ReplyEvent(repository, pullRequest, reply, clone, HandlerEventType.MODIFY));
      }
    );
  }

  public List<Comment> getAll(String namespace, String name, String pullRequestId) {
    Repository repository = repositoryResolver.resolve(new NamespaceAndName(namespace, name));
    PermissionCheck.checkRead(repository);
    return getCommentStore(repository).getAll(pullRequestId);
  }

  public void delete(String namespace, String name, String pullRequestId, String commentId) {
    Repository repository = repositoryResolver.resolve(new NamespaceAndName(namespace, name));
    PullRequest pullRequest = pullRequestService.get(repository, pullRequestId);

    findRootComment(repository, pullRequestId, commentId)
      .ifPresent(
        rootComment -> {
          PermissionCheck.checkModifyComment(repository, rootComment);
          doThrow().violation("Must not delete system comment").when(rootComment.isSystemComment());
          doThrow().violation("Must not delete root comment with existing replies").when(!rootComment.getReplies().isEmpty());
          getCommentStore(repository).delete(pullRequestId, commentId);
          eventBus.post(new CommentEvent(repository, pullRequest, null, rootComment, HandlerEventType.DELETE));
        }
      );
    findReplyWithParent(repository, pullRequestId, commentId)
      .ifPresent(
        replyWithParent -> replyWithParent.execute(
          (parent, reply) -> {
            PermissionCheck.checkModifyComment(repository, reply);
            parent.removeReply(reply);
            getCommentStore(repository).update(pullRequestId, parent);
            eventBus.post(new ReplyEvent(repository, pullRequest, null, reply, HandlerEventType.DELETE));
          }
        )
      );
  }

  public Comment get(String namespace, String name, String pullRequestId, String commentId) {
    Repository repository = repositoryResolver.resolve(new NamespaceAndName(namespace, name));
    return get(repository, pullRequestId, commentId);
  }

  public Comment get(Repository repository, String pullRequestId, String commentId) {
    return findRootComment(repository, pullRequestId, commentId)
      .orElseThrow(() -> notFound(entity(BasicComment.class, String.valueOf(commentId))
        .in(PullRequest.class, pullRequestId)
        .in(repository.getNamespaceAndName())));
  }

  public Reply getReply(String namespace, String name, String pullRequestId, String commentId, String replyId) {
    Comment comment = get(namespace, name, pullRequestId, commentId);
    return comment
      .getReplies()
      .stream()
      .filter(r -> r.getId().equals(replyId))
      .findFirst()
      .orElseThrow(() -> notFound(entity(Reply.class, String.valueOf(replyId))
        .in(Comment.class, commentId)
        .in(PullRequest.class, pullRequestId)
        .in(new NamespaceAndName(namespace, name))));
  }

  private CommentStore getCommentStore(Repository repository) {
    return storeFactory.create(repository);
  }

  public void addStatusChangedComment(Repository repository, String pullRequestId, SystemCommentType commentType) {
    Comment comment = Comment.createSystemComment(commentType.getKey());
    addWithoutPermissionCheck(repository, pullRequestId, comment);
  }

  private Optional<Comment> findRootComment(Repository repository, String pullRequestId, String commentId) {
    return getCommentStore(repository)
      .getAll(pullRequestId)
      .stream()
      .filter(rootComment -> rootComment.getId().equals(commentId))
      .findFirst();
  }

  private Optional<ReplyWithParent> findReplyWithParent(Repository repository, String pullRequestId, String commentId) {
    return streamAllRepliesWithParents(repository, pullRequestId)
      .filter(ReplyWithParent -> ReplyWithParent.reply.getId().equals(commentId))
      .findFirst();
  }

  private Stream<ReplyWithParent> streamAllRepliesWithParents(Repository repository, String pullRequestId) {
    return getCommentStore(repository)
      .getAll(pullRequestId)
      .stream()
      .flatMap(
        rootComment -> rootComment.getReplies().stream().map(reply -> new ReplyWithParent(rootComment, reply))
      );
  }

  private static class ReplyWithParent {

    private final Comment parent;
    private final Reply reply;

    ReplyWithParent(Comment parent, Reply reply) {
      this.parent = parent;
      this.reply = reply;
    }

    void execute(BiConsumer<Comment, Reply> consumer) {
      consumer.accept(parent, reply);
    }
  }
}
