package com.cloudogu.scm.review;

import com.cloudogu.scm.review.pullrequest.api.PullRequestRootResource;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import sonia.scm.api.v2.resources.LinkBuilder;
import sonia.scm.api.v2.resources.ScmPathInfoStore;
import sonia.scm.plugin.Extension;
import sonia.scm.repository.NamespaceAndName;
import sonia.scm.repository.api.Command;
import sonia.scm.repository.api.RepositoryService;
import sonia.scm.repository.api.RepositoryServiceFactory;
import sonia.scm.web.AbstractRepositoryJsonEnricher;

import javax.inject.Inject;
import javax.inject.Provider;

@Extension
public class RepositoryLinkEnricher extends AbstractRepositoryJsonEnricher {

  private final Provider<ScmPathInfoStore> scmPathInfoStore;
  private final RepositoryServiceFactory serviceFactory;

  @Inject
  public RepositoryLinkEnricher(Provider<ScmPathInfoStore> scmPathInfoStore, ObjectMapper objectMapper, RepositoryServiceFactory serviceFactory) {
    super(objectMapper);
    this.scmPathInfoStore = scmPathInfoStore;
    this.serviceFactory = serviceFactory;
  }

  @Override
  protected void enrichRepositoryNode(JsonNode repositoryNode, String namespace, String name) {
    if (repositorySupportsReviews(namespace, name)) {
      String newPullRequest = new LinkBuilder(scmPathInfoStore.get().get(), PullRequestRootResource.class)
        .method("create")
        .parameters(namespace, name)
        .href();
      addLink(repositoryNode, "pullRequest", newPullRequest);
    }
  }

  private boolean repositorySupportsReviews(String namespace, String name) {
    try (RepositoryService service = serviceFactory.create(new NamespaceAndName(namespace, name))) {
      return service.isSupported(Command.BRANCHES);
    }
  }
}
