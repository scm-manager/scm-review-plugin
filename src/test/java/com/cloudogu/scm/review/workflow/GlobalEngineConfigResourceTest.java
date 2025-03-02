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

package com.cloudogu.scm.review.workflow;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.jboss.resteasy.mock.MockHttpRequest;
import org.jboss.resteasy.mock.MockHttpResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.web.RestDispatcher;

import jakarta.ws.rs.core.UriInfo;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedHashSet;
import java.util.Set;

import static com.cloudogu.scm.review.workflow.RepositoryEngineConfigResource.WORKFLOW_MEDIA_TYPE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class})
class GlobalEngineConfigResourceTest {

  @Mock
  private EngineConfigService engineConfigService;
  @Mock
  private UriInfo uriInfo;

  @Mock
  private Subject subject;

  private RestDispatcher dispatcher;
  private final MockHttpResponse response = new MockHttpResponse();

  private Set<Rule> availableRules;

  @BeforeEach
  void init() {
    availableRules = new LinkedHashSet<>();
    GlobalEngineConfigMapperImpl mapper = new GlobalEngineConfigMapperImpl();
    mapper.availableRules = AvailableRules.of(new SuccessRule());
    GlobalEngineConfigResource globalEngineConfigResource = new GlobalEngineConfigResource(mapper, engineConfigService, availableRules);

    dispatcher = new RestDispatcher();
    dispatcher.addSingletonResource(globalEngineConfigResource);

    lenient().when(uriInfo.getBaseUri()).thenReturn(URI.create("localhost/scm/api"));
  }

  @BeforeEach
  void initSubject() {
    ThreadContext.bind(subject);
  }

  @AfterEach
  void unbindSubject() {
    ThreadContext.unbindSubject();
  }

  @Test
  void shouldReturnConfiguration() throws URISyntaxException, UnsupportedEncodingException {
    when(engineConfigService.getGlobalEngineConfig()).thenReturn(new GlobalEngineConfiguration(ImmutableList.of(AppliedRule.of(SuccessRule.class)), true, false));
    when(subject.isPermitted("configuration:write:workflowConfig")).thenReturn(true);

    MockHttpRequest request = MockHttpRequest.get("/v2/workflow/config");

    dispatcher.invoke(request, response);

    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getContentAsString())
      .contains("\"rules\":[{\"rule\":\"SuccessRule\"}]")
      .contains("\"enabled\":true")
      .contains("\"self\":{\"href\":\"/v2/workflow/config\"}");
  }

  @Test
  void shouldReturnConfigurationWithUpdateLink() throws URISyntaxException, UnsupportedEncodingException {
    when(engineConfigService.getGlobalEngineConfig()).thenReturn(new GlobalEngineConfiguration(ImmutableList.of(AppliedRule.of(SuccessRule.class)), true, false));
    when(subject.isPermitted("configuration:write:workflowConfig")).thenReturn(true);

    MockHttpRequest request = MockHttpRequest.get("/v2/workflow/config");

    dispatcher.invoke(request, response);

    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getContentAsString())
      .contains("\"update\":{\"href\":\"/v2/workflow/config\"}")
      .contains("\"availableRules\":{\"href\":\"/v2/workflow/rules\"}");
  }

  @Test
  void shouldSetEngineConfiguration() throws URISyntaxException {
    MockHttpRequest request = MockHttpRequest.put("/v2/workflow/config")
      .content("{\"rules\":[{\"rule\":\"SuccessRule\"}],\"enabled\":true, \"disableRepositoryConfiguration\":false}".getBytes())
      .contentType(WORKFLOW_MEDIA_TYPE);

    dispatcher.invoke(request, response);

    verify(engineConfigService).setGlobalEngineConfig(any(GlobalEngineConfiguration.class));
    assertThat(response.getStatus()).isEqualTo(204);
  }

  @Test
  void shouldReturnAvailableRules() throws URISyntaxException, UnsupportedEncodingException, JsonProcessingException {
    availableRules.add(new SuccessRule());
    availableRules.add(new FailureRule());
    MockHttpRequest request = MockHttpRequest.get("/v2/workflow/rules");

    dispatcher.invoke(request, response);

    assertThat(response.getStatus()).isEqualTo(200);
    final JsonNode jsonNode = new ObjectMapper().readTree(response.getContentAsString());
    final JsonNode rules = jsonNode.get("rules");
    assertThat(rules).isNotNull();
    assertThat(rules.isArray()).isTrue();
    assertThat(rules).hasSize(2);
    final JsonNode successRule = rules.get(0);
    assertThat(successRule).isNotNull();
    assertThat(successRule.get("name").asText()).isEqualTo(SuccessRule.class.getSimpleName());
    assertThat(successRule.get("applicableMultipleTimes").asBoolean()).isFalse();
    final JsonNode failureRule = rules.get(1);
    assertThat(failureRule).isNotNull();
    assertThat(failureRule.get("name").asText()).isEqualTo(FailureRule.class.getSimpleName());
    assertThat(failureRule.get("applicableMultipleTimes").asBoolean()).isFalse();
  }

  public static class SuccessRule implements Rule {

    @Override
    public Result validate(Context context) {
      return success();
    }
  }

  public static class FailureRule implements Rule {

    @Override
    public Result validate(Context context) {
      return success();
    }
  }
}
