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

import com.cloudogu.scm.review.config.service.NamespacePullRequestConfig;
import sonia.scm.migration.NamespaceUpdateContext;
import sonia.scm.migration.NamespaceUpdateStep;
import sonia.scm.plugin.Extension;
import sonia.scm.store.ConfigurationStore;
import sonia.scm.store.ConfigurationStoreFactory;
import sonia.scm.version.Version;

import jakarta.inject.Inject;

@Extension
public class BranchRestrictionsNamespaceUpdateStep implements NamespaceUpdateStep {

  private final ConfigurationStoreFactory configurationStoreFactory;

  @Inject
  public BranchRestrictionsNamespaceUpdateStep(ConfigurationStoreFactory configurationStoreFactory) {
    this.configurationStoreFactory = configurationStoreFactory;
  }

  @Override
  public void doUpdate(NamespaceUpdateContext namespaceUpdateContext) {
    String namespace = namespaceUpdateContext.getNamespace();
    ConfigurationStore<OldNamespacePullRequestConfig> namespaceStore = configurationStoreFactory
      .withType(OldNamespacePullRequestConfig.class)
      .withName("pullRequestConfig")
      .forNamespace(namespace)
      .build();
    namespaceStore.getOptional().ifPresent(config -> {
      NamespacePullRequestConfig newNamespaceConfig = new NamespacePullRequestConfig();
      config.fill(newNamespaceConfig);
      newNamespaceConfig.setDisableRepositoryConfiguration(config.isDisableRepositoryConfiguration());
      newNamespaceConfig.setOverwriteParentConfig(config.isOverwriteParentConfig());
      ConfigurationStore<NamespacePullRequestConfig> newNamespaceStore = configurationStoreFactory
        .withType(NamespacePullRequestConfig.class)
        .withName("pullRequestConfig")
        .forNamespace(namespace)
        .build();
      newNamespaceStore.set(newNamespaceConfig);
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
