package com.cloudogu.scm.review;

import sonia.scm.repository.Repository;
import sonia.scm.store.DataStore;
import sonia.scm.store.DataStoreFactory;

import javax.inject.Inject;

public class PullRequestStoreFactory {

  private final DataStoreFactory dataStoreFactory;

  @Inject
  public PullRequestStoreFactory(DataStoreFactory dataStoreFactory) {
    this.dataStoreFactory = dataStoreFactory;
  }

  public PullRequestStore create(Repository repository) {
    DataStore<PullRequest> store = dataStoreFactory.withType(PullRequest.class).withName("pullRequest").forRepository(repository).build();
    return new PullRequestStore(store, repository);
  }

}
