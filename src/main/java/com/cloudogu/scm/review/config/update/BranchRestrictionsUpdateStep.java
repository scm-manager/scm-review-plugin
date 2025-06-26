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

import com.cloudogu.scm.review.config.service.GlobalPullRequestConfig;
import jakarta.inject.Inject;
import sonia.scm.migration.UpdateStep;
import sonia.scm.plugin.Extension;
import sonia.scm.store.ConfigurationStore;
import sonia.scm.store.ConfigurationStoreFactory;
import sonia.scm.version.Version;

@Extension
public class BranchRestrictionsUpdateStep implements UpdateStep {

  private final ConfigurationStoreFactory configurationStoreFactory;

  @Inject
  public BranchRestrictionsUpdateStep(ConfigurationStoreFactory configurationStoreFactory) {
    this.configurationStoreFactory = configurationStoreFactory;
  }

  @Override
  public void doUpdate() {
    ConfigurationStore<OldGlobalPullRequestConfig> store = configurationStoreFactory
      .withType(OldGlobalPullRequestConfig.class)
      .withName("pullRequestConfig")
      .build();
    store.getOptional().ifPresent(config -> {
      GlobalPullRequestConfig newGlobalConfig = new GlobalPullRequestConfig();
      config.fill(newGlobalConfig);
      newGlobalConfig.setDisableRepositoryConfiguration(config.isDisableRepositoryConfiguration());
      ConfigurationStore<GlobalPullRequestConfig> newStore = configurationStoreFactory
        .withType(GlobalPullRequestConfig.class)
        .withName("pullRequestConfig")
        .build();
      newStore.set(newGlobalConfig);
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
