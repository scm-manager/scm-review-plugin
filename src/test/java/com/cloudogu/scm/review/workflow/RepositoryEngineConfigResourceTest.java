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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.jboss.resteasy.mock.MockHttpRequest;
import org.jboss.resteasy.mock.MockHttpResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.ScmConstraintViolationException;
import sonia.scm.repository.NamespaceAndName;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryManager;
import sonia.scm.web.RestDispatcher;

import javax.ws.rs.core.UriInfo;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Optional;
import java.util.LinkedHashSet;
import java.util.Set;

import static com.cloudogu.scm.review.workflow.RepositoryEngineConfigResource.WORKFLOW_MEDIA_TYPE;
import static java.util.Optional.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RepositoryEngineConfigResourceTest {

  private static final Repository REPOSITORY = new Repository("1", "git", "space", "X");

  @Mock
  private RepositoryEngineConfigurator repositoryEngineConfigurator;
  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private GlobalEngineConfigurator globalEngineConfigurator;
  @Mock
  private RepositoryManager repositoryManager;
  @Mock
  private UriInfo uriInfo;
  @Mock
  private Engine engine;
  @Mock
  private ConfigurationValidator configurationValidator;

  @Mock
  private Subject subject;

  private RestDispatcher dispatcher;
  private final MockHttpResponse response = new MockHttpResponse();

  private final Set<Rule> availableRules = ImmutableSet.of(new SuccessRule(), new FailureRule(), new ConfigurableRule());

  @BeforeEach
  void init() {
    RepositoryEngineConfigMapperImpl mapper = new RepositoryEngineConfigMapperImpl();
    mapper.availableRules = new AvailableRules(availableRules);
    mapper.configurationValidator = configurationValidator;
    mapper.globalEngineConfigurator = globalEngineConfigurator;
    RepositoryEngineConfigResource repositoryEngineConfigResource = new RepositoryEngineConfigResource(repositoryManager, repositoryEngineConfigurator, mapper, availableRules);

    dispatcher = new RestDispatcher();
    dispatcher.addSingletonResource(repositoryEngineConfigResource);

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

  @BeforeEach
  void initRepositoryManager() {
    lenient().doReturn(REPOSITORY).when(repositoryManager).get(new NamespaceAndName("space", "X"));
    lenient().doReturn(null).when(repositoryManager).get(new NamespaceAndName("unknown", "repository"));
  }

  @Test
  void shouldCheckRepositoryPermissionReadWorkflowConfig() throws URISyntaxException {
    MockHttpRequest request = MockHttpRequest.get("/v2/workflow/space/X/config");
    doThrow(new AuthorizationException()).when(subject).checkPermission("repository:readWorkflowConfig:1");

    dispatcher.invoke(request, response);

    assertThat(response.getStatus()).isEqualTo(403);
  }

  @Test
  void shouldReturnConfigurationForRepository() throws URISyntaxException, UnsupportedEncodingException {
    AppliedRule appliedRule = new AppliedRule(AvailableRules.nameOf(SuccessRule.class), null);
    when(repositoryEngineConfigurator.getEngineConfiguration(REPOSITORY)).thenReturn(new EngineConfiguration(ImmutableList.of(appliedRule), true));

    MockHttpRequest request = MockHttpRequest.get("/v2/workflow/space/X/config");

    dispatcher.invoke(request, response);

    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getContentAsString())
      .contains("\"rules\":[{\"rule\":\"SuccessRule\"}]")
      .contains("\"enabled\":true")
      .contains("\"self\":{\"href\":\"/v2/workflow/space/X/config\"}");
  }

  @Test
  void shouldReturnConfigurationWithConfiguredRuleForRepository() throws URISyntaxException, UnsupportedEncodingException {
    ConfigurationForRule configurationForRule = new ConfigurationForRule(42, "haxor");
    AppliedRule appliedRule = new AppliedRule(AvailableRules.nameOf(ConfigurableRule.class), configurationForRule);
    when(repositoryEngineConfigurator.getEngineConfiguration(any())).thenReturn(new EngineConfiguration(ImmutableList.of(appliedRule), true));

    MockHttpRequest request = MockHttpRequest.get("/v2/workflow/space/X/config");

    dispatcher.invoke(request, response);

    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getContentAsString())
      .contains("\"rules\":[{\"rule\":\"ConfigurableRule\",\"configuration\":{\"number\":42,\"string\":\"haxor\"}}]")
      .contains("\"enabled\":true")
      .contains("\"self\":{\"href\":\"/v2/workflow/space/X/config\"}");
  }

  @Test
  void shouldFailForUnknownRepositoryInGet() throws URISyntaxException {
    MockHttpRequest request = MockHttpRequest.get("/v2/workflow/unknown/repository/config");

    dispatcher.invoke(request, response);

    assertThat(response.getStatus()).isEqualTo(404);
  }

  @Test
  void shouldReturnConfigurationForRepositoryWithUpdateLink() throws URISyntaxException, UnsupportedEncodingException {
    when(repositoryEngineConfigurator.getEngineConfiguration(REPOSITORY)).thenReturn(new EngineConfiguration(ImmutableList.of(new AppliedRule(AvailableRules.nameOf(SuccessRule.class), null)), true));
    when(globalEngineConfigurator.getEngineConfiguration().isDisableRepositoryConfiguration()).thenReturn(false);
    when(subject.isPermitted("repository:writeWorkflowConfig:1")).thenReturn(true);

    MockHttpRequest request = MockHttpRequest.get("/v2/workflow/space/X/config");

    dispatcher.invoke(request, response);

    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getContentAsString())
      .contains("\"update\":{\"href\":\"/v2/workflow/space/X/config\"}")
      .contains("\"availableRules\":{\"href\":\"/v2/workflow/rules\"}");
  }

  @Test
  void shouldCheckRepositoryPermissionWriteWorkflowConfig() throws URISyntaxException {
    doThrow(new AuthorizationException()).when(subject).checkPermission("repository:writeWorkflowConfig:1");
    MockHttpRequest request = MockHttpRequest.put("/v2/workflow/space/X/config")
      .content("{\"rules\":[{\"rule\":\"SimpleRule\"}],\"enabled\":true}".getBytes())
      .contentType(WORKFLOW_MEDIA_TYPE);

    dispatcher.invoke(request, response);

    assertThat(response.getStatus()).isEqualTo(403);
    verify(repositoryEngineConfigurator, never()).setEngineConfiguration(any(Repository.class), any(EngineConfiguration.class));
  }

  @Test
  void shouldFailForUnknownRepositoryInSet() throws URISyntaxException {
    MockHttpRequest request = MockHttpRequest.put("/v2/workflow/unknown/repository/config")
      .content("{\"rules\":[{\"rule\":\"SimpleRule\"}],\"enabled\":true}".getBytes())
      .contentType(WORKFLOW_MEDIA_TYPE);

    dispatcher.invoke(request, response);

    assertThat(response.getStatus()).isEqualTo(404);
  }

  @Test
  void shouldSetEngineConfigurationWithSimpleRule() throws URISyntaxException {
    MockHttpRequest request = MockHttpRequest.put("/v2/workflow/space/X/config")
      .content("{\"rules\":[{\"rule\":\"SuccessRule\"}],\"enabled\":true}".getBytes())
      .contentType(WORKFLOW_MEDIA_TYPE);

    dispatcher.invoke(request, response);

    verify(repositoryEngineConfigurator).setEngineConfiguration(any(), argThat(engineConfiguration -> {
      assertThat(engineConfiguration).isNotNull();
      assertThat(engineConfiguration.getRules()).contains(new AppliedRule(SuccessRule.class.getSimpleName()));
      return true;
    }));
    assertThat(response.getStatus()).isEqualTo(204);
  }

  @Test
  void shouldSetEngineConfigurationWithConfigurableRule() throws URISyntaxException {

    MockHttpRequest request = MockHttpRequest.put("/v2/workflow/space/X/config")
      .content("{\"rules\":[{\"rule\":\"ConfigurableRule\",\"configuration\":{\"number\":42,\"string\":\"haxor\"}}],\"enabled\":true}".getBytes())
      .contentType(WORKFLOW_MEDIA_TYPE);

    dispatcher.invoke(request, response);

    verify(repositoryEngineConfigurator).setEngineConfiguration(any(), argThat(engineConfiguration -> {
      assertThat(engineConfiguration).isNotNull();
      assertThat(engineConfiguration.getRules()).hasSize(1);
      AppliedRule appliedRule = engineConfiguration.getRules().get(0);
      assertThat(appliedRule.getRule()).isEqualTo("ConfigurableRule");
      assertThat(appliedRule.getConfiguration()).isNotNull();
      return true;
    }));
    assertThat(response.getStatus()).isEqualTo(204);
  }

  @Test
  void shouldSetValidateConfiguredRule() throws URISyntaxException {

    MockHttpRequest request = MockHttpRequest.put("/v2/workflow/space/X/config")
      .content("{\"rules\":[{\"rule\":\"ConfigurableRule\",\"configuration\":{\"number\":42,\"string\":\"haxor\"}}],\"enabled\":true}".getBytes())
      .contentType(WORKFLOW_MEDIA_TYPE);
    doThrow(ScmConstraintViolationException.class)
      .when(configurationValidator).validate(argThat(o -> o instanceof ConfigurationForRule && ((ConfigurationForRule)o).number == 42));

    dispatcher.invoke(request, response);

    verify(repositoryEngineConfigurator, never()).setEngineConfiguration(any(), any());
    assertThat(response.getStatus()).isEqualTo(400);
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
      return failed();
    }
  }

  public static class ConfigurableRule implements Rule {
    @Override
    public Optional<Class<?>> getConfigurationType() {
      return of(ConfigurationForRule.class);
    }

    @Override
    public Result validate(Context context) {
      return success();
    }

    @Override
    public boolean isApplicableMultipleTimes() {
      return true;
    }
  }

  @Data @AllArgsConstructor @NoArgsConstructor
  public static class ConfigurationForRule {
    int number;
    String string;
  }
}
