/*
 * Copyright (c) 2020 - present Cloudogu GmbH
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Affero General Public License as published by the Free
 * Software Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see https://www.gnu.org/licenses/.
 */

package com.cloudogu.scm.review.comment.service;

import com.cloudogu.scm.review.PermissionCheck;
import com.cloudogu.scm.review.RepositoryResolver;
import com.cloudogu.scm.review.comment.api.MentionMapper;
import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import com.cloudogu.scm.review.pullrequest.service.PullRequestEmergencyMergedEvent;
import com.cloudogu.scm.review.pullrequest.service.PullRequestEvent;
import com.cloudogu.scm.review.pullrequest.service.PullRequestMergedEvent;
import com.cloudogu.scm.review.pullrequest.service.PullRequestRejectedEvent;
import com.cloudogu.scm.review.pullrequest.service.PullRequestReopenedEvent;
import com.cloudogu.scm.review.pullrequest.service.PullRequestService;
import com.cloudogu.scm.review.pullrequest.service.PullRequestStatusChangedEvent;
import com.github.legman.Subscribe;
import com.google.common.base.Strings;
import jakarta.inject.Inject;
import org.apache.shiro.SecurityUtils;
import sonia.scm.EagerSingleton;
import sonia.scm.HandlerEventType;
import sonia.scm.NotFoundException;
import sonia.scm.event.ScmEventBus;
import sonia.scm.plugin.Extension;
import sonia.scm.repository.NamespaceAndName;
import sonia.scm.repository.Repository;
import sonia.scm.security.KeyGenerator;

import java.util.Collection;
import java.util.List;
import java.util.Map;
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

@EagerSingleton
@Extension
public class CommentService {

  private final RepositoryResolver repositoryResolver;
  private final PullRequestService pullRequestService;
  private final CommentStoreFactory storeFactory;
  private final KeyGenerator keyGenerator;
  private final ScmEventBus eventBus;
  private final CommentInitializer commentInitializer;
  private final MentionMapper mentionMapper;

  @Inject
  public CommentService(RepositoryResolver repositoryResolver, PullRequestService pullRequestService, CommentStoreFactory storeFactory, KeyGenerator keyGenerator, ScmEventBus eventBus, CommentInitializer commentInitializer, MentionMapper mentionMapper) {
    this.repositoryResolver = repositoryResolver;
    this.pullRequestService = pullRequestService;
    this.storeFactory = storeFactory;
    this.keyGenerator = keyGenerator;
    this.eventBus = eventBus;
    this.commentInitializer = commentInitializer;
    this.mentionMapper = mentionMapper;
  }

  public String add(String namespace, String name, String pullRequestId, Comment pullRequestComment) {
    Repository repository = repositoryResolver.resolve(new NamespaceAndName(namespace, name));
    PermissionCheck.checkComment(repository);
    return addWithoutPermissionCheck(repository, pullRequestId, pullRequestComment);
  }

  private String addWithoutPermissionCheck(Repository repository, String pullRequestId, Comment pullRequestComment) {
    PullRequest pullRequest = pullRequestService.get(repository, pullRequestId);
    initializeNewComment(pullRequestComment, pullRequest, repository.getId());
    String newId = getCommentStore(repository).add(pullRequestId, pullRequestComment);
    fireMentionEventIfMentionsExist(repository, pullRequest, pullRequestComment);
    eventBus.post(new CommentEvent(repository, pullRequest, pullRequestComment, null, HandlerEventType.CREATE));
    return newId;
  }

  public String reply(String namespace, String name, String pullRequestId, String rootCommentId, Reply reply) {
    Repository repository = repositoryResolver.resolve(new NamespaceAndName(namespace, name));
    PermissionCheck.checkComment(repository);
    PullRequest pullRequest = pullRequestService.get(repository, pullRequestId);
    Comment originalRootComment = get(repository, pullRequestId, rootCommentId);
    reply.setId(keyGenerator.createKey());
    initializeNewComment(reply, pullRequest, repository.getId());
    originalRootComment.addReply(reply);

    getCommentStore(repository).update(pullRequestId, originalRootComment);

    fireMentionEventIfMentionsExist(repository, pullRequest, reply);
    eventBus.post(new ReplyEvent(repository, pullRequest, reply, null, originalRootComment, HandlerEventType.CREATE));
    return reply.getId();
  }

