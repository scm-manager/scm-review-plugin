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
  ConfigurationStore<RepositoryPullRequestConfig> store;

  @BeforeEach
  public void init() {
    ConfigurationStoreFactory storeFactory = new InMemoryConfigurationStoreFactory();
    service = new ConfigService(storeFactory);
    store = storeFactory
      .withType(RepositoryPullRequestConfig.class)
      .withName("pullRequestConfig")
      .forRepository(REPOSITORY)
      .build();
  }

  @Test
  void initialConfigShouldBeDisabled() {
    Assertions.assertThat(service.getRepositoryPullRequestConfig(REPOSITORY).isEnabled()).isFalse();
  }

  @Test
  void shouldStoreChangedConfig() {
    RepositoryPullRequestConfig config = new RepositoryPullRequestConfig();
    config.setEnabled(true);
    service.setRepositoryPullRequestConfig(REPOSITORY, config);

    Assertions.assertThat(service.getRepositoryPullRequestConfig(REPOSITORY).isEnabled()).isTrue();
    Assertions.assertThat(store.get().isEnabled()).isTrue();
  }
}
