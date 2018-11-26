package com.cloudogu.scm.review;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.Striped;
import sonia.scm.repository.Repository;
import sonia.scm.store.DataStore;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.locks.Lock;

public class PullRequestStore {

  private static final Striped<Lock> LOCKS = Striped.lock(10);


  private final DataStore<PullRequest> store;

  PullRequestStore(DataStore<PullRequest> store) {
    this.store = store;
  }

  public String add(Repository repository, PullRequest pullRequest) {
    Lock lock = LOCKS.get(repository.getNamespaceAndName());
    lock.lock();
    try {
      String id = createId();
      pullRequest.setId(id);
      pullRequest.setCreationDate(Instant.now());
      store.put(id, pullRequest);
      return id;
    } finally {
      lock.unlock();
    }
  }

  public Optional<PullRequest> get(Repository repository, String id) {
    Lock lock = LOCKS.get(repository.getNamespaceAndName());
    lock.lock();
    try {
      return Optional.ofNullable(store.get(id));
    } finally {
      lock.unlock();
    }

  }

  @VisibleForTesting
  String createId() {
    return String.valueOf(store.getAll().size() + 1);
  }

}
