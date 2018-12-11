package com.cloudogu.scm.review.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import sonia.scm.api.v2.resources.LinkBuilder;
import sonia.scm.api.v2.resources.ScmPathInfoStore;
import sonia.scm.plugin.Extension;
import sonia.scm.repository.NamespaceAndName;
import sonia.scm.repository.api.Command;
import sonia.scm.repository.api.RepositoryService;
import sonia.scm.repository.api.RepositoryServiceFactory;
import sonia.scm.web.JsonEnricherBase;
import sonia.scm.web.JsonEnricherContext;

import javax.inject.Inject;
import javax.inject.Provider;

import static java.util.Collections.singletonMap;
import static sonia.scm.web.VndMediaType.REPOSITORY;
import static sonia.scm.web.VndMediaType.REPOSITORY_COLLECTION;

@Extension
public class RepositoryLinkEnricher extends JsonEnricherBase {

  private final Provider<ScmPathInfoStore> scmPathInfoStore;
  private final RepositoryServiceFactory serviceFactory;

  @Inject
  public RepositoryLinkEnricher(Provider<ScmPathInfoStore> scmPathInfoStore, ObjectMapper objectMapper, RepositoryServiceFactory serviceFactory) {
    super(objectMapper);
    this.scmPathInfoStore = scmPathInfoStore;
    this.serviceFactory = serviceFactory;
  }

  @Override
  public void enrich(JsonEnricherContext context) {
    if (resultHasMediaType(REPOSITORY, context)) {
      JsonNode repositoryNode = context.getResponseEntity();
      enrichRepository(repositoryNode);
    } else if (resultHasMediaType(REPOSITORY_COLLECTION, context)) {
      JsonNode collectionNode = context.getResponseEntity();
      JsonNode embedded = collectionNode.get("_embedded");
      JsonNode repositories = embedded.get("repositories");
      for (JsonNode repositoryNode : repositories) {
        enrichRepository(repositoryNode);
      }
    }
  }

  private void enrichRepository(JsonNode repositoryNode) {
    String namespace = repositoryNode.get("namespace").asText();
    String name = repositoryNode.get("name").asText();

    if (repositorySupportsReviews(namespace, name)) {
      String newPullRequest = new LinkBuilder(scmPathInfoStore.get().get(), PullRequestResource.class)
        .method("create")
        .parameters(namespace, name)
        .href();

      JsonNode newPullRequestNode = createObject(singletonMap("href", value(newPullRequest)));

      addPropertyNode(repositoryNode.get("_links"), "pullRequest", newPullRequestNode);
    }
  }

  private boolean repositorySupportsReviews(String namespace, String name) {
    try (RepositoryService service = serviceFactory.create(new NamespaceAndName(namespace, name))) {
      return service.isSupported(Command.BRANCHES);
    }
  }
}