  private void fireMentionEventIfMentionsExist(Repository repository, PullRequest pullRequest, BasicComment comment) {
    if (!comment.getMentionUserIds().isEmpty()) {
      BasicComment parsedReply = mentionMapper.parseMentionsUserIdsToDisplayNames(comment);
      eventBus.post(new MentionEvent(repository, pullRequest, parsedReply, null, HandlerEventType.CREATE));
    }
  }

  private void initializeNewComment(BasicComment comment, PullRequest pullRequest, String repositoryId) {
    comment.setMentionUserIds(mentionMapper.extractMentionsFromComment(comment.getComment()));
    commentInitializer.initialize(comment, pullRequest, repositoryId);
  }

  private String getCurrentUserId() {
    return SecurityUtils.getSubject().getPrincipals().getPrimaryPrincipal().toString();
  }

  public void markAsOutdated(String namespace, String name, String pullRequestId, String commentId) {
    Repository repository = repositoryResolver.resolve(new NamespaceAndName(namespace, name));
    Comment rootComment = findRootComment(repository, pullRequestId, commentId)
      .<NotFoundException>orElseThrow(() -> {
          throw notFound(entity(BasicComment.class, commentId)
            .in(PullRequest.class, pullRequestId)
            .in(repository.getNamespaceAndName()));
        }
      );
    if (rootComment.getType() == CommentType.COMMENT && !rootComment.isOutdated()) {
      rootComment.setOutdated(true);
      getCommentStore(repository).update(pullRequestId, rootComment);
    }
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
    handleMentions(repository, pullRequest, rootComment, clone);
    rootComment.addTransition(new ExecutedTransition<>(keyGenerator.createKey(), CHANGE_TEXT, System.currentTimeMillis(), getCurrentUserId()));
    getCommentStore(repository).update(pullRequestId, rootComment);
    eventBus.post(new CommentEvent(repository, pullRequest, rootComment, clone, HandlerEventType.MODIFY));
  }

