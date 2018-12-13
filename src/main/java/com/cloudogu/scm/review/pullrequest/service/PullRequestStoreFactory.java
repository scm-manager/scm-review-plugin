package com.cloudogu.scm.review.pullrequest.service;

import sonia.scm.repository.Repository;
import sonia.scm.store.DataStore;
import sonia.scm.store.DataStoreFactory;

import javax.inject.Inject;

public class PullRequestStoreFactory {

  private static final String PULL_REQUEST_STORE_NAME = "pullRequest";

  private final DataStoreFactory dataStoreFactory;

  @Inject
  public PullRequestStoreFactory(DataStoreFactory dataStoreFactory) {
    this.dataStoreFactory = dataStoreFactory;
  }

  public PullRequestStore create(Repository repository) {
    DataStore<PullRequest> store = dataStoreFactory.withType(PullRequest.class).withName(PULL_REQUEST_STORE_NAME).forRepository(repository).build();
    return new PullRequestStore(store, repository);
  }

}
