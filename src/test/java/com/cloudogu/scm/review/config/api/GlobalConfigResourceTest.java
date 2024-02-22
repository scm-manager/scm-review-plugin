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
package com.cloudogu.scm.review.config.api;

import com.cloudogu.scm.review.config.service.BasePullRequestConfig;
import com.cloudogu.scm.review.config.service.ConfigService;
import com.cloudogu.scm.review.config.service.GlobalPullRequestConfig;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.jboss.resteasy.mock.MockHttpRequest;
import org.jboss.resteasy.mock.MockHttpResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.repository.Repository;
import sonia.scm.web.RestDispatcher;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class GlobalConfigResourceTest {

  private static final Repository REPOSITORY = new Repository("1", "git", "space", "X");

  @Mock
  private ConfigService configService;
  @Mock
  private UriInfo uriInfo;

  private RestDispatcher dispatcher;
  private final MockHttpResponse response = new MockHttpResponse();

  private GlobalConfigResource configResource;

  @BeforeEach
  void init() {
    configResource = new GlobalConfigResource(configService, new GlobalConfigMapperImpl());

    dispatcher = new RestDispatcher();
    dispatcher.addSingletonResource(configResource);

    lenient().when(uriInfo.getBaseUri()).thenReturn(URI.create("localhost/scm/api"));

    lenient().when(configService.getGlobalPullRequestConfig()).thenReturn(new GlobalPullRequestConfig());
  }

  @Nested
  class WithReadPermissions {

    Subject subject = mock(Subject.class);

    @BeforeEach
    void bindSubject() {
      lenient().when(subject.isPermitted("configuration:read:pullRequest")).thenReturn(true);
      doThrow(new AuthorizationException()).when(subject).checkPermission("configuration:write:pullRequest");
      ThreadContext.bind(subject);
    }

    @AfterEach
    void unbindSubject() {
      ThreadContext.unbindSubject();
    }

    @Test
    void shouldCreateSelfLink() throws URISyntaxException, UnsupportedEncodingException {
      MockHttpRequest request = MockHttpRequest.get("/v2/pull-requests/config");

      dispatcher.invoke(request, response);

      assertThat(response.getStatus()).isEqualTo(200);
      assertThat(response.getContentAsString()).contains("\"self\":{\"href\":\"/v2/pull-requests/config\"}");
    }

    @Test
    void shouldGetConfig() throws URISyntaxException, UnsupportedEncodingException {
      MockHttpRequest request = MockHttpRequest.get("/v2/pull-requests/config");

      dispatcher.invoke(request, response);

      assertThat(response.getStatus()).isEqualTo(200);
      assertThat(response.getContentAsString()).contains("\"restrictBranchWriteAccess\":false");
    }

    @Nested
    class WithWritePermission {

      @BeforeEach
      void giveWritePermission() {
        lenient().when(subject.isPermitted("configuration:write:pullRequest")).thenReturn(true);
        doNothing().when(subject).checkPermission("configuration:write:pullRequest");
      }

      @Test
      void shouldCreateUpdateLink() throws URISyntaxException, UnsupportedEncodingException {
        MockHttpRequest request = MockHttpRequest.get("/v2/pull-requests/config");

        dispatcher.invoke(request, response);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getContentAsString()).contains("\"update\":{\"href\":\"/v2/pull-requests/config\"}");
      }

      @Test
      void shouldSetConfig() throws URISyntaxException {
        MockHttpRequest request = MockHttpRequest.put("/v2/pull-requests/config")
          .content("{\"restrictBranchWriteAccess\": true, \"protectedBranchPatterns\": [{\"branch\":\"feature/*\", \"path\":\"*\"}]}".getBytes())
          .contentType(MediaType.APPLICATION_JSON);

        dispatcher.invoke(request, response);

        assertThat(response.getStatus()).isEqualTo(204);
        verify(configService)
          .setGlobalPullRequestConfig(argThat(argument -> {
            assertThat(argument.isRestrictBranchWriteAccess()).isTrue();
            assertThat(argument.getProtectedBranchPatterns().get(0).getBranch()).contains("feature/*");
            assertThat(argument.getProtectedBranchPatterns().get(0).getPath()).contains("*");
            return true;
          }));
      }
    }

    @Nested
    class WithoutWritePermission {

      @BeforeEach
      void revokeWritePermission() {
        lenient().when(subject.isPermitted("configuration:write:pullRequest")).thenReturn(false);
        doThrow(new AuthorizationException()).when(subject).checkPermission("configuration:write:pullRequest");
      }

      @Test
      void shouldNotCreateUpdateLink() throws URISyntaxException, UnsupportedEncodingException {
        MockHttpRequest request = MockHttpRequest.get("/v2/pull-requests/config");

        dispatcher.invoke(request, response);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getContentAsString()).doesNotContain("\"update\"");
      }

      @Test
      void shouldSetConfig() throws URISyntaxException {
        MockHttpRequest request = MockHttpRequest.put("/v2/pull-requests/config")
          .content("{\"enabled\": true, \"protectedBranchPatterns\": [{\"branch\":\"feature/*\", \"path\":\"*\"}]}".getBytes())
          .contentType(MediaType.APPLICATION_JSON);

        dispatcher.invoke(request, response);

        assertThat(response.getStatus()).isEqualTo(403);
        verify(configService, never()).setGlobalPullRequestConfig(any());
      }
    }
  }

  @Nested
  class WithoutPermission {
    @BeforeEach
    void bindSubject() {
      Subject subject = mock(Subject.class);
      doThrow(new AuthorizationException("not permitted")).when(subject).checkPermission("configuration:read:pullRequest");
      ThreadContext.bind(subject);
    }

    @AfterEach
    void unbindSubject() {
      ThreadContext.unbindSubject();
    }

    @Test
    void shouldNotReturnConfig() throws URISyntaxException {
      MockHttpRequest request = MockHttpRequest.get("/v2/pull-requests/config");

      dispatcher.invoke(request, response);

      assertThat(response.getStatus()).isEqualTo(403);
    }
  }
}
