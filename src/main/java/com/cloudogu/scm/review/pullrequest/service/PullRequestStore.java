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

package com.cloudogu.scm.review.pullrequest.service;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.Striped;
import sonia.scm.NotFoundException;
import sonia.scm.repository.Repository;
import sonia.scm.store.QueryableMutableStore;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.function.Supplier;

public class PullRequestStore implements AutoCloseable {

  private static final Striped<Lock> LOCKS = Striped.lock(10);

  private final QueryableMutableStore<PullRequest> store;
  private Repository repository;

  PullRequestStore(QueryableMutableStore<PullRequest> store, Repository repository) {
    this.store = store;
    this.repository = repository;
  }

  public String add(PullRequest pullRequest) {
    return withLockDo(() -> {
      String id = createId();
      pullRequest.setId(id);
      store.put(id, pullRequest);
      return id;
    });
  }

  public List<PullRequest> getAll() {
    return withLockDo(() -> {
      Map<String, PullRequest> result = store.getAll();
      return new ArrayList<>(result.values());
    });
  }

  public PullRequest get(String id) {
    return withLockDo(() -> {
      PullRequest result = store.get(id);
      if (result == null) {
        throw new NotFoundException(PullRequest.class, id);
      }
      return result;
    });
  }

  public void update(PullRequest pullRequest) {
    withLockDo(() -> {
      String id = pullRequest.getId();
      pullRequest.setLastModified(Instant.now());
      store.put(id, pullRequest);
      return null;
    });
  }

  private <T> T withLockDo(Supplier<T> worker) {
    Lock lock = LOCKS.get(repository.getNamespaceAndName());
    lock.lock();
    try {
      return worker.get();
    } finally {
      lock.unlock();
    }
  }

  @VisibleForTesting
  String createId() {
    return String.valueOf(
      store
        .query()
        .project(PullRequestQueryFields.INTERNAL_ID)
        .findAll()
        .stream()
        .map(a -> a[0].toString())
        .mapToInt(Integer::parseInt)
        .max()
        .orElse(0) + 1
    );
  }

  @Override
  public void close() {
    store.close();
  }
}
