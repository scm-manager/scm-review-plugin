package com.cloudogu.scm.review;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;
import com.google.inject.Provider;
import com.google.inject.util.Providers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.api.v2.resources.ScmPathInfoStore;
import sonia.scm.web.JsonEnricherContext;
import sonia.scm.web.VndMediaType;

import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.net.URI;
import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class RepositoryLinkEnricherTest {

  private final ObjectMapper objectMapper = new ObjectMapper();
  private RepositoryLinkEnricher linkEnricher;
  private JsonNode rootNode;

  @BeforeEach
  public void setUp() throws IOException {
    URL resource = Resources.getResource("com/cloudogu/scm/review/repository-001.json");
    rootNode = objectMapper.readTree(resource);

    ScmPathInfoStore pathInfoStore = new ScmPathInfoStore();
    pathInfoStore.set(() -> URI.create("/"));
    Provider<ScmPathInfoStore> pathInfoStoreProvider = Providers.of(pathInfoStore);

    linkEnricher = new RepositoryLinkEnricher(pathInfoStoreProvider, objectMapper);

  }

  @Test
  void testEnrich() {
    JsonEnricherContext context = new JsonEnricherContext(
      URI.create("/"),
      MediaType.valueOf(VndMediaType.REPOSITORY),
      rootNode
    );

    linkEnricher.enrich(context);

    String newPrLink = context.getResponseEntity()
      .get("_links")
      .get("newPullRequest")
      .get("href")
      .asText();

    assertThat(newPrLink).isEqualTo("/v2/pull-requests/scmadmin/web-resources");
  }

  @Test
  void testWithUnsupportedMediaType() {
    JsonEnricherContext context = new JsonEnricherContext(
      URI.create("/"),
      MediaType.valueOf(VndMediaType.USER),
      rootNode
    );

    linkEnricher.enrich(context);

    boolean hasNewPullRequestLink = context.getResponseEntity()
      .get("_links")
      .has("newPullRequest");

    assertThat(hasNewPullRequestLink).isFalse();
  }

}
