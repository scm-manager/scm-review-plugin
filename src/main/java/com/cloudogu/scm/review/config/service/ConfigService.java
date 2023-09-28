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

import com.cloudogu.scm.review.RepositoryResolver;
import com.cloudogu.scm.review.config.ConfigEvaluator;
import org.apache.shiro.SecurityUtils;
import sonia.scm.group.GroupCollector;
import sonia.scm.repository.NamespaceAndName;
import sonia.scm.repository.Repository;
import sonia.scm.store.ConfigurationStore;
import sonia.scm.store.ConfigurationStoreFactory;
import sonia.scm.util.GlobUtil;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Collection;
import java.util.Set;

import static java.util.Collections.emptyList;

@Singleton
public class ConfigService {

  private static final String STORE_NAME = "pullRequestConfig";

  private final ConfigurationStoreFactory storeFactory;
  private final ConfigurationStore<GlobalPullRequestConfig> globalStore;
  private final GroupCollector groupCollector;
  private final RepositoryResolver repositoryResolver;

  @Inject
  public ConfigService(ConfigurationStoreFactory storeFactory, GroupCollector groupCollector, RepositoryResolver repositoryResolver) {
    this.storeFactory = storeFactory;
    globalStore = storeFactory.withType(GlobalPullRequestConfig.class).withName(STORE_NAME).build();
    this.groupCollector = groupCollector;
    this.repositoryResolver = repositoryResolver;
  }

  public BasePullRequestConfig evaluateConfig(NamespaceAndName namespaceAndName) {
    return evaluateConfig(repositoryResolver.resolve(namespaceAndName));
  }

  public BasePullRequestConfig evaluateConfig(Repository repository) {
    GlobalPullRequestConfig globalConfig = globalStore.getOptional().orElse(new GlobalPullRequestConfig());
    NamespacePullRequestConfig namespaceConfig = getNamespacePullRequestConfig(repository.getNamespace());
    RepositoryPullRequestConfig repositoryConfig = getStore(repository).getOptional().orElse(new RepositoryPullRequestConfig());

    return ConfigEvaluator.evaluate(globalConfig, namespaceConfig, repositoryConfig);
  }

  public RepositoryPullRequestConfig getRepositoryPullRequestConfig(Repository repository) {
    return getStore(repository).getOptional().orElse(new RepositoryPullRequestConfig());
  }

  public void setRepositoryPullRequestConfig(Repository repository, RepositoryPullRequestConfig repositoryPullRequestConfig) {
    getStore(repository).set(repositoryPullRequestConfig);
  }

  public NamespacePullRequestConfig getNamespacePullRequestConfig(String namespace) {
    return getStore(namespace).getOptional().orElse(new NamespacePullRequestConfig());
  }

  public void setNamespacePullRequestConfig(String namespace, NamespacePullRequestConfig pullRequestConfig) {
    getStore(namespace).set(pullRequestConfig);
  }

  public GlobalPullRequestConfig getGlobalPullRequestConfig() {
    return globalStore.getOptional().orElse(new GlobalPullRequestConfig());
  }

  public void setGlobalPullRequestConfig(GlobalPullRequestConfig pullRequestConfig) {
    globalStore.set(pullRequestConfig);
  }

  public boolean isEnabled(Repository repository) {
    return getGlobalPullRequestConfig().isRestrictBranchWriteAccess()
      || (!getGlobalPullRequestConfig().isDisableRepositoryConfiguration() && getRepositoryPullRequestConfig(repository).isRestrictBranchWriteAccess());
  }

  public boolean isBranchProtected(Repository repository, String branch) {
    return getProtectedBranches(repository).stream()
      .map(BasePullRequestConfig.BranchProtection::getBranch)
      .anyMatch(branchPattern -> patternMatches(branchPattern, branch));
  }

  public boolean isBranchPathProtected(Repository repository, String branch, String path) {
    return getProtectedBranches(repository)
      .stream()
      .filter(branchProtection -> patternMatches(branchProtection.getBranch(), branch))
      .anyMatch(branchProtection -> patternMatches(branchProtection.getPath(), path.endsWith("/") ? path : path + "/")) ;
  }

  public boolean isPreventMergeFromAuthor(Repository repository) {
    return evaluateConfig(repository).isPreventMergeFromAuthor();
  }

  private Collection<BasePullRequestConfig.BranchProtection> getProtectedBranches(Repository repository) {
    BasePullRequestConfig config = evaluateConfig(repository);
    if (config.isRestrictBranchWriteAccess()) {
      return getProtectedBranches(config);
    } else {
      return emptyList();
    }
  }

  private Collection<BasePullRequestConfig.BranchProtection> getProtectedBranches(BasePullRequestConfig config) {
    String user = SecurityUtils.getSubject().getPrincipal().toString();
    Set<String> groups = groupCollector.collect(user);
    if (userCanBypassProtection(config, user, groups)) {
      return emptyList();
    }
    return config.getProtectedBranchPatterns();
  }

  private boolean userCanBypassProtection(BasePullRequestConfig config, String user, Set<String> groups) {
    return config.getBranchProtectionBypasses()
      .stream()
      .anyMatch(bypass -> bypassMatchesUser(bypass, user, groups));
  }

  private boolean bypassMatchesUser(BasePullRequestConfig.ProtectionBypass bypass, String user, Set<String> groups) {
    if (bypass.isGroup()) {
      return groups.contains(bypass.getName());
    } else {
      return user.equals(bypass.getName());
    }
  }

  private boolean patternMatches(String pattern, String value) {
    return GlobUtil.matches(pattern, value);
  }

  private ConfigurationStore<RepositoryPullRequestConfig> getStore(Repository repository) {
    return storeFactory.withType(RepositoryPullRequestConfig.class).withName(STORE_NAME).forRepository(repository).build();
  }

  private ConfigurationStore<NamespacePullRequestConfig> getStore(String namespace) {
    return storeFactory.withType(NamespacePullRequestConfig.class).withName(STORE_NAME).forNamespace(namespace).build();
  }
}
