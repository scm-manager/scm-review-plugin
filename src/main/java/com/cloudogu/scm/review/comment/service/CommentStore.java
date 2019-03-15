package com.cloudogu.scm.review.comment.service;

import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import com.google.common.util.concurrent.Striped;
import org.apache.shiro.authz.AuthorizationException;
import sonia.scm.NotFoundException;
import sonia.scm.security.KeyGenerator;
import sonia.scm.store.DataStore;

import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class CommentStore {

  private static final Striped<Lock> LOCKS = Striped.lock(10);

  private final DataStore<PullRequestComments> store;
  private KeyGenerator keyGenerator;

  CommentStore(DataStore<PullRequestComments> store, KeyGenerator keyGenerator) {
    this.store = store;
    this.keyGenerator = keyGenerator;
  }

  public String add(String pullRequestId, PullRequestComment pullRequestComment) {
    return withLockDo(pullRequestId, () -> {
      PullRequestComments pullRequestComments = Optional.ofNullable(store.get(pullRequestId)).orElse(new PullRequestComments());
      String commentId = keyGenerator.createKey();
      pullRequestComment.setId(commentId);
      pullRequestComments.getComments().add(pullRequestComment);
      store.put(pullRequestId, pullRequestComments);
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

  public void update(String pullRequestId, String commentId, String newComment) {
    withLockDo(pullRequestId, () -> {
      PullRequestComments pullRequestComments = this.get(pullRequestId);
      applyChange(commentId, pullRequestComments, comment -> comment.setComment(newComment));
      store.put(pullRequestId, pullRequestComments);
      return null;
    });
  }

  public void delete(String pullRequestId, String commentId) {
    withLockDo(pullRequestId, () -> {
      PullRequestComments pullRequestComments = Optional.ofNullable(store.get(pullRequestId)).orElse(new PullRequestComments());
      applyChange(commentId, pullRequestComments, comment -> pullRequestComments.getComments().remove(comment));
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
