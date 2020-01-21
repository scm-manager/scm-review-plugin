package com.cloudogu.scm.review.config.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryTestData;
import sonia.scm.store.ConfigurationStore;
import sonia.scm.store.ConfigurationStoreFactory;
import sonia.scm.store.InMemoryConfigurationStoreFactory;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

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
    assertThat(service.getRepositoryPullRequestConfig(REPOSITORY).isEnabled()).isFalse();
  }

  @Test
  void initialGlobalConfigShouldBeDisabled() {
    assertThat(service.getGlobalPullRequestConfig().isEnabled()).isFalse();
  }

  @Test
  void shouldStoreChangedRepositoryConfig() {
    PullRequestConfig config = new PullRequestConfig();
    config.setEnabled(true);
    service.setRepositoryPullRequestConfig(REPOSITORY, config);

    assertThat(service.getRepositoryPullRequestConfig(REPOSITORY).isEnabled()).isTrue();
    assertThat(repositoryStore.get().isEnabled()).isTrue();
  }

  @Test
  void shouldStoreChangedGlobalConfig() {
    GlobalPullRequestConfig globalConfig = new GlobalPullRequestConfig();
    globalConfig.setEnabled(true);
    service.setGlobalPullRequestConfig(globalConfig);

    assertThat(service.getGlobalPullRequestConfig().isEnabled()).isTrue();
    assertThat(globalStore.get().isEnabled()).isTrue();
  }

  @Test
  void shouldBeDisabledIfGloballyAndRepoDisabled() {
    mockGlobalConfig(false, false);

    assertThat(service.isEnabled(REPOSITORY)).isFalse();
  }

  @Test
  void shouldBeEnabledIfGloballyEnabled() {
    mockGlobalConfig(true, true);
    mockRepoConfig(false);

    assertThat(service.isEnabled(REPOSITORY)).isTrue();
  }

  @Test
  void shouldBeEnabledIfEnabledForRepository() {
    mockGlobalConfig(false, false);
    mockRepoConfig(true);

    assertThat(service.isEnabled(REPOSITORY)).isTrue();
  }

  @Test
  void shouldBeDisabledIfEnabledForRepositoryOnlyButRepoConfigIsDisabled() {
    mockGlobalConfig(false, true);
    mockRepoConfig(true);

    assertThat(service.isEnabled(REPOSITORY)).isFalse();
  }

  @Test
  void shouldProtectBranchFromGlobalConfig() {
    mockGlobalConfig(true, false, "master");
    mockRepoConfig(false);

    assertThat(service.isBranchProtected(REPOSITORY, "master")).isTrue();
  }

  @Test
  void shouldProtectBranchFromRepoConfig() {
    mockGlobalConfig(true, false, "master");
    mockRepoConfig(true, "develop");

    assertThat(service.isBranchProtected(REPOSITORY, "develop")).isTrue();
  }

  @Test
  void shouldOverwriteProtectedBranchWithRepoConfig() {
    mockGlobalConfig(true, false, "master");
    mockRepoConfig(true, "develop");

    assertThat(service.isBranchProtected(REPOSITORY, "master")).isFalse();
  }

  @Test
  void shouldNotOverwriteProtectedBranchWithDisabledRepoConfig() {
    mockGlobalConfig(true, true, "master");
    mockRepoConfig(true, "develop");

    assertThat(service.isBranchProtected(REPOSITORY, "master")).isTrue();
  }

  @Test
  void shouldProtectBranchFromGlobalConfigWithPattern() {
    mockGlobalConfig(true, false, "feature/*");

    assertThat(service.isBranchProtected(REPOSITORY, "feature/something")).isTrue();
  }

  private void mockGlobalConfig(boolean enabled, boolean disableRepositoryConfig, String... protectedBranches) {
    GlobalPullRequestConfig globalConfig = new GlobalPullRequestConfig();
    globalConfig.setEnabled(enabled);
    globalConfig.setDisableRepositoryConfiguration(disableRepositoryConfig);
    globalConfig.setProtectedBranchPatterns(asList(protectedBranches));
    service.setGlobalPullRequestConfig(globalConfig);
  }

  private void mockRepoConfig(boolean enabled, String... protectedBranches) {
    PullRequestConfig config = new PullRequestConfig();
    config.setEnabled(enabled);
    config.setProtectedBranchPatterns(asList(protectedBranches));
    service.setRepositoryPullRequestConfig(REPOSITORY, config);
  }
}
