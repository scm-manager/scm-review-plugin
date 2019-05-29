package com.cloudogu.scm.review.comment.service;

import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import com.cloudogu.scm.review.pullrequest.service.PullRequestStoreFactory;
import com.google.common.util.concurrent.Striped;
import sonia.scm.HandlerEventType;
import sonia.scm.event.ScmEventBus;
import sonia.scm.repository.Repository;
import sonia.scm.security.KeyGenerator;
import sonia.scm.store.DataStore;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.function.Supplier;

import static java.util.Optional.ofNullable;

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

  public String add(Repository repository, String pullRequestId, PullRequestRootComment pullRequestComment) {

    PullRequest pullRequest = pullRequestStoreFactory.create(repository).get(pullRequestId);
    return withLockDo(pullRequestId, () -> {
      PullRequestComments pullRequestComments = ofNullable(store.get(pullRequestId)).orElse(new PullRequestComments());
      String commentId = keyGenerator.createKey();
      pullRequestComment.setId(commentId);
      pullRequestComments.getComments().add(pullRequestComment);
      store.put(pullRequestId, pullRequestComments);
      eventBus.post(new CommentEvent(repository, pullRequest, pullRequestComment, null, HandlerEventType.CREATE));
      return commentId;
    });
  }

  public void update(String pullRequestId, PullRequestRootComment rootComment) {
    withLockDo(pullRequestId, () -> {
      PullRequestComments pullRequestComments = this.get(pullRequestId);
      List<PullRequestRootComment> pullRequestCommentList = pullRequestComments.getComments();
      for (int i = 0; i < pullRequestCommentList.size(); ++i) {
        if (pullRequestCommentList.get(i).getId().equals(rootComment.getId())) {
          pullRequestCommentList.set(i, rootComment);
          break;
        }
      }
      store.put(pullRequestId, pullRequestComments);
      return null;
    });
  }

  public List<PullRequestRootComment> getAll(String pullRequestId) {
    return get(pullRequestId).getComments();
  }

  PullRequestComments get(String pullRequestId) {
    return withLockDo(pullRequestId, () -> {
      PullRequestComments result = store.get(pullRequestId);
      if (result == null) {
        return new PullRequestComments();
      }
      return result;
    });
  }

  public void delete(String pullRequestId, String commentId) {
    withLockDo(pullRequestId, () -> {
      PullRequestComments pullRequestComments = get(pullRequestId);
      for (Iterator<PullRequestRootComment> iter = pullRequestComments.getComments().iterator(); iter.hasNext(); ) {
        if (iter.next().getId().equals(commentId)) {
          iter.remove();
          break;
        }
      }
      store.put(pullRequestId, pullRequestComments);
      return null;
    });
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
}
