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
package com.cloudogu.scm.review;

import com.cloudogu.scm.review.config.api.RepositoryConfigResource;
import com.cloudogu.scm.review.config.service.ConfigService;
import com.cloudogu.scm.review.pullrequest.api.PullRequestRootResource;
import com.cloudogu.scm.review.workflow.GlobalEngineConfigurator;
import com.cloudogu.scm.review.workflow.RepositoryEngineConfigResource;
import sonia.scm.api.v2.resources.Enrich;
import sonia.scm.api.v2.resources.HalAppender;
import sonia.scm.api.v2.resources.HalEnricher;
import sonia.scm.api.v2.resources.HalEnricherContext;
import sonia.scm.api.v2.resources.LinkBuilder;
import sonia.scm.api.v2.resources.ScmPathInfoStore;
import sonia.scm.plugin.Extension;
import sonia.scm.repository.Repository;
import sonia.scm.repository.api.Command;
import sonia.scm.repository.api.RepositoryService;
import sonia.scm.repository.api.RepositoryServiceFactory;

import javax.inject.Inject;
import javax.inject.Provider;

import static com.cloudogu.scm.review.PermissionCheck.mayConfigure;
import static com.cloudogu.scm.review.PermissionCheck.mayConfigureWorkflowConfig;
import static com.cloudogu.scm.review.PermissionCheck.mayRead;
import static com.cloudogu.scm.review.PermissionCheck.mayReadWorkflowConfig;

@Extension
@Enrich(Repository.class)
public class RepositoryLinkEnricher implements HalEnricher {

  private final Provider<ScmPathInfoStore> scmPathInfoStore;
  private final RepositoryServiceFactory serviceFactory;
  private final ConfigService configService;
  private final GlobalEngineConfigurator globalEngineConfigurator;

  @Inject
  public RepositoryLinkEnricher(Provider<ScmPathInfoStore> scmPathInfoStore, RepositoryServiceFactory serviceFactory, ConfigService configService, GlobalEngineConfigurator globalEngineConfigurator) {
    this.scmPathInfoStore = scmPathInfoStore;
    this.serviceFactory = serviceFactory;
    this.configService = configService;
    this.globalEngineConfigurator = globalEngineConfigurator;
  }

  @Override
  public void enrich(HalEnricherContext context, HalAppender appender) {
    Repository repository = context.oneRequireByType(Repository.class);
    try (RepositoryService repositoryService = serviceFactory.create(repository)) {
      if (repositoryService.isSupported(Command.MERGE)) {
        if (mayRead(repository)) {
          LinkBuilder linkBuilder = new LinkBuilder(scmPathInfoStore.get().get(), PullRequestRootResource.class);
          appender.appendLink("pullRequest", linkBuilder.method("getAll").parameters(repository.getNamespace(), repository.getName()).href());
        }
        if (mayConfigure(repository) && !configService.getGlobalPullRequestConfig().isDisableRepositoryConfiguration()) {
          LinkBuilder linkBuilder = new LinkBuilder(scmPathInfoStore.get().get(), RepositoryConfigResource.class);
          appender.appendLink("pullRequestConfig", linkBuilder.method("getRepositoryConfig").parameters(repository.getNamespace(), repository.getName()).href());
        }
        if (isWorkflowEngineConfigurable(repository)) {
          LinkBuilder linkBuilder = new LinkBuilder(scmPathInfoStore.get().get(), RepositoryEngineConfigResource.class);
          appender.appendLink("workflowConfig", linkBuilder.method("getRepositoryEngineConfig").parameters(repository.getNamespace(), repository.getName()).href());
        }
      }
    }
  }

  private boolean isWorkflowEngineConfigurable(Repository repository) {
    return isWorkflowRepositoryConfigurationEnabled() && isPermittedToConfigureWorkflow(repository);
  }

  private boolean isPermittedToConfigureWorkflow(Repository repository) {
    return mayReadWorkflowConfig(repository) || mayConfigureWorkflowConfig(repository);
  }

  private boolean isWorkflowRepositoryConfigurationEnabled() {
    return !globalEngineConfigurator.getEngineConfiguration().isDisableRepositoryConfiguration();
  }
}
