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

package com.cloudogu.scm.review.workflow;

import com.cloudogu.scm.review.PermissionCheck;
import jakarta.inject.Inject;
import sonia.scm.repository.Repository;

public class EngineConfigService {

  private final RepositoryEngineConfigurator repositoryEngineConfigurator;
  private final GlobalEngineConfigurator globalEngineConfigurator;

  @Inject
  public EngineConfigService(RepositoryEngineConfigurator repositoryEngineConfigurator, GlobalEngineConfigurator globalEngineConfigurator) {
    this.repositoryEngineConfigurator = repositoryEngineConfigurator;
    this.globalEngineConfigurator = globalEngineConfigurator;
  }

  public EngineConfiguration getRepositoryEngineConfig(Repository repository) {
    PermissionCheck.checkReadWorkflowConfig(repository);
    return repositoryEngineConfigurator.getEngineConfiguration(repository);
  }

  public void setRepositoryEngineConfig(Repository repository, EngineConfiguration config) {
    PermissionCheck.checkWriteWorkflowConfig(repository);
    repositoryEngineConfigurator.setEngineConfiguration(repository, config);
  }

  public GlobalEngineConfiguration getGlobalEngineConfig() {
    PermissionCheck.checkReadWorkflowEngineGlobalConfig();
    return globalEngineConfigurator.getEngineConfiguration();
  }

  public void setGlobalEngineConfig(GlobalEngineConfiguration config) {
    PermissionCheck.checkWriteWorkflowEngineGlobalConfig();
    globalEngineConfigurator.setEngineConfiguration(config);
  }

  public EngineConfiguration getEffectiveEngineConfig(Repository repository) {
    GlobalEngineConfiguration globalEngineConfiguration = globalEngineConfigurator.getEngineConfiguration();
    EngineConfiguration repositoryEngineConfiguration = repositoryEngineConfigurator.getEngineConfiguration(repository);
    if (!globalEngineConfiguration.isDisableRepositoryConfiguration() && repositoryEngineConfiguration.isEnabled()) {
      return repositoryEngineConfiguration;
    }
    return globalEngineConfiguration;
  }

  public boolean isEngineActiveForRepository(Repository repository) {
    GlobalEngineConfiguration globalEngineConfiguration = globalEngineConfigurator.getEngineConfiguration();
    EngineConfiguration repositoryEngineConfiguration = repositoryEngineConfigurator.getEngineConfiguration(repository);

    return (!globalEngineConfiguration.isDisableRepositoryConfiguration()
      && repositoryEngineConfiguration.isEnabled())
      || globalEngineConfiguration.isEnabled();
  }

  public boolean isWorkflowRepositoryConfigurationEnabled() {
    return !globalEngineConfigurator.getEngineConfiguration().isDisableRepositoryConfiguration();
  }
}
