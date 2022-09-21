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

package com.cloudogu.scm.review.workflow;

import com.cloudogu.scm.review.PermissionCheck;
import sonia.scm.repository.Repository;

import javax.inject.Inject;

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
