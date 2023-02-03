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

package com.cloudogu.scm.review.workflow;

import com.cloudogu.scm.review.config.service.ConfigService;
import com.cloudogu.scm.review.pullrequest.api.PullRequestResource;
import com.cloudogu.scm.review.pullrequest.api.PullRequestRootResource;
import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import com.cloudogu.scm.review.pullrequest.service.PullRequestService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.util.Providers;
import org.jboss.resteasy.mock.MockHttpRequest;
import org.jboss.resteasy.mock.MockHttpResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.repository.Repository;
import sonia.scm.user.UserDisplayManager;
import sonia.scm.web.RestDispatcher;

import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EngineResultResourceTest {

  private static final Repository REPOSITORY = new Repository("1", "git", "space", "X");
  private static final PullRequest PULL_REQUEST = new PullRequest("42", "feature", "master");
  private static final Object FAILURE_CONTEXT = new Object() {
    public String getExpected() {
      return "expected value";
    }
  };

  private final RestDispatcher dispatcher = new RestDispatcher();
  private final MockHttpResponse response = new MockHttpResponse();

  @Mock
  private Engine engine;
  @Mock
  private PullRequestService pullRequestService;
  @Mock
  private ConfigService configService;
  @Mock
  private UserDisplayManager userDisplayManager;

  @InjectMocks
  private EngineResultResource resource;

  @BeforeEach
  void setUpResource() {
    PullRequestResource pullRequestResource = new PullRequestResource(null, null, null, Providers.of(resource), null);
    PullRequestRootResource pullRequestRootResource = new PullRequestRootResource(null, null, null, Providers.of(pullRequestResource), configService, userDisplayManager);

    dispatcher.addSingletonResource(pullRequestRootResource);
  }

  @BeforeEach
  void mockPullRequestService() {
    when(pullRequestService.getRepository("space", "X")).thenReturn(REPOSITORY);
    when(pullRequestService.get("space", "X", "42")).thenReturn(PULL_REQUEST);
  }

  @Test
  void shouldGetEmptyResultWithoutRules() throws URISyntaxException, UnsupportedEncodingException, JsonProcessingException {
    when(engine.validate(REPOSITORY, PULL_REQUEST)).thenReturn(new Results(emptyList()));

    MockHttpRequest request = MockHttpRequest.get("/v2/pull-requests/space/X/42/workflow");
    dispatcher.invoke(request, response);

    assertThat(response.getStatus()).isEqualTo(200);
    JsonNode jsonNode = getJsonNode();
    JsonNode results = jsonNode.get("results");
    assertThat(results).isNotNull();
    assertThat(results.size()).isEqualTo(0);

    JsonNode links = jsonNode.get("_links");
    assertThat(links).isNotNull();
    assertThat(links.get("self")).isNotNull();
    assertThat(links.get("self").get("href")).isNotNull();
    assertThat(links.get("self").get("href").asText()).isEqualTo("/v2/pull-requests/space/X/42/workflow/");
  }

  @Test
  void shouldGetResultForExistingRules() throws URISyntaxException, UnsupportedEncodingException, JsonProcessingException {
    when(engine.validate(REPOSITORY, PULL_REQUEST))
      .thenReturn(new Results(asList(new FailingTestRule().validate(null), new SucceedingTestRule().validate(null))));

    MockHttpRequest request = MockHttpRequest.get("/v2/pull-requests/space/X/42/workflow");
    dispatcher.invoke(request, response);

    assertThat(response.getStatus()).isEqualTo(200);
    JsonNode jsonNode = getJsonNode();
    JsonNode results = jsonNode.get("results");
    assertThat(results).isNotNull();
    assertThat(results.size()).isEqualTo(2);

    assertThat(results.get(0)).isNotNull();
    assertThat(results.get(0).get("rule").asText()).isEqualTo("FailingTestRule");
    JsonNode failureContext = results.get(0).get("context");
    assertThat(failureContext).isNotNull();
    assertThat(failureContext.get("expected").asText()).isEqualTo("expected value");

    assertThat(results.get(1)).isNotNull();
    assertThat(results.get(1).get("rule").asText()).isEqualTo("SucceedingTestRule");
  }

  private JsonNode getJsonNode() throws JsonProcessingException, UnsupportedEncodingException {
    return new ObjectMapper().readTree(response.getContentAsString());
  }

  private static class FailingTestRule implements Rule {
    @Override
    public Result validate(Context context) {
      return failed(FAILURE_CONTEXT);
    }
  }

  private static class SucceedingTestRule implements Rule {
    @Override
    public Result validate(Context context) {
      return failed();
    }
  }
}
