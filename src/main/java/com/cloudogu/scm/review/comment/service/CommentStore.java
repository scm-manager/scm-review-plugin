package com.cloudogu.scm.review.comment.service;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.Striped;
import sonia.scm.store.DataStore;

import java.util.Optional;
import java.util.concurrent.locks.Lock;

public class CommentStore {

  private static final Striped<Lock> LOCKS = Striped.lock(10);

  private final DataStore<Comments> store;

  CommentStore(DataStore<Comments> store) {
    this.store = store;
  }

  public String add(String pullRequestId, Comment comment) {
    Lock lock = LOCKS.get(pullRequestId);
    String commentId;
    lock.lock();
    try {
      Comments comments = Optional.ofNullable(store.get(pullRequestId)).orElse(new Comments());
      commentId = createId();
      comment.setId(commentId);
      comments.getComments().add(comment);
      store.put(pullRequestId, comments);
    } finally {
      lock.unlock();
    }
    return commentId;
  }

  public Comments get(String pullRequestId) {
    Lock lock = LOCKS.get(pullRequestId);
    lock.lock();
    Comments result;
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
