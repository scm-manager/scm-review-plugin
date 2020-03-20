/*
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
package com.cloudogu.scm.review.pullrequest.service;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.Striped;
import sonia.scm.NotFoundException;
import sonia.scm.repository.Repository;
import sonia.scm.store.DataStore;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.function.Supplier;

public class PullRequestStore {

  private static final Striped<Lock> LOCKS = Striped.lock(10);

  private final DataStore<PullRequest> store;
  private Repository repository;

  PullRequestStore(DataStore<PullRequest> store, Repository repository) {
    this.store = store;
    this.repository = repository;
  }

  public String add(PullRequest pullRequest) {
    return withLockDo(() -> {
      String id = createId();
      pullRequest.setId(id);
      pullRequest.setCreationDate(Instant.now());
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
    return String.valueOf(store.getAll().size() + 1);
  }
}
