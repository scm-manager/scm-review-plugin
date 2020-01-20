package com.cloudogu.scm.review;

import com.cloudogu.scm.review.config.api.ConfigResource;
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

  @Inject
  public RepositoryLinkEnricher(Provider<ScmPathInfoStore> scmPathInfoStore, RepositoryServiceFactory serviceFactory) {
    this.scmPathInfoStore = scmPathInfoStore;
    this.serviceFactory = serviceFactory;
  }

  @Override
  public void enrich(HalEnricherContext context, HalAppender appender) {
    Repository repository = context.oneRequireByType(Repository.class);
    try (RepositoryService repositoryService = serviceFactory.create(repository)) {
      if (mayRead(repository) && repositoryService.isSupported(Command.MERGE)) {
        LinkBuilder linkBuilder = new LinkBuilder(scmPathInfoStore.get().get(), PullRequestRootResource.class);
        appender.appendLink("pullRequest", linkBuilder.method("getAll").parameters(repository.getNamespace(), repository.getName()).href());
      }
      if (mayConfigure(repository) && repositoryService.isSupported(Command.MERGE)) {
        LinkBuilder linkBuilder = new LinkBuilder(scmPathInfoStore.get().get(), ConfigResource.class);
        appender.appendLink("pullRequestConfig", linkBuilder.method("getConfig").parameters(repository.getNamespace(), repository.getName()).href());
      }
    }
  }
}
