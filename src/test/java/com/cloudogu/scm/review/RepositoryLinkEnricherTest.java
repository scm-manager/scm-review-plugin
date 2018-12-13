package com.cloudogu.scm.review;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;
import com.google.inject.Provider;
import com.google.inject.util.Providers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import sonia.scm.api.v2.resources.ScmPathInfoStore;
import sonia.scm.repository.NamespaceAndName;
import sonia.scm.repository.api.Command;
import sonia.scm.repository.api.RepositoryService;
import sonia.scm.repository.api.RepositoryServiceFactory;
import sonia.scm.web.JsonEnricherContext;
import sonia.scm.web.VndMediaType;

import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.net.URI;
import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RepositoryLinkEnricherTest {

  private final ObjectMapper objectMapper = new ObjectMapper();
  private RepositoryLinkEnricher linkEnricher;
  private JsonNode rootNode;
  @Mock
  private RepositoryService repositoryService;
  @Mock
  private RepositoryServiceFactory serviceFactory;

  @Before
  public void setUp() throws IOException {
    URL resource = Resources.getResource("com/cloudogu/scm/review/repository-001.json");
    rootNode = objectMapper.readTree(resource);

    ScmPathInfoStore pathInfoStore = new ScmPathInfoStore();
    pathInfoStore.set(() -> URI.create("/"));
    Provider<ScmPathInfoStore> pathInfoStoreProvider = Providers.of(pathInfoStore);
    when(serviceFactory.create(new NamespaceAndName("scmadmin", "web-resources"))).thenReturn(repositoryService);
    when(repositoryService.isSupported(Command.BRANCHES)).thenReturn(false);

    linkEnricher = new RepositoryLinkEnricher(pathInfoStoreProvider, objectMapper, serviceFactory);

  }

  @Test
  public void shouldEnrichRepositoriesWithBranchSupport() {
    JsonEnricherContext context = new JsonEnricherContext(
      URI.create("/"),
      MediaType.valueOf(VndMediaType.REPOSITORY),
      rootNode
    );

    when(repositoryService.isSupported(Command.BRANCHES)).thenReturn(true);
    linkEnricher.enrich(context);

    String newPrLink = context.getResponseEntity()
      .get("_links")
      .get("pullRequest")
      .get("href")
      .asText();

    assertThat(newPrLink).isEqualTo("/v2/pull-requests/scmadmin/web-resources");
  }

  @Test
  public void shouldNotEnrichRepositoriesWithoutBranchSupport() {
    JsonEnricherContext context = new JsonEnricherContext(
      URI.create("/"),
      MediaType.valueOf(VndMediaType.REPOSITORY),
      rootNode
    );

    when(repositoryService.isSupported(Command.BRANCHES)).thenReturn(false);
    linkEnricher.enrich(context);

    JsonNode newPrLink = context.getResponseEntity()
      .get("_links")
      .get("pullRequest");

    assertThat(newPrLink).isNull();
  }

  @Test
  public void shouldNotModifyObjectsWithUnsupportedMediaType() {
    JsonEnricherContext context = new JsonEnricherContext(
      URI.create("/"),
      MediaType.valueOf(VndMediaType.USER),
      rootNode
    );

    linkEnricher.enrich(context);

    boolean hasNewPullRequestLink = context.getResponseEntity()
      .get("_links")
      .has("pullRequest");

    assertThat(hasNewPullRequestLink).isFalse();
  }

}
