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

package com.cloudogu.scm.review.pullrequest.api;

import com.cloudogu.scm.review.pullrequest.service.PullRequestSuggestionService;
import com.fasterxml.jackson.databind.JsonNode;
import org.github.sdorra.jse.ShiroExtension;
import org.github.sdorra.jse.SubjectAware;
import org.jboss.resteasy.mock.MockHttpRequest;
import org.jboss.resteasy.mock.MockHttpResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.api.v2.resources.ScmPathInfoStore;
import sonia.scm.repository.NamespaceAndName;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryManager;
import sonia.scm.repository.RepositoryTestData;
import sonia.scm.web.JsonMockHttpResponse;
import sonia.scm.web.RestDispatcher;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, ShiroExtension.class})
@SubjectAware("Trainer Red")
class PullRequestSuggestionResourceTest {

  private final Clock clock = Clock.fixed(Instant.now(), ZoneId.systemDefault());
  private final String domainForLinks = "https://test-domain.de/scm/api/";

  @Mock
  private PullRequestSuggestionService suggestionService;
  @Mock
  private RepositoryManager repositoryManager;
  private RestDispatcher dispatcher;

  @BeforeEach
  void setup() {
    ScmPathInfoStore scmPathInfoStore = new ScmPathInfoStore();
    scmPathInfoStore.set(() -> URI.create(domainForLinks));

    PullRequestSuggestionResource suggestionResource = new PullRequestSuggestionResource(
      suggestionService, repositoryManager, scmPathInfoStore
    );

    dispatcher = new RestDispatcher();
    dispatcher.addSingletonResource(suggestionResource);
  }

  @Nested
  class RemovePushEntry {

    @Test
    void shouldReturnNotFoundBecauseRepositoryDoesNotExist() throws URISyntaxException {
      when(repositoryManager.get(any(NamespaceAndName.class))).thenReturn(null);

      MockHttpRequest request = MockHttpRequest.delete(
        "/v2/push-entries/unknown-namespace/unknown-repository/feature"
      );
      MockHttpResponse response = new MockHttpResponse();
      dispatcher.invoke(request, response);

      assertThat(response.getStatus()).isEqualTo(404);
      verifyNoInteractions(suggestionService);
    }

    @Test
    void shouldReturnNoContentBecauseEntryWasDeleted() throws URISyntaxException {
      Repository repository = RepositoryTestData.create42Puzzle();
      when(repositoryManager.get(any(NamespaceAndName.class))).thenReturn(repository);

      MockHttpRequest request = MockHttpRequest.delete(
        "/v2/push-entries/namespace/repository/feature"
      );
      MockHttpResponse response = new MockHttpResponse();
      dispatcher.invoke(request, response);

      assertThat(response.getStatus()).isEqualTo(204);
      verify(suggestionService).removePushEntry(repository.getId(), "feature", "Trainer Red");
    }
  }

  @Nested
  class GetAllPushEntries {

    @Test
    void shouldReturnNotFoundBecauseRepositoryDoesNotExist() throws URISyntaxException {
      when(repositoryManager.get(any(NamespaceAndName.class))).thenReturn(null);

      MockHttpRequest request = MockHttpRequest.get(
        "/v2/push-entries/unknown-namespace/unknown-repository"
      );
      MockHttpResponse response = new MockHttpResponse();
      dispatcher.invoke(request, response);

      assertThat(response.getStatus()).isEqualTo(404);
      verifyNoInteractions(suggestionService);
    }

    @Test
    void shouldReturnPushEntries() throws URISyntaxException {
      Repository repository = RepositoryTestData.create42Puzzle();
      when(repositoryManager.get(any(NamespaceAndName.class))).thenReturn(repository);

      when(suggestionService.getPushEntries(repository.getId(), "Trainer Red")).thenReturn(List.of(
        new PullRequestSuggestionService.PushEntry(
          repository.getId(), "feature", "Trainer Red", Instant.now(clock)
        ),
        new PullRequestSuggestionService.PushEntry(
          repository.getId(), "bugfix", "Trainer Red", Instant.now(clock)
        )
      ));

      MockHttpRequest request = MockHttpRequest.get(
        "/v2/push-entries/namespace/repository"
      );
      JsonMockHttpResponse response = new JsonMockHttpResponse();
      dispatcher.invoke(request, response);

      assertThat(response.getStatus()).isEqualTo(200);
      JsonNode responseBody = response.getContentAsJson();
      assertThat(responseBody.isArray()).isTrue();
      assertThat(responseBody.size()).isEqualTo(2);

      assertThat(responseBody.get(0).get("branch").asText()).isEqualTo("feature");
      assertThat(responseBody.get(0).get("_links").get("delete").get("href").asText()).isEqualTo(
        String.format("%sv2/push-entries/namespace/repository/feature", domainForLinks)
      );

      assertThat(responseBody.get(1).get("branch").asText()).isEqualTo("bugfix");
      assertThat(responseBody.get(1).get("_links").get("delete").get("href").asText()).isEqualTo(
        String.format("%sv2/push-entries/namespace/repository/bugfix", domainForLinks)
      );
    }
  }
}
