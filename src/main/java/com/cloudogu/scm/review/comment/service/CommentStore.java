/**
 * MIT License
 *
 * Copyright (c) 2020-present Cloudogu GmbH and Contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.cloudogu.scm.review.comment.service;

import com.google.common.util.concurrent.Striped;
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
