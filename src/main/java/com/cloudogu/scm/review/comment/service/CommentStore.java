package com.cloudogu.scm.review.comment.service;

import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.Striped;
import sonia.scm.NotFoundException;
import sonia.scm.store.DataStore;

import java.util.Comparator;
import java.util.Optional;
import java.util.concurrent.locks.Lock;

public class CommentStore {

  private static final Striped<Lock> LOCKS = Striped.lock(10);

  private final DataStore<PullRequestComments> store;

  CommentStore(DataStore<PullRequestComments> store) {
    this.store = store;
  }

  public int add(String pullRequestId, PullRequestComment pullRequestComment) {
    Lock lock = LOCKS.get(pullRequestId);
    int commentId;
    lock.lock();
    try {
      PullRequestComments pullRequestComments = Optional.ofNullable(store.get(pullRequestId)).orElse(new PullRequestComments());
      commentId = createId(pullRequestId);
      pullRequestComment.setId(commentId);
      pullRequestComments.getComments().add(pullRequestComment);
      store.put(pullRequestId, pullRequestComments);
    } finally {
      lock.unlock();
    }
    return commentId;
  }

  public void delete(String pullRequestId, int commentId) {
    Lock lock = LOCKS.get(pullRequestId);
    lock.lock();
    try {
      PullRequestComments pullRequestComments = Optional.ofNullable(store.get(pullRequestId)).orElse(new PullRequestComments());
      pullRequestComments.getComments()
        .stream()
        .filter(c -> c.getId() == (commentId))
        .findFirst()
        .ifPresent(comment -> pullRequestComments.getComments().remove(comment));
      store.put(pullRequestId, pullRequestComments);
    } finally {
      lock.unlock();
    }
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


  @VisibleForTesting
  int createId(String pullRequestId) {
    PullRequestComments pullRequestComments = store.get(pullRequestId);
    if (pullRequestComments == null){
      return 1;
    }
    return pullRequestComments.getComments()
      .stream()
      .map(PullRequestComment::getId)
      .max(Comparator.naturalOrder())
      .map(s -> s+1)
      .orElse(1);
  }

}
