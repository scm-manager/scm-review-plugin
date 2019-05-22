package com.cloudogu.scm.review.comment.service;

import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import com.cloudogu.scm.review.pullrequest.service.PullRequestStoreFactory;
import com.google.common.util.concurrent.Striped;
import sonia.scm.HandlerEventType;
import org.apache.shiro.authz.AuthorizationException;
import sonia.scm.NotFoundException;
import sonia.scm.event.ScmEventBus;
import sonia.scm.repository.Repository;
import sonia.scm.security.KeyGenerator;
import sonia.scm.store.DataStore;

import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class CommentStore {

  private static final Striped<Lock> LOCKS = Striped.lock(10);

  private final DataStore<PullRequestComments> store;
  private final PullRequestStoreFactory pullRequestStoreFactory;
  private final ScmEventBus eventBus;
  private KeyGenerator keyGenerator;

  CommentStore(DataStore<PullRequestComments> store, PullRequestStoreFactory pullRequestStoreFactory, ScmEventBus eventBus, KeyGenerator keyGenerator) {
    this.store = store;
    this.pullRequestStoreFactory = pullRequestStoreFactory;
    this.eventBus = eventBus;
    this.keyGenerator = keyGenerator;
  }

  public String add(Repository repository, String pullRequestId, PullRequestComment pullRequestComment) {

    PullRequest pullRequest = pullRequestStoreFactory.create(repository).get(pullRequestId);
    return withLockDo(pullRequestId, () -> {
      PullRequestComments pullRequestComments = Optional.ofNullable(store.get(pullRequestId)).orElse(new PullRequestComments());
      String commentId = keyGenerator.createKey();
      pullRequestComment.setId(commentId);
      pullRequestComments.getComments().add(pullRequestComment);
      store.put(pullRequestId, pullRequestComments);
      eventBus.post(new CommentEvent(repository, pullRequest, pullRequestComment, null, HandlerEventType.CREATE));
      return commentId;
    });
  }

  public PullRequestComments get(String pullRequestId) {
    return withLockDo(pullRequestId, () -> {
      PullRequestComments result = store.get(pullRequestId);
      if (result == null) {
        throw new NotFoundException(PullRequest.class, pullRequestId);
      }
      return result;
    });
  }

  public void update(Repository repository, String pullRequestId, String commentId, String newComment, boolean done) {
    PullRequest pullRequest = pullRequestStoreFactory.create(repository).get(pullRequestId);
    withLockDo(pullRequestId, () -> {
      PullRequestComments pullRequestComments = this.get(pullRequestId);
      applyChange(commentId, pullRequestComments, comment -> {
        PullRequestComment oldComment = comment.toBuilder().build();
        comment.setComment(newComment);
        comment.setDone(done);
        eventBus.post(new CommentEvent(repository, pullRequest, comment, oldComment, HandlerEventType.MODIFY));
      });
      store.put(pullRequestId, pullRequestComments);
      return null;
    });
  }

  public void delete(Repository repository, String pullRequestId, String commentId) {
    PullRequest pullRequest = pullRequestStoreFactory.create(repository).get(pullRequestId);
    withLockDo(pullRequestId, () -> {
      PullRequestComments pullRequestComments = Optional.ofNullable(store.get(pullRequestId)).orElse(new PullRequestComments());
      applyChange(commentId, pullRequestComments, comment -> {
        PullRequestComment oldComment = comment.toBuilder().build();
        pullRequestComments.getComments().remove(comment);
        eventBus.post(new CommentEvent(repository, pullRequest, null, oldComment, HandlerEventType.DELETE));
      });
      store.put(pullRequestId, pullRequestComments);
      return null;
    });
  }

  private PullRequestComment checkNoSystemComment(PullRequestComment comment) {
    if (comment.isSystemComment()) {
      throw new AuthorizationException("It is forbidden to delete a system comment.");
    } else {
      return comment;
    }
  }

  private <T> T withLockDo(String pullRequestId, Supplier<T> worker) {
    Lock lock = LOCKS.get(pullRequestId);
    lock.lock();
    try {
      return worker.get();
    } finally {
      lock.unlock();
    }
  }

  private void applyChange(String commentId, PullRequestComments pullRequestComments, Consumer<PullRequestComment> commentConsumer) {
    pullRequestComments.getComments()
      .stream()
      .filter(c -> c.getId().equals(commentId))
      .findFirst()
      .map(this::checkNoSystemComment)
      .ifPresent(commentConsumer);
  }
}
