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

import org.apache.shiro.SecurityUtils;
import sonia.scm.group.GroupCollector;
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

  @Inject
  public ConfigService(ConfigurationStoreFactory storeFactory, GroupCollector groupCollector) {
    this.storeFactory = storeFactory;
    globalStore = storeFactory.withType(GlobalPullRequestConfig.class).withName(STORE_NAME).build();
    this.groupCollector = groupCollector;
  }

  public PullRequestConfig getRepositoryPullRequestConfig(Repository repository) {
    ConfigurationStore<PullRequestConfig> store = getStore(repository);
    return store.getOptional().orElse(new PullRequestConfig());
  }

  public void setRepositoryPullRequestConfig(Repository repository, PullRequestConfig pullRequestConfig) {
    getStore(repository).set(pullRequestConfig);
  }

  public GlobalPullRequestConfig getGlobalPullRequestConfig() {
    return globalStore.getOptional().orElse(new GlobalPullRequestConfig());
  }

  public void setGlobalPullRequestConfig(GlobalPullRequestConfig pullRequestConfig) {
    globalStore.set(pullRequestConfig);
  }

  public boolean isEnabled(Repository repository) {
    return getGlobalPullRequestConfig().isRestrictBranchWriteAccess() || (!getGlobalPullRequestConfig().isDisableRepositoryConfiguration() && getRepositoryPullRequestConfig(repository).isRestrictBranchWriteAccess());
  }

  public boolean isBranchProtected(Repository repository, String branch) {
    return getProtectedBranches(repository).stream().anyMatch(branchPattern -> branchMatches(branch, branchPattern));
  }

  public boolean isPreventMergeFromAuthor(Repository repository) {
    if (getRepositoryPullRequestConfig(repository).isPreventMergeFromAuthor() && !getGlobalPullRequestConfig().isDisableRepositoryConfiguration()) {
      return true;
    } else {
      return getGlobalPullRequestConfig().isPreventMergeFromAuthor();
    }
  }

  private Collection<String> getProtectedBranches(Repository repository) {
    if (getRepositoryPullRequestConfig(repository).isRestrictBranchWriteAccess() && !getGlobalPullRequestConfig().isDisableRepositoryConfiguration()) {
      return getProtectedBranches(getRepositoryPullRequestConfig(repository));
    } else if (getGlobalPullRequestConfig().isRestrictBranchWriteAccess()) {
      return getProtectedBranches(getGlobalPullRequestConfig());
    } else {
      return emptyList();
    }
  }

  private Collection<String> getProtectedBranches(PullRequestConfig config) {
    String user = SecurityUtils.getSubject().getPrincipal().toString();
    Set<String> groups = groupCollector.collect(user);
    if (config.getProtectedBranchExceptions().stream().anyMatch(exceptionEntry -> exceptionMatchesUser(exceptionEntry, user, groups))) {
      return emptyList();
    }
    return config.getProtectedBranchPatterns();
  }

  private boolean exceptionMatchesUser(PullRequestConfig.ExceptionEntry exception, String user, Set<String> groups) {
    if (exception.isGroup()) {
      return groups.contains(exception.getName());
    } else {
      return user.equals(exception.getName());
    }
  }

  private boolean branchMatches(String branch, String branchPattern) {
    return GlobUtil.matches(branchPattern, branch);
  }

  private ConfigurationStore<PullRequestConfig> getStore(Repository repository) {
    return storeFactory.withType(PullRequestConfig.class).withName(STORE_NAME).forRepository(repository).build();
  }
}
