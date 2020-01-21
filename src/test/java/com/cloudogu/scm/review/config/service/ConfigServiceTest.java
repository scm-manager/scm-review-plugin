package com.cloudogu.scm.review.config.service;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryTestData;
import sonia.scm.store.ConfigurationStore;
import sonia.scm.store.ConfigurationStoreFactory;
import sonia.scm.store.InMemoryConfigurationStoreFactory;

class ConfigServiceTest {

  private static final Repository REPOSITORY = RepositoryTestData.createHeartOfGold();

  ConfigService service;
  ConfigurationStore<PullRequestConfig> repositoryStore;
  ConfigurationStore<PullRequestConfig> globalStore;

  @BeforeEach
  public void init() {
    ConfigurationStoreFactory storeFactory = new InMemoryConfigurationStoreFactory();
    service = new ConfigService(storeFactory);
    repositoryStore = storeFactory
      .withType(PullRequestConfig.class)
      .withName("pullRequestConfig")
      .forRepository(REPOSITORY)
      .build();
    globalStore = storeFactory
      .withType(PullRequestConfig.class)
      .withName("pullRequestConfig")
      .build();
  }

  @Test
  void initialRepositoryConfigShouldBeDisabled() {
    Assertions.assertThat(service.getRepositoryPullRequestConfig(REPOSITORY).isEnabled()).isFalse();
  }

  @Test
  void initialGlobalConfigShouldBeDisabled() {
    Assertions.assertThat(service.getGlobalPullRequestConfig().isEnabled()).isFalse();
  }

  @Test
  void shouldStoreChangedRepositoryConfig() {
    PullRequestConfig config = new PullRequestConfig();
    config.setEnabled(true);
    service.setRepositoryPullRequestConfig(REPOSITORY, config);

    Assertions.assertThat(service.getRepositoryPullRequestConfig(REPOSITORY).isEnabled()).isTrue();
    Assertions.assertThat(repositoryStore.get().isEnabled()).isTrue();
  }

  @Test
  void shouldStoreChangedGlobalConfig() {
    PullRequestConfig config = new PullRequestConfig();
    config.setEnabled(true);
    service.setGlobalPullRequestConfig(config);

    Assertions.assertThat(service.getGlobalPullRequestConfig().isEnabled()).isTrue();
    Assertions.assertThat(globalStore.get().isEnabled()).isTrue();
  }
}
