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
import sonia.scm.store.QueryableMutableStore;
import sonia.scm.store.QueryableStore;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.function.Function;
import java.util.function.Supplier;

public class CommentStore {

  private static final Striped<Lock> LOCKS = Striped.lock(10);

  private final Function<String, QueryableMutableStore<Comment>> storeSupplier;
  private KeyGenerator keyGenerator;

  CommentStore(Function<String, QueryableMutableStore<Comment>> storeSupplier, KeyGenerator keyGenerator) {
    this.storeSupplier = storeSupplier;
    this.keyGenerator = keyGenerator;
  }

  public String add(String pullRequestId, Comment pullRequestComment) {
    return withLockDo(pullRequestId, () -> {
      try (QueryableMutableStore<Comment> store = storeSupplier.apply(pullRequestId)) {
        String commentId = keyGenerator.createKey();
        pullRequestComment.setId(commentId);
        store.put(commentId, pullRequestComment);
        return commentId;
      }
    });
  }

  public void update(String pullRequestId, Comment rootComment) {
    withLockDo(pullRequestId, () -> {
      try (QueryableMutableStore<Comment> store = storeSupplier.apply(pullRequestId)) {
        store.put(rootComment.getId(), rootComment);
      }
      return null;
    });
  }

  public List<Comment> getAll(String pullRequestId) {
    try (QueryableMutableStore<Comment> store = storeSupplier.apply(pullRequestId)) {
      return store
        .query()
        .orderBy(CommentQueryFields.DATE, QueryableStore.Order.ASC)
        .findAll();
    }
  }

  Optional<Comment> getPullRequestCommentById(String pullRequestId, String commentId) {
    return getAll(pullRequestId).stream().filter(comment -> comment.getId().equals(commentId)).findFirst();
  }

  public void delete(String pullRequestId, String commentId) {
    withLockDo(pullRequestId, () -> {
      try (QueryableMutableStore<Comment> store = storeSupplier.apply(pullRequestId)) {
        store.remove(commentId);
      }
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
