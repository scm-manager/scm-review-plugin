package com.cloudogu.scm.review;

import sonia.scm.store.DataStore;

public class PullRequestStore {

  private final DataStore<PullRequest> store;

  PullRequestStore(DataStore<PullRequest> store) {
    this.store = store;
  }

  public String add(PullRequest pullRequest) {
    // TODO handle concurrency, striped lock?
    String id = createId();
    store.put(id, pullRequest);
    return id;
  }

  private String createId() {
    return String.valueOf(store.getAll().size() + 1);
  }

}
