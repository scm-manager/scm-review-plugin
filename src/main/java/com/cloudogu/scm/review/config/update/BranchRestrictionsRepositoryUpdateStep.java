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

package com.cloudogu.scm.review.config.update;

import com.cloudogu.scm.review.config.service.RepositoryPullRequestConfig;
import jakarta.inject.Inject;
import sonia.scm.migration.RepositoryUpdateContext;
import sonia.scm.migration.RepositoryUpdateStep;
import sonia.scm.plugin.Extension;
import sonia.scm.store.ConfigurationStore;
import sonia.scm.store.ConfigurationStoreFactory;
import sonia.scm.version.Version;

@Extension
public class BranchRestrictionsRepositoryUpdateStep implements RepositoryUpdateStep {

  private final ConfigurationStoreFactory configurationStoreFactory;

  @Inject
  public BranchRestrictionsRepositoryUpdateStep(ConfigurationStoreFactory configurationStoreFactory) {
    this.configurationStoreFactory = configurationStoreFactory;
  }

  @Override
  public void doUpdate(RepositoryUpdateContext repositoryUpdateContext) {
    String repositoryId = repositoryUpdateContext.getRepositoryId();
    ConfigurationStore<OldRepositoryPullRequestConfig> repositoryStore = configurationStoreFactory
      .withType(OldRepositoryPullRequestConfig.class)
      .withName("pullRequestConfig")
      .forRepository(repositoryId)
      .build();
    repositoryStore.getOptional().ifPresent(config -> {
      RepositoryPullRequestConfig newRepositoryConfig = new RepositoryPullRequestConfig();
      config.fill(newRepositoryConfig);
      newRepositoryConfig.setOverwriteParentConfig(config.isOverwriteParentConfig());
      ConfigurationStore<RepositoryPullRequestConfig> newRepositoryStore = configurationStoreFactory
        .withType(RepositoryPullRequestConfig.class)
        .withName("pullRequestConfig")
        .forRepository(repositoryId)
        .build();
      newRepositoryStore.set(newRepositoryConfig);
    });
  }

  @Override
  public Version getTargetVersion() {
    return Version.parse("2.31.0");
  }

  @Override
  public String getAffectedDataType() {
    return "com.cloudogu.scm.review.config";
  }
}
