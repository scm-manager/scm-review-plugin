package com.cloudogu.scm.review.comment.service;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.Striped;
import sonia.scm.store.DataStore;

import java.util.Optional;
import java.util.concurrent.locks.Lock;

public class CommentStore {

  private static final Striped<Lock> LOCKS = Striped.lock(10);

  private final DataStore<PullRequestComments> store;

  CommentStore(DataStore<PullRequestComments> store) {
    this.store = store;
  }

  public String add(String pullRequestId, PullRequestComment pullRequestComment) {
    Lock lock = LOCKS.get(pullRequestId);
    String commentId;
    lock.lock();
    try {
      PullRequestComments pullRequestComments = Optional.ofNullable(store.get(pullRequestId)).orElse(new PullRequestComments());
      commentId = createId();
      pullRequestComment.setId(commentId);
      pullRequestComments.getComments().add(pullRequestComment);
      store.put(pullRequestId, pullRequestComments);
    } finally {
      lock.unlock();
    }
    return commentId;
  }

  public PullRequestComments get(String pullRequestId) {
    Lock lock = LOCKS.get(pullRequestId);
    lock.lock();
    PullRequestComments result;
    try {
      result = store.get(pullRequestId);
    } finally {
      lock.unlock();
    }
    return result;
  }


  @VisibleForTesting
  String createId() {
    return String.valueOf(store.getAll().size() + 1);
  }

}
