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

import javax.ws.rs.core.UriInfo;
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
  private GlobalEngineConfigurator configurator;
  @Mock
  private UriInfo uriInfo;

  @Mock
  private Subject subject;

  private RestDispatcher dispatcher;
  private final MockHttpResponse response = new MockHttpResponse();

  private final Set<Rule> availableRules = new LinkedHashSet<>();

  @BeforeEach
  void init() {
    GlobalEngineConfigMapperImpl mapper = new GlobalEngineConfigMapperImpl();
    mapper.availableRules = AvailableRules.of(new SuccessRule());
    GlobalEngineConfigResource globalEngineConfigResource = new GlobalEngineConfigResource(mapper, configurator, availableRules);

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
  void shouldCheckPermissionReadWorkflowConfig() throws URISyntaxException {
    MockHttpRequest request = MockHttpRequest.get("/v2/workflow/config");
    doThrow(new AuthorizationException()).when(subject).checkPermission("configuration:read:workflowConfig");

    dispatcher.invoke(request, response);

    assertThat(response.getStatus()).isEqualTo(403);
  }

  @Test
  void shouldReturnConfiguration() throws URISyntaxException, UnsupportedEncodingException {
    when(configurator.getEngineConfiguration()).thenReturn(new GlobalEngineConfiguration(ImmutableList.of(AppliedRule.of(SuccessRule.class)), true, false));
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
    when(configurator.getEngineConfiguration()).thenReturn(new GlobalEngineConfiguration(ImmutableList.of(AppliedRule.of(SuccessRule.class)), true, false));
    when(subject.isPermitted("configuration:write:workflowConfig")).thenReturn(true);

    MockHttpRequest request = MockHttpRequest.get("/v2/workflow/config");

    dispatcher.invoke(request, response);

    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getContentAsString())
      .contains("\"update\":{\"href\":\"/v2/workflow/config\"}")
      .contains("\"availableRules\":{\"href\":\"/v2/workflow/rules\"}");
  }

  @Test
  void shouldCheckPermissionWriteWorkflowConfig() throws URISyntaxException {
    doThrow(new AuthorizationException()).when(subject).checkPermission("configuration:write:workflowConfig");
    MockHttpRequest request = MockHttpRequest.put("/v2/workflow/config")
      .content("{\"rules\":[{\"rule\":\"SuccessRule\"}],\"enabled\":true, \"disableRepositoryConfiguration\":false}".getBytes())
      .contentType(WORKFLOW_MEDIA_TYPE);

    dispatcher.invoke(request, response);

    assertThat(response.getStatus()).isEqualTo(403);
    verify(configurator, never()).setEngineConfiguration(any(GlobalEngineConfiguration.class));
  }

  @Test
  void shouldSetEngineConfiguration() throws URISyntaxException {
    MockHttpRequest request = MockHttpRequest.put("/v2/workflow/config")
      .content("{\"rules\":[{\"rule\":\"SuccessRule\"}],\"enabled\":true, \"disableRepositoryConfiguration\":false}".getBytes())
      .contentType(WORKFLOW_MEDIA_TYPE);

    dispatcher.invoke(request, response);

    verify(configurator).setEngineConfiguration(any(GlobalEngineConfiguration.class));
    assertThat(response.getStatus()).isEqualTo(204);
  }

  @Test
  void shouldReturnAvailableRules() throws URISyntaxException, UnsupportedEncodingException {
    availableRules.add(new SuccessRule());
    availableRules.add(new FailureRule());
    MockHttpRequest request = MockHttpRequest.get("/v2/workflow/rules");

    dispatcher.invoke(request, response);

    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getContentAsString()).isEqualTo("[\"SuccessRule\",\"FailureRule\"]");
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
