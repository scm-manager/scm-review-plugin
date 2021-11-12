/*
 * MIT License
 *
 * Copyright (c) 2020-present Cloudogu GmbH and Contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.cloudogu.scm.review.config.service;

import com.cloudogu.scm.review.config.service.PullRequestConfig.ProtectionBypass;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sonia.scm.group.GroupCollector;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryTestData;
import sonia.scm.store.ConfigurationStore;
import sonia.scm.store.ConfigurationStoreFactory;
import sonia.scm.store.InMemoryConfigurationStoreFactory;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConfigServiceTest {

  private static final Repository REPOSITORY = RepositoryTestData.createHeartOfGold();

  ConfigService service;
  ConfigurationStore<PullRequestConfig> repositoryStore;
  ConfigurationStore<PullRequestConfig> globalStore;
  GroupCollector groupCollector;

  @BeforeEach
  public void init() {
    groupCollector = mock(GroupCollector.class);
    ConfigurationStoreFactory storeFactory = new InMemoryConfigurationStoreFactory();
    service = new ConfigService(storeFactory, groupCollector);
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

  @BeforeEach
  void mockUser() {
    Subject subject = mock(Subject.class);
    when(subject.getPrincipal()).thenReturn("trillian");
    ThreadContext.bind(subject);
  }

  @AfterEach
  void cleanupContext() {
    ThreadContext.unbindSubject();
  }

  @Test
  void initialRepositoryConfigShouldBeDisabled() {
    assertThat(service.getRepositoryPullRequestConfig(REPOSITORY).isRestrictBranchWriteAccess()).isFalse();
  }

  @Test
  void initialGlobalConfigShouldBeDisabled() {
    assertThat(service.getGlobalPullRequestConfig().isRestrictBranchWriteAccess()).isFalse();
  }

  @Test
  void shouldStoreChangedRepositoryConfig() {
    PullRequestConfig config = new PullRequestConfig();
    config.setRestrictBranchWriteAccess(true);
    service.setRepositoryPullRequestConfig(REPOSITORY, config);

    assertThat(service.getRepositoryPullRequestConfig(REPOSITORY).isRestrictBranchWriteAccess()).isTrue();
    assertThat(repositoryStore.get().isRestrictBranchWriteAccess()).isTrue();
  }

  @Test
  void shouldStoreChangedGlobalConfig() {
    GlobalPullRequestConfig globalConfig = new GlobalPullRequestConfig();
    globalConfig.setRestrictBranchWriteAccess(true);
    service.setGlobalPullRequestConfig(globalConfig);

    assertThat(service.getGlobalPullRequestConfig().isRestrictBranchWriteAccess()).isTrue();
    assertThat(globalStore.get().isRestrictBranchWriteAccess()).isTrue();
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

  @Test
  void shouldNotProtectBranchForBypassedUserInGlobalConfig() {
    GlobalPullRequestConfig globalConfig = mockGlobalConfig(true, false, "master");
    globalConfig.setBranchProtectionBypasses(singletonList(new PullRequestConfig.ProtectionBypass("trillian", false)));
    mockRepoConfig(false);

    assertThat(service.isBranchProtected(REPOSITORY, "master")).isFalse();
  }

  @Test
  void shouldNotProtectBranchForBypassedUserInRepositoryConfig() {
    mockGlobalConfig(true, false, "master");
    PullRequestConfig config = mockRepoConfig(true, "master");
    config.setBranchProtectionBypasses(singletonList(new PullRequestConfig.ProtectionBypass("trillian", false)));

    assertThat(service.isBranchProtected(REPOSITORY, "master")).isFalse();
  }

  @Test
  void shouldNotProtectBranchForBypassedGroupInGlobalConfig() {
    GlobalPullRequestConfig globalConfig = mockGlobalConfig(true, false, "master");
    globalConfig.setBranchProtectionBypasses(singletonList(new ProtectionBypass("hitchhikers", true)));
    when(groupCollector.collect("trillian")).thenReturn(singleton("hitchhikers"));
    mockRepoConfig(false);

    assertThat(service.isBranchProtected(REPOSITORY, "master")).isFalse();
  }

  @Test
  void shouldNotProtectBranchForBypassedGroupInRepositoryConfig() {
    mockGlobalConfig(true, false, "master");
    PullRequestConfig config = mockRepoConfig(true, "master");
    config.setBranchProtectionBypasses(singletonList(new PullRequestConfig.ProtectionBypass("hitchhikers", true)));
    when(groupCollector.collect("trillian")).thenReturn(singleton("hitchhikers"));

    assertThat(service.isBranchProtected(REPOSITORY, "master")).isFalse();
  }

  @Test
  void shouldNotPreventMergesFromAuthorByDefault() {
    assertThat(service.isPreventMergeFromAuthor(REPOSITORY)).isFalse();
  }

  @Test
  void shouldPreventMergesFromAuthorIfSetInGlobalConfig() {
    GlobalPullRequestConfig pullRequestConfig = new GlobalPullRequestConfig();
    pullRequestConfig.setPreventMergeFromAuthor(true);
    service.setGlobalPullRequestConfig(pullRequestConfig);

    assertThat(service.isPreventMergeFromAuthor(REPOSITORY)).isTrue();
  }

  @Test
  void shouldPreventMergesFromAuthorIfSetInRepositoryConfig() {
    PullRequestConfig pullRequestConfig = new PullRequestConfig();
    pullRequestConfig.setPreventMergeFromAuthor(true);
    service.setRepositoryPullRequestConfig(REPOSITORY, pullRequestConfig);

    assertThat(service.isPreventMergeFromAuthor(REPOSITORY)).isTrue();
  }

  @Test
  void shouldNotPreventMergesFromAuthorIfSetInRepositoryConfigNutDisabledGlobally() {
    GlobalPullRequestConfig globalPullRequestConfig = new GlobalPullRequestConfig();
    globalPullRequestConfig.setDisableRepositoryConfiguration(true);
    service.setGlobalPullRequestConfig(globalPullRequestConfig);

    PullRequestConfig pullRequestConfig = new PullRequestConfig();
    pullRequestConfig.setPreventMergeFromAuthor(true);
    service.setRepositoryPullRequestConfig(REPOSITORY, pullRequestConfig);

    assertThat(service.isPreventMergeFromAuthor(REPOSITORY)).isFalse();
  }

  private GlobalPullRequestConfig mockGlobalConfig(boolean enabled, boolean disableRepositoryConfig, String... protectedBranches) {
    GlobalPullRequestConfig globalConfig = new GlobalPullRequestConfig();
    globalConfig.setRestrictBranchWriteAccess(enabled);
    globalConfig.setDisableRepositoryConfiguration(disableRepositoryConfig);
    globalConfig.setProtectedBranchPatterns(asList(protectedBranches));
    service.setGlobalPullRequestConfig(globalConfig);
    return globalConfig;
  }

  private PullRequestConfig mockRepoConfig(boolean enabled, String... protectedBranches) {
    PullRequestConfig config = new PullRequestConfig();
    config.setRestrictBranchWriteAccess(enabled);
    config.setProtectedBranchPatterns(asList(protectedBranches));
    service.setRepositoryPullRequestConfig(REPOSITORY, config);
    return config;
  }
}
