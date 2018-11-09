package com.cloudogu.scm.review;

import sonia.scm.repository.NamespaceAndName;
import sonia.scm.repository.Repository;
import sonia.scm.store.DataStore;
import sonia.scm.store.DataStoreFactory;

import javax.inject.Inject;

public class PullRequestStoreFactory {

  private final RepositoryResolver repositoryResolver;
  private final DataStoreFactory dataStoreFactory;

  @Inject
  public PullRequestStoreFactory(RepositoryResolver repositoryResolver, DataStoreFactory dataStoreFactory) {
    this.repositoryResolver = repositoryResolver;
    this.dataStoreFactory = dataStoreFactory;
  }

  public PullRequestStore create(NamespaceAndName namespaceAndName) {
    Repository repository = repositoryResolver.resolve(namespaceAndName);
    DataStore<PullRequest> store = dataStoreFactory.getStore(PullRequest.class, repository.getId());
    return new PullRequestStore(store);
  }

}
