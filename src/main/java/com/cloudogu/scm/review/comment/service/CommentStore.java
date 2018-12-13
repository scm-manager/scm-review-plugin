package com.cloudogu.scm.review.comment.service;

import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import com.google.common.util.concurrent.Striped;
import sonia.scm.NotFoundException;
import sonia.scm.security.KeyGenerator;
import sonia.scm.store.DataStore;

import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.function.Consumer;

public class CommentStore {

  private static final Striped<Lock> LOCKS = Striped.lock(10);

  private final DataStore<PullRequestComments> store;
  private KeyGenerator keyGenerator;

  CommentStore(DataStore<PullRequestComments> store, KeyGenerator keyGenerator) {
    this.store = store;
    this.keyGenerator = keyGenerator;
  }

  public String add(String pullRequestId, PullRequestComment pullRequestComment) {
    Lock lock = LOCKS.get(pullRequestId);
    String commentId;
    lock.lock();
    try {
      PullRequestComments pullRequestComments = Optional.ofNullable(store.get(pullRequestId)).orElse(new PullRequestComments());
      commentId = keyGenerator.createKey();
      pullRequestComment.setId(commentId);
      pullRequestComments.getComments().add(pullRequestComment);
      store.put(pullRequestId, pullRequestComments);
    } finally {
      lock.unlock();
    }
    return commentId;
  }

  public void update(String pullRequestId, String commentId, String newComment) {
    PullRequestComments pullRequestComments = this.get(pullRequestId);
    Lock lock = LOCKS.get(pullRequestId);
    lock.lock();
    try {
      applyChange(commentId, pullRequestComments, comment -> comment.setComment(newComment));
      store.put(pullRequestId, pullRequestComments);
    } finally {
      lock.unlock();
    }
  }

  public void delete(String pullRequestId, String commentId) {
    Lock lock = LOCKS.get(pullRequestId);
    lock.lock();
    try {
      PullRequestComments pullRequestComments = Optional.ofNullable(store.get(pullRequestId)).orElse(new PullRequestComments());
      applyChange(commentId, pullRequestComments, comment -> pullRequestComments.getComments().remove(comment));
      store.put(pullRequestId, pullRequestComments);
    } finally {
      lock.unlock();
    }
  }

  public void applyChange(String commentId, PullRequestComments pullRequestComments, Consumer<PullRequestComment> commentConsumer) {
    pullRequestComments.getComments()
      .stream()
      .filter(c -> c.getId().equals(commentId))
      .findFirst()
      .ifPresent(commentConsumer);
  }

  public PullRequestComments get(String pullRequestId) {
    Lock lock = LOCKS.get(pullRequestId);
    lock.lock();
    PullRequestComments result;
    try {
      result = store.get(pullRequestId);
      if (result == null) {
        throw new NotFoundException(PullRequest.class, pullRequestId);
      }
    } finally {
      lock.unlock();
    }
    return result;
  }
}
