package com.cloudogu.scm.review.config.service;

import sonia.scm.repository.Repository;
import sonia.scm.store.ConfigurationStore;
import sonia.scm.store.ConfigurationStoreFactory;

import javax.inject.Inject;

public class ConfigService {

  private static final String STORE_NAME = "pullRequestConfig";

  private final ConfigurationStoreFactory storeFactory;
  private final ConfigurationStore<PullRequestConfig> globalStore;

  @Inject
  public ConfigService(ConfigurationStoreFactory storeFactory) {
    this.storeFactory = storeFactory;
    globalStore = storeFactory.withType(PullRequestConfig.class).withName(STORE_NAME).build();
  }

  public PullRequestConfig getRepositoryPullRequestConfig(Repository repository) {
    ConfigurationStore<PullRequestConfig> store = getStore(repository);
    return store.getOptional().orElse(new PullRequestConfig());
  }

  public void setRepositoryPullRequestConfig(Repository repository, PullRequestConfig pullRequestConfig) {
    getStore(repository).set(pullRequestConfig);
  }

  public PullRequestConfig getGlobalPullRequestConfig() {
    return globalStore.getOptional().orElse(new PullRequestConfig());
  }

  public void setGlobalPullRequestConfig(PullRequestConfig pullRequestConfig) {
    globalStore.set(pullRequestConfig);
  }

  private ConfigurationStore<PullRequestConfig> getStore(Repository repository) {
    return storeFactory.withType(PullRequestConfig.class).withName(STORE_NAME).forRepository(repository).build();
  }
}
