package com.cloudogu.scm.review.config.service;

import sonia.scm.repository.Repository;
import sonia.scm.store.ConfigurationStore;
import sonia.scm.store.ConfigurationStoreFactory;

import javax.inject.Inject;

public class ConfigService {

  private static final String STORE_NAME = "pullRequestConfig";

  private final ConfigurationStoreFactory storeFactory;

  @Inject
  public ConfigService(ConfigurationStoreFactory storeFactory) {
    this.storeFactory = storeFactory;
  }

  public RepositoryPullRequestConfig getRepositoryPullRequestConfig(Repository repository) {
    ConfigurationStore<RepositoryPullRequestConfig> store = getStore(repository);
    return store.getOptional().orElse(new RepositoryPullRequestConfig());
  }

  public void setRepositoryPullRequestConfig(Repository repository, RepositoryPullRequestConfig repositoryPullRequestConfig) {
    getStore(repository).set(repositoryPullRequestConfig);
  }

  private ConfigurationStore<RepositoryPullRequestConfig> getStore(Repository repository) {
    return storeFactory.withType(RepositoryPullRequestConfig.class).withName(STORE_NAME).forRepository(repository).build();
  }
}
