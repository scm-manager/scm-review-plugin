/*
 * Copyright (c) 2020 - present Cloudogu GmbH
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Affero General Public License as published by the Free
 * Software Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see https://www.gnu.org/licenses/.
 */

package com.cloudogu.scm.review.config.service;

import com.cloudogu.scm.review.RepositoryResolver;
import com.cloudogu.scm.review.config.service.ConfigService.BranchProtectionMatcher;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
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
  @Mock
  RepositoryResolver repositoryResolver;

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
    service = new ConfigService(storeFactory, groupCollector, repositoryResolver);
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
    mockGlobalConfig(true, false, new BasePullRequestConfig.BranchProtection("master", "*"));
    mockRepoConfig(false, false);

    assertThat(service.isBranchProtected(REPOSITORY, "master")).isTrue();
  }

  @Test
  void shouldProtectBranchFromRepoConfig() {
    mockGlobalConfig(true, false, new BasePullRequestConfig.BranchProtection("master", "*"));
    mockRepoConfig(true, true, new BasePullRequestConfig.BranchProtection("develop", "*"));

    assertThat(service.isBranchProtected(REPOSITORY, "develop")).isTrue();
  }

  @Test
  void shouldProtectBranchFromNamespaceConfig() {
    NamespacePullRequestConfig namespacePullRequestConfig = mockNamespaceConfig(true, false);
    namespacePullRequestConfig.setRestrictBranchWriteAccess(true);
    namespacePullRequestConfig.setProtectedBranchPatterns(singletonList(new BasePullRequestConfig.BranchProtection("develop", "*")));

    assertThat(service.isBranchProtected(REPOSITORY, "develop")).isTrue();
  }

  @Test
  void shouldOverwriteProtectedBranchWithRepoConfig() {
    mockGlobalConfig(true, false, new BasePullRequestConfig.BranchProtection("master", "*"));
    mockRepoConfig(true, true, new BasePullRequestConfig.BranchProtection("develop", "*"));

    assertThat(service.isBranchProtected(REPOSITORY, "master")).isFalse();
  }

  @Test
  void shouldNotOverwriteProtectedBranchWithDisabledRepoConfig() {
    mockGlobalConfig(true, true, new BasePullRequestConfig.BranchProtection("master", "*"));
    mockRepoConfig(true, true, new BasePullRequestConfig.BranchProtection("develop", "*"));

    assertThat(service.isBranchProtected(REPOSITORY, "master")).isTrue();
  }

  @Test
  void shouldProtectBranchFromGlobalConfigWithPattern() {
    mockGlobalConfig(true, false, new BasePullRequestConfig.BranchProtection("feature/*", "*"));

    assertThat(service.isBranchProtected(REPOSITORY, "feature/something")).isTrue();
  }

  @Test
  void shouldProtectBranchWithWildcardPath() {
    mockGlobalConfig(true, false, new BasePullRequestConfig.BranchProtection("feature/*", "*/java/*"));

    assertThat(service.isBranchPathProtected(REPOSITORY, "feature/something", "src")).isFalse();
    assertThat(service.isBranchPathProtected(REPOSITORY, "feature/something", "src/main/java/")).isTrue();
    assertThat(service.isBranchPathProtected(REPOSITORY, "feature/something", "src/main/java/blubb")).isTrue();
    assertThat(service.isBranchPathProtected(REPOSITORY, "feature/something", "src/main/java/blubb/blobb")).isTrue();
  }

  @Test
  void shouldNotProtectBranchForBypassedUserInGlobalConfig() {
    GlobalPullRequestConfig globalConfig = mockGlobalConfig(true, false, new BasePullRequestConfig.BranchProtection("master", "*"));
    globalConfig.setBranchProtectionBypasses(singletonList(new RepositoryPullRequestConfig.ProtectionBypass("trillian", false)));
    mockRepoConfig(false, true);

    assertThat(service.isBranchProtected(REPOSITORY, "master")).isFalse();
  }

  @Test
  void shouldNotProtectBranchForBypassedUserInRepositoryConfig() {
    mockGlobalConfig(true, false, new BasePullRequestConfig.BranchProtection("master", "*"));
    RepositoryPullRequestConfig config = mockRepoConfig(true, true, new BasePullRequestConfig.BranchProtection("master", "*"));
    config.setBranchProtectionBypasses(singletonList(new RepositoryPullRequestConfig.ProtectionBypass("trillian", false)));

    assertThat(service.isBranchProtected(REPOSITORY, "master")).isFalse();
  }

  @Test
  void shouldNotProtectBranchForBypassedGroupInGlobalConfig() {
    mockRepoConfig(false, true);

    assertThat(service.isBranchProtected(REPOSITORY, "master")).isFalse();
  }

  @Test
  void shouldNotProtectBranchForBypassedGroupInRepositoryConfig() {
    mockGlobalConfig(true, false, new BasePullRequestConfig.BranchProtection("master", "*"));
    RepositoryPullRequestConfig config = mockRepoConfig(true, true, new BasePullRequestConfig.BranchProtection("master", "*"));
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
    repositoryPullRequestConfig.setOverwriteParentConfig(true);
    when(repositoryStore.getOptional()).thenReturn(Optional.of(repositoryPullRequestConfig));

    assertThat(service.isPreventMergeFromAuthor(REPOSITORY)).isTrue();
  }

  @Test
  void shouldPreventMergesFromAuthorIfSetInNamespaceConfig() {
    NamespacePullRequestConfig namespacePullRequestConfig = new NamespacePullRequestConfig();
    namespacePullRequestConfig.setOverwriteParentConfig(true);
    namespacePullRequestConfig.setPreventMergeFromAuthor(true);
    when(namespaceStore.getOptional()).thenReturn(Optional.of(namespacePullRequestConfig));

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

  @Test
  void shouldGetConfigForNamespace() {
    mockGlobalConfig(true, false);
    mockNamespaceConfig(true, false);
    mockRepoConfig(true, true);

    when(repositoryResolver.resolve(REPOSITORY.getNamespaceAndName())).thenReturn(REPOSITORY);

    BasePullRequestConfig config = service.evaluateConfig(REPOSITORY.getNamespaceAndName());

    assertThat(config).isInstanceOf(RepositoryPullRequestConfig.class);
  }

  private GlobalPullRequestConfig mockGlobalConfig(boolean enabled, boolean disableRepositoryConfig, BasePullRequestConfig.BranchProtection... protectedBranches) {
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

  private RepositoryPullRequestConfig mockRepoConfig(boolean enabled, boolean overwrite, BasePullRequestConfig.BranchProtection... protectedBranches) {
    RepositoryPullRequestConfig config = new RepositoryPullRequestConfig();
    config.setRestrictBranchWriteAccess(enabled);
    config.setProtectedBranchPatterns(asList(protectedBranches));
    config.setOverwriteParentConfig(overwrite);
    lenient().when(repositoryStore.getOptional()).thenReturn(Optional.of(config));
    return config;
  }

  @Nested
  class BranchProtectionMatcherTest {
    @Test
    void shouldProtectAllPaths() {
      BranchProtectionMatcher matcher = new BranchProtectionMatcher(
        singletonList(new BasePullRequestConfig.BranchProtection("master", "*")));

      assertThat(matcher.matches("master", "README.md")).isTrue();
      assertThat(matcher.matches("master", "src/main/java")).isTrue();
      assertThat(matcher.matches("master", "develop/something.txt")).isTrue();
      assertThat(matcher.matches("develop", "README.md")).isFalse();
    }

    @Test
    void shouldProtectPathsInFolder() {
      BranchProtectionMatcher matcher = new BranchProtectionMatcher(
        singletonList(new BasePullRequestConfig.BranchProtection("master", "develop/*")));

      assertThat(matcher.matches("master", "README.md")).isFalse();
      assertThat(matcher.matches("master", "src/main/java")).isFalse();
      assertThat(matcher.matches("master", "develop/something.txt")).isTrue();
      assertThat(matcher.matches("master", "develop")).isFalse();
      assertThat(matcher.matches("develop", "README.md")).isFalse();
    }

    @Test
    void shouldProtectPathsInSubFolder() {
      BranchProtectionMatcher matcher = new BranchProtectionMatcher(
        singletonList(new BasePullRequestConfig.BranchProtection("master", "*/main/*")));

      assertThat(matcher.matches("master", "README.md")).isFalse();
      assertThat(matcher.matches("master", "src/main/java")).isTrue();
      assertThat(matcher.matches("master", "develop/something.txt")).isFalse();
      assertThat(matcher.matches("master", "main/something.txt")).isFalse();
    }

    @Test
    void shouldProtectSingleFile() {
      BranchProtectionMatcher matcher = new BranchProtectionMatcher(
        singletonList(new BasePullRequestConfig.BranchProtection("master", "README.md")));

      assertThat(matcher.matches("master", "README.md")).isTrue();
      assertThat(matcher.matches("master", "src/main/java")).isFalse();
      assertThat(matcher.matches("master", "develop/something.txt")).isFalse();
      assertThat(matcher.matches("develop", "README.md")).isFalse();
    }

    @Test
    void shouldProtectDifferentBranches() {
      BranchProtectionMatcher matcher = new BranchProtectionMatcher(
        singletonList(new BasePullRequestConfig.BranchProtection("release/*", "*")));

      assertThat(matcher.matches("master", "README.md")).isFalse();
      assertThat(matcher.matches("release/1.0.0", "README.md")).isTrue();
    }
  }
}
