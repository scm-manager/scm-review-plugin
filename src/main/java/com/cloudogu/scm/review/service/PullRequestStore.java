package com.cloudogu.scm.review.service;

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

public class PullRequestStore {

  private static final Striped<Lock> LOCKS = Striped.lock(10);


  private final DataStore<PullRequest> store;
  private Repository repository;

  PullRequestStore(DataStore<PullRequest> store, Repository repository) {
    this.store = store;
    this.repository = repository;
  }

  public String add(PullRequest pullRequest) {
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

  public List<PullRequest> getAll() {
    Lock lock = LOCKS.get(repository.getNamespaceAndName());
    lock.lock();
    try {
      Map<String, PullRequest> result = store.getAll();
      return new ArrayList<>(result.values());
    } finally {
      lock.unlock();
    }
  }

  public PullRequest get(String id) {
    Lock lock = LOCKS.get(repository.getNamespaceAndName());
    lock.lock();
    try {
      PullRequest result = store.get(id);
      if (result == null) {
        throw new NotFoundException(PullRequest.class, id);
      }
      return result;
    } finally {
      lock.unlock();
    }
  }

  public void update(PullRequest pullRequest) {
    Lock lock = LOCKS.get(repository.getNamespaceAndName());
    lock.lock();
    try {
      String id = pullRequest.getId();
      pullRequest.setLastModified(Instant.now());
      store.put(id, pullRequest);
    } finally {
      lock.unlock();
    }
  }

  @VisibleForTesting
  String createId() {
    return String.valueOf(store.getAll().size() + 1);
  }

}
