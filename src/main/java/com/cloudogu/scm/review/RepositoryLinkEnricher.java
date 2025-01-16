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

package com.cloudogu.scm.review;

import com.cloudogu.scm.review.config.api.RepositoryConfigResource;
import com.cloudogu.scm.review.config.service.ConfigService;
import com.cloudogu.scm.review.pullrequest.api.PullRequestRootResource;
import com.cloudogu.scm.review.pullrequest.api.PullRequestSuggestionResource;
import com.cloudogu.scm.review.pullrequest.service.PullRequestService;
import com.cloudogu.scm.review.workflow.EngineConfigService;
import com.cloudogu.scm.review.workflow.RepositoryEngineConfigResource;
import sonia.scm.api.v2.resources.Enrich;
import sonia.scm.api.v2.resources.HalAppender;
import sonia.scm.api.v2.resources.HalEnricher;
import sonia.scm.api.v2.resources.HalEnricherContext;
import sonia.scm.api.v2.resources.LinkBuilder;
import sonia.scm.api.v2.resources.ScmPathInfoStore;
import sonia.scm.plugin.Extension;
import sonia.scm.repository.Repository;

import jakarta.inject.Inject;
import jakarta.inject.Provider;

import static com.cloudogu.scm.review.PermissionCheck.mayConfigure;
import static com.cloudogu.scm.review.PermissionCheck.mayConfigureWorkflowConfig;
import static com.cloudogu.scm.review.PermissionCheck.mayCreate;
import static com.cloudogu.scm.review.PermissionCheck.mayRead;
import static com.cloudogu.scm.review.PermissionCheck.mayReadWorkflowConfig;

@Extension
@Enrich(Repository.class)
public class RepositoryLinkEnricher implements HalEnricher {

  private final Provider<ScmPathInfoStore> scmPathInfoStore;
  private final PullRequestService pullRequestService;
  private final ConfigService configService;
  private final EngineConfigService engineConfigService;


  @Inject
  public RepositoryLinkEnricher(Provider<ScmPathInfoStore> scmPathInfoStore, PullRequestService pullRequestService, ConfigService configService, EngineConfigService engineConfigService) {
    this.scmPathInfoStore = scmPathInfoStore;
    this.pullRequestService = pullRequestService;
    this.configService = configService;
    this.engineConfigService = engineConfigService;
  }

  @Override
  public void enrich(HalEnricherContext context, HalAppender appender) {
    Repository repository = context.oneRequireByType(Repository.class);
    if (pullRequestService.supportsPullRequests(repository)) {
      if (mayRead(repository)) {
        LinkBuilder linkBuilder = new LinkBuilder(scmPathInfoStore.get().get(), PullRequestRootResource.class);
        appender.appendLink("pullRequest", linkBuilder.method("getAll").parameters(repository.getNamespace(), repository.getName()).href());
      }
      if (mayCreate(repository)) {
        LinkBuilder linkBuilder = new LinkBuilder(scmPathInfoStore.get().get(), PullRequestRootResource.class);
        appender.appendLink("pullRequestCheck", linkBuilder.method("check").parameters(repository.getNamespace(), repository.getName()).href());
      }
      if (mayConfigure(repository) && !configService.getGlobalPullRequestConfig().isDisableRepositoryConfiguration() && !configService.getNamespacePullRequestConfig(repository.getNamespace()).isDisableRepositoryConfiguration()) {
        LinkBuilder linkBuilder = new LinkBuilder(scmPathInfoStore.get().get(), RepositoryConfigResource.class);
        appender.appendLink("pullRequestConfig", linkBuilder.method("getRepositoryConfig").parameters(repository.getNamespace(), repository.getName()).href());
      }
      if (mayCreate(repository)) {
        LinkBuilder linkBuilder = new LinkBuilder(scmPathInfoStore.get().get(), PullRequestRootResource.class);
        appender.appendLink("pullRequestTemplate", linkBuilder.method("getPullRequestTemplate").parameters(repository.getNamespace(), repository.getName()).href());
      }
      if (isWorkflowEngineConfigurable(repository)) {
        LinkBuilder linkBuilder = new LinkBuilder(scmPathInfoStore.get().get(), RepositoryEngineConfigResource.class);
        appender.appendLink("workflowConfig", linkBuilder.method("getRepositoryEngineConfig").parameters(repository.getNamespace(), repository.getName()).href());
      }
      if (engineConfigService.isEngineActiveForRepository(repository)) {
        LinkBuilder linkBuilder = new LinkBuilder(scmPathInfoStore.get().get(), RepositoryEngineConfigResource.class);
        appender.appendLink("effectiveWorkflowConfig", linkBuilder.method("getEffectiveRepositoryEngineConfig").parameters(repository.getNamespace(), repository.getName()).href());
      }

      LinkBuilder linkBuilder = new LinkBuilder(scmPathInfoStore.get().get(), PullRequestSuggestionResource.class);
      appender.appendLink("pullRequestSuggestions", linkBuilder.method("getAllPushEntries").parameters(repository.getNamespace(), repository.getName()).href());
    }
  }

  private boolean isWorkflowEngineConfigurable(Repository repository) {
    return isPermittedToConfigureWorkflow(repository) && engineConfigService.isWorkflowRepositoryConfigurationEnabled();
  }

  private boolean isPermittedToConfigureWorkflow(Repository repository) {
    return mayReadWorkflowConfig(repository) || mayConfigureWorkflowConfig(repository);
  }
}