  private void handleMentions(Repository repository, PullRequest pullRequest, BasicComment rootComment, BasicComment clone) {
    rootComment.setMentionUserIds(mentionMapper.extractMentionsFromComment(rootComment.getComment()));
    postMentionEventIfNewMentionWasAdded(repository, pullRequest, rootComment, clone);
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

  public ExecutedTransition<CommentTransition> transform(String namespace, String name, String pullRequestId, String commentId, CommentTransition transition) {
    Comment comment = get(namespace, name, pullRequestId, commentId);
    Repository repository = repositoryResolver.resolve(new NamespaceAndName(namespace, name));
    PullRequest pullRequest = pullRequestService.get(repository, pullRequestId);
    Comment clone = comment.clone();
    transition.accept(clone);
    ExecutedTransition<CommentTransition> executedTransition = new ExecutedTransition<>(keyGenerator.createKey(), transition, System.currentTimeMillis(), getCurrentUserId());
    clone.addCommentTransition(executedTransition);
    getCommentStore(repository).update(pullRequestId, clone);
    eventBus.post(new CommentEvent(repository, pullRequest, clone, comment, HandlerEventType.MODIFY));
    return executedTransition;
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
        reply.addTransition(new ExecutedTransition<>(keyGenerator.createKey(), CHANGE_TEXT, System.currentTimeMillis(), getCurrentUserId()));
        handleMentions(repository, pullRequest, reply, clone);
        getCommentStore(repository).update(pullRequestId, parent);
        eventBus.post(new ReplyEvent(repository, pullRequest, reply, clone, parent, HandlerEventType.MODIFY));
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
            doThrow().violation("Must not delete system reply").when(reply.isSystemReply());
            parent.removeReply(reply);
            getCommentStore(repository).update(pullRequestId, parent);
            eventBus.post(new ReplyEvent(repository, pullRequest, null, reply, parent, HandlerEventType.DELETE));
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


  private void postMentionEventIfNewMentionWasAdded(Repository repository, PullRequest pullRequest, BasicComment rootComment, BasicComment clone) {
    boolean newMention = false;
    for (String userId : rootComment.getMentionUserIds()) {
      if (!clone.getMentionUserIds().contains(userId)) {
        newMention = true;
        break;
      }
    }
    if (newMention) {
      BasicComment parsedComment = mentionMapper.parseMentionsUserIdsToDisplayNames(rootComment);
      eventBus.post(new MentionEvent(repository, pullRequest, parsedComment, clone, HandlerEventType.MODIFY));
    }
  }

  private CommentStore getCommentStore(Repository repository) {
    return storeFactory.create(repository);
  }

  void addStatusChangedComment(Repository repository, String pullRequestId, SystemCommentType commentType) {
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
      .filter(replyWithParent -> replyWithParent.reply.getId().equals(commentId))
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

  @Subscribe
  public void addCommentOnMerge(PullRequestMergedEvent mergedEvent) {
    addStatusChangedComment(mergedEvent.getRepository(), mergedEvent.getPullRequest().getId(), SystemCommentType.MERGED);
  }

  @Subscribe
  public void addCommentOnEmergencyMerge(PullRequestEmergencyMergedEvent mergedEvent) {
    PullRequest pullRequest = mergedEvent.getPullRequest();
    Comment comment = new Comment();
    comment.setEmergencyMerged(true);
    comment.setComment(pullRequest.getOverrideMessage());

    addWithoutPermissionCheck(mergedEvent.getRepository(), pullRequest.getId(), comment);
  }

  @Subscribe
  public void addCommentOnReject(PullRequestRejectedEvent rejectedEvent) {
    Comment comment = Comment.createSystemComment(getCommentType(rejectedEvent.getCause()).getKey());

    String commentId = addWithoutPermissionCheck(
      rejectedEvent.getRepository(),
      rejectedEvent.getPullRequest().getId(),
      comment
    );

    if(!Strings.isNullOrEmpty(rejectedEvent.getMessage())) {
      reply(
        rejectedEvent.getRepository().getNamespace(),
        rejectedEvent.getRepository().getName(),
        rejectedEvent.getPullRequest().getId(),
        commentId,
        Reply.createNewReply(rejectedEvent.getMessage())
      );
    }
  }

  @Subscribe
  public void addCommentOnReopen(PullRequestReopenedEvent reopenedEvent) {
    addStatusChangedComment(reopenedEvent.getRepository(), reopenedEvent.getPullRequest().getId(), SystemCommentType.REOPENED);
  }

  @Subscribe
  public void addCommentOnStatusChanged(PullRequestStatusChangedEvent mergedEvent) {
    switch (mergedEvent.getStatus()) {
      case DRAFT:
        addStatusChangedComment(mergedEvent.getRepository(), mergedEvent.getPullRequest().getId(), SystemCommentType.STATUS_TO_DRAFT);
        break;
      case OPEN:
        addStatusChangedComment(mergedEvent.getRepository(), mergedEvent.getPullRequest().getId(), SystemCommentType.STATUS_TO_OPEN);
        break;
      default:
        throw new IllegalArgumentException("unknown status: " + mergedEvent.getStatus());
    }
  }

  @Subscribe
  public void addCommentOnTargetBranchChange(PullRequestEvent updatedEvent) {
    if (updatedEvent.getOldItem() == null || updatedEvent.getItem() == null) {
      return;
    }
    if (updatedEvent.getItem().getTarget().equals(updatedEvent.getOldItem().getTarget())) {
      return;
    }
    Comment comment = Comment.createSystemComment(
      SystemCommentType.TARGET_CHANGED.getKey(),
      Map.of(
        "oldTarget", updatedEvent.getOldItem().getTarget(),
        "newTarget", updatedEvent.getItem().getTarget()));
    addWithoutPermissionCheck(updatedEvent.getRepository(), updatedEvent.getItem().getId(), comment);
  }

  private SystemCommentType getCommentType(PullRequestRejectedEvent.RejectionCause cause) {
    switch (cause) {
      case SOURCE_BRANCH_DELETED:
        return SystemCommentType.SOURCE_DELETED;
      case TARGET_BRANCH_DELETED:
        return SystemCommentType.TARGET_DELETED;
      case REJECTED_BY_USER:
        return SystemCommentType.REJECTED;
      default:
        throw new IllegalArgumentException("unknown cause: " + cause);
    }
  }
}
