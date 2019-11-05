package com.cloudogu.scm.review;

import com.cloudogu.scm.review.pullrequest.api.MergeResource;
import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import sonia.scm.api.v2.resources.Enrich;
import sonia.scm.api.v2.resources.HalAppender;
import sonia.scm.api.v2.resources.HalEnricher;
import sonia.scm.api.v2.resources.HalEnricherContext;
import sonia.scm.api.v2.resources.LinkBuilder;
import sonia.scm.api.v2.resources.ScmPathInfoStore;
import sonia.scm.plugin.Extension;
import sonia.scm.repository.NamespaceAndName;
import sonia.scm.repository.api.MergeStrategy;
import sonia.scm.repository.api.RepositoryService;
import sonia.scm.repository.api.RepositoryServiceFactory;

import javax.inject.Inject;
import javax.inject.Provider;

@Extension
@Enrich(PullRequest.class)
public class PullRequestLinkEnricher implements HalEnricher {

  private final Provider<ScmPathInfoStore> scmPathInfoStoreProvider;
  private final RepositoryServiceFactory serviceFactory;

  @Inject
  public PullRequestLinkEnricher(Provider<ScmPathInfoStore> scmPathInfoStoreProvider, RepositoryServiceFactory serviceFactory) {
    this.scmPathInfoStoreProvider = scmPathInfoStoreProvider;
    this.serviceFactory = serviceFactory;
  }

  @Override
  public void enrich(HalEnricherContext context, HalAppender appender) {
    NamespaceAndName namespaceAndName = context.oneRequireByType(NamespaceAndName.class);
    try (RepositoryService repositoryService = serviceFactory.create(namespaceAndName)) {
      LinkBuilder linkBuilder = new LinkBuilder(scmPathInfoStoreProvider.get().get(), MergeResource.class);
      HalAppender.LinkArrayBuilder linkCollection = appender.linkArrayBuilder("merge");

      if (repositoryService.getMergeCommand().isSupported(MergeStrategy.SQUASH)) {
        createStrategyLink(linkBuilder, linkCollection, namespaceAndName, "squash");
      }

      linkCollection.build();
    }
  }

  private HalAppender.LinkArrayBuilder createStrategyLink(LinkBuilder linkBuilder,
                                                          HalAppender.LinkArrayBuilder linkCollection,
                                                          NamespaceAndName namespaceAndName,
                                                          String strategy) {
    return linkCollection.append(strategy, createLink(linkBuilder, namespaceAndName) + "?strategy=" + strategy);
  }

  private String createLink(LinkBuilder linkBuilder, NamespaceAndName namespaceAndName) {
    return linkBuilder.method("merge").parameters(namespaceAndName.getNamespace(), namespaceAndName.getName()).href();
  }
}
