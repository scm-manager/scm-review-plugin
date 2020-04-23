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
import sonia.scm.repository.NamespaceAndName;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryManager;
import sonia.scm.web.RestDispatcher;

import javax.ws.rs.core.UriInfo;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;

import static com.cloudogu.scm.review.workflow.RepositoryEngineConfigResource.WORKFLOW_MEDIA_TYPE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RepositoryEngineConfigResourceTest {

  private static final Repository REPOSITORY = new Repository("1", "git", "space", "X");

  @Mock
  private EngineConfigurator configurator;
  @Mock
  private RepositoryManager repositoryManager;
  @Mock
  private UriInfo uriInfo;
  @Mock
  private Engine engine;

  @Mock
  private Subject subject;

  private RestDispatcher dispatcher;
  private final MockHttpResponse response = new MockHttpResponse();

  private final Set<Rule> availableRules = new HashSet<>();

  @BeforeEach
  void init() {
    RepositoryEngineConfigMapperImpl mapper = new RepositoryEngineConfigMapperImpl();
    mapper.availableRules = AvailableRules.of(SimpleRule.class);
    RepositoryEngineConfigResource repositoryEngineConfigResource = new RepositoryEngineConfigResource(repositoryManager, engine, mapper, availableRules);

    dispatcher = new RestDispatcher();
    dispatcher.addSingletonResource(repositoryEngineConfigResource);

    lenient().when(uriInfo.getBaseUri()).thenReturn(URI.create("localhost/scm/api"));
  }

  @BeforeEach
  void initSubject() {
    ThreadContext.bind(subject);
  }

  @BeforeEach
  void initEngine() {
    lenient().when(engine.configure(REPOSITORY)).thenReturn(configurator);
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
    when(configurator.getEngineConfiguration()).thenReturn(new EngineConfiguration(ImmutableList.of(AvailableRules.nameOf(SimpleRule.class)), true));

    MockHttpRequest request = MockHttpRequest.get("/v2/workflow/space/X/config");

    dispatcher.invoke(request, response);

    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getContentAsString())
      .contains("\"rules\":[\"SimpleRule\"]")
      .contains("\"enabled\":true")
      .contains("\"self\":{\"href\":\"/v2/workflow/space/X/config\"}");
  }

  @Test
  void shouldFailForUnknownRepositoryInGet() throws URISyntaxException, UnsupportedEncodingException {
    MockHttpRequest request = MockHttpRequest.get("/v2/workflow/unknown/repository/config");

    dispatcher.invoke(request, response);

    assertThat(response.getStatus()).isEqualTo(404);
  }

  @Test
  void shouldReturnConfigurationForRepositoryWithUpdateLink() throws URISyntaxException, UnsupportedEncodingException {
    when(configurator.getEngineConfiguration()).thenReturn(new EngineConfiguration(ImmutableList.of(AvailableRules.nameOf(SimpleRule.class)), true));
    when(subject.isPermitted("repository:writeWorkflowConfig:1")).thenReturn(true);

    MockHttpRequest request = MockHttpRequest.get("/v2/workflow/space/X/config");

    dispatcher.invoke(request, response);

    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getContentAsString())
      .contains("\"update\":{\"href\":\"/v2/workflow/space/X/config\"}");
  }

  @Test
  void shouldCheckRepositoryPermissionWriteWorkflowConfig() throws URISyntaxException {
    doThrow(new AuthorizationException()).when(subject).checkPermission("repository:writeWorkflowConfig:1");
    MockHttpRequest request = MockHttpRequest.put("/v2/workflow/space/X/config")
      .content("{\"rules\":[\"com.cloudogu.scm.review.workflow.RepositoryEngineConfigResourceTest$SimpleRule\"],\"enabled\":true}".getBytes())
      .contentType(WORKFLOW_MEDIA_TYPE);

    dispatcher.invoke(request, response);

    assertThat(response.getStatus()).isEqualTo(403);
    verify(configurator, never()).setEngineConfiguration(any(EngineConfiguration.class));
  }

  @Test
  void shouldFailForUnknownRepositoryInSet() throws URISyntaxException, UnsupportedEncodingException {
    MockHttpRequest request = MockHttpRequest.put("/v2/workflow/unnkown/repository/config")
      .content("{\"rules\":[\"com.cloudogu.scm.review.workflow.RepositoryEngineConfigResourceTest$SimpleRule\"],\"enabled\":true}".getBytes())
      .contentType(WORKFLOW_MEDIA_TYPE);

    dispatcher.invoke(request, response);

    assertThat(response.getStatus()).isEqualTo(404);
  }

  @Test
  void shouldSetEngineConfiguration() throws URISyntaxException {

    MockHttpRequest request = MockHttpRequest.put("/v2/workflow/space/X/config")
      .content("{\"rules\":[\"SimpleRule\"],\"enabled\":true}".getBytes())
      .contentType(WORKFLOW_MEDIA_TYPE);

    dispatcher.invoke(request, response);

    verify(configurator).setEngineConfiguration(any(EngineConfiguration.class));
    assertThat(response.getStatus()).isEqualTo(204);
  }

  public static class SimpleRule implements Rule {

    @Override
    public Result validate(Context context) {
      return success();
    }
  }
}
