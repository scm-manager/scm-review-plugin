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

import com.google.common.util.concurrent.Striped;
import sonia.scm.security.KeyGenerator;
import sonia.scm.store.DataStore;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.function.Supplier;

import static java.util.Optional.ofNullable;

public class CommentStore {

  private static final Striped<Lock> LOCKS = Striped.lock(10);

  private final DataStore<PullRequestComments> store;
  private KeyGenerator keyGenerator;

  CommentStore(DataStore<PullRequestComments> store, KeyGenerator keyGenerator) {
    this.store = store;
    this.keyGenerator = keyGenerator;
  }

  public String add(String pullRequestId, Comment pullRequestComment) {
    return withLockDo(pullRequestId, () -> {
      PullRequestComments pullRequestComments = ofNullable(store.get(pullRequestId)).orElse(new PullRequestComments());
      String commentId = keyGenerator.createKey();
      pullRequestComment.setId(commentId);
      pullRequestComments.getComments().add(pullRequestComment);
      store.put(pullRequestId, pullRequestComments);
      return commentId;
    });
  }

  public void update(String pullRequestId, Comment rootComment) {
    withLockDo(pullRequestId, () -> {
      PullRequestComments pullRequestComments = this.get(pullRequestId);
      List<Comment> pullRequestCommentList = pullRequestComments.getComments();
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

  public List<Comment> getAll(String pullRequestId) {
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

  Optional<Comment> getPullRequestCommentById(String pullRequestId, String commentId) {
    return getAll(pullRequestId).stream().filter(comment -> comment.getId().equals(commentId)).findFirst();
  }

  public void delete(String pullRequestId, String commentId) {
    withLockDo(pullRequestId, () -> {
      PullRequestComments pullRequestComments = get(pullRequestId);
      for (Iterator<Comment> iter = pullRequestComments.getComments().iterator(); iter.hasNext(); ) {
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
