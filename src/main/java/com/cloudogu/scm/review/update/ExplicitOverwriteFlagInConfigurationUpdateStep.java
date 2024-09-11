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

package com.cloudogu.scm.review.update;

import com.cloudogu.scm.review.config.service.RepositoryPullRequestConfig;
import sonia.scm.migration.RepositoryUpdateContext;
import sonia.scm.migration.RepositoryUpdateStep;
import sonia.scm.plugin.Extension;
import sonia.scm.store.ConfigurationStore;
import sonia.scm.store.ConfigurationStoreFactory;
import sonia.scm.version.Version;

import jakarta.inject.Inject;

@Extension
public class ExplicitOverwriteFlagInConfigurationUpdateStep implements RepositoryUpdateStep {

  private final ConfigurationStoreFactory configurationStoreFactory;

  @Inject
  public ExplicitOverwriteFlagInConfigurationUpdateStep(ConfigurationStoreFactory configurationStoreFactory) {
    this.configurationStoreFactory = configurationStoreFactory;
  }

  @Override
  public void doUpdate(RepositoryUpdateContext repositoryUpdateContext) {
    ConfigurationStore<RepositoryPullRequestConfig> store = configurationStoreFactory
      .withType(RepositoryPullRequestConfig.class)
      .withName("pullRequestConfig")
      .forRepository(repositoryUpdateContext.getRepositoryId())
      .build();
    store
      .getOptional()
      .ifPresent(
        configuration -> {
          if (hasRepositoryConfiguration(configuration)) {
            configuration.setOverwriteParentConfig(true);
            store.set(configuration);
          }
        }
      );
  }

  private static boolean hasRepositoryConfiguration(RepositoryPullRequestConfig configuration) {
    return configuration.isPreventMergeFromAuthor()
      || configuration.isRestrictBranchWriteAccess()
      || configuration.getDefaultReviewers() != null && !configuration.getDefaultReviewers().isEmpty();
  }

  @Override
  public Version getTargetVersion() {
    return Version.parse("2.27.0");
  }

  @Override
  public String getAffectedDataType() {
    return "sonia.scm.pullrequest.configuration";
  }
}
