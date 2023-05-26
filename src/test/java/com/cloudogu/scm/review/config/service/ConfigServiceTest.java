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

import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.group.GroupCollector;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryTestData;
import sonia.scm.store.ConfigurationStore;
import sonia.scm.store.ConfigurationStoreFactory;

import java.util.Optional;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConfigServiceTest {

  private static final Repository REPOSITORY = RepositoryTestData.createHeartOfGold();

  ConfigService service;
  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  ConfigurationStoreFactory storeFactory;
  @Mock
  ConfigurationStore<RepositoryPullRequestConfig> repositoryStore;
  @Mock
  ConfigurationStore<NamespacePullRequestConfig> namespaceStore;
  @Mock
  ConfigurationStore<GlobalPullRequestConfig> globalStore;
  @Mock
  GroupCollector groupCollector;

  @BeforeEach
  public void init() {
    when(storeFactory
      .withType(RepositoryPullRequestConfig.class)
      .withName("pullRequestConfig")
      .forRepository(REPOSITORY)
      .build()).thenReturn(repositoryStore);
    when(storeFactory
      .withType(NamespacePullRequestConfig.class)
      .withName("pullRequestConfig")
      .forNamespace(REPOSITORY.getNamespace())
      .build()).thenReturn(namespaceStore);
    when(storeFactory
      .withType(GlobalPullRequestConfig.class)
      .withName("pullRequestConfig")
      .build()).thenReturn(globalStore);
    service = new ConfigService(storeFactory, groupCollector);
  }

  @BeforeEach
  void mockUser() {
    Subject subject = mock(Subject.class);
    lenient().when(subject.getPrincipal()).thenReturn("trillian");
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
    RepositoryPullRequestConfig config = new RepositoryPullRequestConfig();
    config.setRestrictBranchWriteAccess(true);
    service.setRepositoryPullRequestConfig(REPOSITORY, config);

    verify(repositoryStore).set(argThat(c -> {
      assertThat(c.isRestrictBranchWriteAccess()).isTrue();
      return true;
    }));
  }

  @Test
  void shouldStoreChangedGlobalConfig() {
    GlobalPullRequestConfig globalConfig = new GlobalPullRequestConfig();
    globalConfig.setRestrictBranchWriteAccess(true);
    service.setGlobalPullRequestConfig(globalConfig);


    verify(globalStore).set(argThat(c -> {
      assertThat(c.isRestrictBranchWriteAccess()).isTrue();
      return true;
    }));
  }

  @Test
  void shouldBeDisabledIfGloballyAndRepoDisabled() {
    mockGlobalConfig(false, false);

    assertThat(service.isEnabled(REPOSITORY)).isFalse();
  }

  @Test
  void shouldBeEnabledIfGloballyEnabled() {
    mockGlobalConfig(true, true);
    mockRepoConfig(false, true);

    assertThat(service.isEnabled(REPOSITORY)).isTrue();
  }

  @Test
  void shouldBeEnabledIfEnabledForRepository() {
    mockGlobalConfig(false, false);
    mockRepoConfig(true, true);

    assertThat(service.isEnabled(REPOSITORY)).isTrue();
  }

  @Test
  void shouldBeDisabledIfEnabledForRepositoryOnlyButRepoConfigIsDisabled() {
    mockGlobalConfig(false, true);
    mockRepoConfig(true, true);

    assertThat(service.isEnabled(REPOSITORY)).isFalse();
  }

  @Test
  void shouldProtectBranchFromGlobalConfig() {
    mockGlobalConfig(true, false, "master");
    mockRepoConfig(false, true);

    assertThat(service.isBranchProtected(REPOSITORY, "master")).isTrue();
  }

  @Test
  void shouldProtectBranchFromRepoConfig() {
    mockGlobalConfig(true, false, "master");
    mockRepoConfig(true, true, "develop");

    assertThat(service.isBranchProtected(REPOSITORY, "develop")).isTrue();
  }

  @Test
  void shouldOverwriteProtectedBranchWithRepoConfig() {
    mockGlobalConfig(true, false, "master");
    mockRepoConfig(true, true, "develop");

    assertThat(service.isBranchProtected(REPOSITORY, "master")).isFalse();
  }

  @Test
  void shouldNotOverwriteProtectedBranchWithDisabledRepoConfig() {
    mockGlobalConfig(true, true, "master");
    mockRepoConfig(true, true, "develop");

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
    globalConfig.setBranchProtectionBypasses(singletonList(new RepositoryPullRequestConfig.ProtectionBypass("trillian", false)));
    mockRepoConfig(false, true);

    assertThat(service.isBranchProtected(REPOSITORY, "master")).isFalse();
  }

  @Test
  void shouldNotProtectBranchForBypassedUserInRepositoryConfig() {
    mockGlobalConfig(true, false, "master");
    RepositoryPullRequestConfig config = mockRepoConfig(true, true, "master");
    config.setBranchProtectionBypasses(singletonList(new RepositoryPullRequestConfig.ProtectionBypass("trillian", false)));

    assertThat(service.isBranchProtected(REPOSITORY, "master")).isFalse();
  }

  @Test
  void shouldNotProtectBranchForBypassedGroupInGlobalConfig() {
    GlobalPullRequestConfig globalConfig = mockGlobalConfig(true, false, "master");
    globalConfig.setBranchProtectionBypasses(singletonList(new BasePullRequestConfig.ProtectionBypass("hitchhikers", true)));
    when(groupCollector.collect("trillian")).thenReturn(singleton("hitchhikers"));
    mockRepoConfig(false, true);

    assertThat(service.isBranchProtected(REPOSITORY, "master")).isFalse();
  }

  @Test
  void shouldNotProtectBranchForBypassedGroupInRepositoryConfig() {
    mockGlobalConfig(true, false, "master");
    RepositoryPullRequestConfig config = mockRepoConfig(true, true, "master");
    config.setBranchProtectionBypasses(singletonList(new RepositoryPullRequestConfig.ProtectionBypass("hitchhikers", true)));
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
    when(globalStore.getOptional()).thenReturn(Optional.of(pullRequestConfig));

    assertThat(service.isPreventMergeFromAuthor(REPOSITORY)).isTrue();
  }

  @Test
  void shouldPreventMergesFromAuthorIfSetInRepositoryConfig() {
    RepositoryPullRequestConfig repositoryPullRequestConfig = new RepositoryPullRequestConfig();
    repositoryPullRequestConfig.setPreventMergeFromAuthor(true);
    when(repositoryStore.getOptional()).thenReturn(Optional.of(repositoryPullRequestConfig));

    assertThat(service.isPreventMergeFromAuthor(REPOSITORY)).isTrue();
  }

  @Test
  void shouldNotPreventMergesFromAuthorIfSetInRepositoryConfigNutDisabledGlobally() {
    GlobalPullRequestConfig globalPullRequestConfig = new GlobalPullRequestConfig();
    globalPullRequestConfig.setDisableRepositoryConfiguration(true);
    service.setGlobalPullRequestConfig(globalPullRequestConfig);

    RepositoryPullRequestConfig repositoryPullRequestConfig = new RepositoryPullRequestConfig();
    repositoryPullRequestConfig.setPreventMergeFromAuthor(true);
    service.setRepositoryPullRequestConfig(REPOSITORY, repositoryPullRequestConfig);

    assertThat(service.isPreventMergeFromAuthor(REPOSITORY)).isFalse();
  }

  @Test
  void shouldEvaluateGlobalConfigIfOthersAreDisabled() {
    mockGlobalConfig(true, true);

    BasePullRequestConfig config = service.evaluateConfig(REPOSITORY);

    assertThat(config).isInstanceOf(GlobalPullRequestConfig.class);
  }

  @Test
  void shouldEvaluateGlobalConfigIfNotOverwritten() {
    mockGlobalConfig(true, false);
    mockNamespaceConfig(false, false);
    mockRepoConfig(false, false);

    BasePullRequestConfig config = service.evaluateConfig(REPOSITORY);

    assertThat(config).isInstanceOf(GlobalPullRequestConfig.class);
  }

  @Test
  void shouldEvaluateNamespaceConfigIfRepoConfigIsDisabled() {
    mockGlobalConfig(true, false);
    mockNamespaceConfig(true, true);

    BasePullRequestConfig config = service.evaluateConfig(REPOSITORY);

    assertThat(config).isInstanceOf(NamespacePullRequestConfig.class);
  }

  @Test
  void shouldEvaluateNamespaceConfigIfNotDisabledAndNotOverwritten() {
    mockGlobalConfig(true, false);
    mockNamespaceConfig(true, false);
    mockRepoConfig(false, false);

    BasePullRequestConfig config = service.evaluateConfig(REPOSITORY);

    assertThat(config).isInstanceOf(NamespacePullRequestConfig.class);
  }

  @Test
  void shouldEvaluateRepositoryConfigIfNotDisabledAndOverwrite() {
    mockGlobalConfig(true, false);
    mockNamespaceConfig(true, false);
    mockRepoConfig(false, true);

    BasePullRequestConfig config = service.evaluateConfig(REPOSITORY);

    assertThat(config).isInstanceOf(RepositoryPullRequestConfig.class);
  }

  private GlobalPullRequestConfig mockGlobalConfig(boolean enabled, boolean disableRepositoryConfig, String... protectedBranches) {
    GlobalPullRequestConfig globalConfig = new GlobalPullRequestConfig();
    globalConfig.setRestrictBranchWriteAccess(enabled);
    globalConfig.setDisableRepositoryConfiguration(disableRepositoryConfig);
    globalConfig.setProtectedBranchPatterns(asList(protectedBranches));
    lenient().when(globalStore.getOptional()).thenReturn(Optional.of(globalConfig));
    return globalConfig;
  }

  private NamespacePullRequestConfig mockNamespaceConfig(boolean overwrite, boolean disableRepositoryConfig) {
    NamespacePullRequestConfig config = new NamespacePullRequestConfig();
    config.setOverwriteParentConfig(overwrite);
    config.setDisableRepositoryConfiguration(disableRepositoryConfig);
    lenient().when(namespaceStore.getOptional()).thenReturn(Optional.of(config));
    return config;
  }

  private RepositoryPullRequestConfig mockRepoConfig(boolean enabled, boolean overwrite, String... protectedBranches) {
    RepositoryPullRequestConfig config = new RepositoryPullRequestConfig();
    config.setRestrictBranchWriteAccess(enabled);
    config.setProtectedBranchPatterns(asList(protectedBranches));
    config.setOverwriteParentConfig(overwrite);
    lenient().when(repositoryStore.getOptional()).thenReturn(Optional.of(config));
    return config;
  }
}
