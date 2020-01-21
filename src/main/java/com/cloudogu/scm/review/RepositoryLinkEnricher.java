package com.cloudogu.scm.review;

import com.cloudogu.scm.review.config.api.RepositoryConfigResource;
import com.cloudogu.scm.review.config.service.ConfigService;
import com.cloudogu.scm.review.pullrequest.api.PullRequestRootResource;
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
import static com.cloudogu.scm.review.PermissionCheck.mayRead;

@Extension
@Enrich(Repository.class)
public class RepositoryLinkEnricher implements HalEnricher {

  private final Provider<ScmPathInfoStore> scmPathInfoStore;
  private final RepositoryServiceFactory serviceFactory;
  private final ConfigService configService;

  @Inject
  public RepositoryLinkEnricher(Provider<ScmPathInfoStore> scmPathInfoStore, RepositoryServiceFactory serviceFactory, ConfigService configService) {
    this.scmPathInfoStore = scmPathInfoStore;
    this.serviceFactory = serviceFactory;
    this.configService = configService;
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
      }
    }
  }
}
