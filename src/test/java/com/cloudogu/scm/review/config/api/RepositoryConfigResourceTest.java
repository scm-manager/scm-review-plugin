package com.cloudogu.scm.review.config.api;

import com.cloudogu.scm.review.config.service.ConfigService;
import com.cloudogu.scm.review.config.service.PullRequestConfig;
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
import sonia.scm.repository.NamespaceAndName;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryManager;
import sonia.scm.web.RestDispatcher;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RepositoryConfigResourceTest {

  private static final Repository REPOSITORY = new Repository("1", "git", "space", "X");

  @Mock
  private ConfigService configService;
  @Mock
  private RepositoryManager repositoryManager;
  @Mock
  private UriInfo uriInfo;

  private RestDispatcher dispatcher;
  private final MockHttpResponse response = new MockHttpResponse();

  private RepositoryConfigResource configResource;

  @BeforeEach
  void init() {
    configResource = new RepositoryConfigResource(configService, new RepositoryConfigMapperImpl(), repositoryManager);

    dispatcher = new RestDispatcher();
    dispatcher.addSingletonResource(configResource);

    lenient().when(uriInfo.getBaseUri()).thenReturn(URI.create("localhost/scm/api"));

    lenient().when(configService.getRepositoryPullRequestConfig(REPOSITORY)).thenReturn(new PullRequestConfig());
  }

  @Nested
  class WithPermissions {

    @BeforeEach
    void bindSubject() {
      ThreadContext.bind(mock(Subject.class));
    }

    @AfterEach
    void unbindSubject() {
      ThreadContext.unbindSubject();
    }

    @Nested
    class WithRepository {

      @BeforeEach
      void initRepositoryManager() {
        when(repositoryManager.get(new NamespaceAndName("space", "X"))).thenReturn(REPOSITORY);
      }

      @Test
      void shouldCreateSelfLink() throws URISyntaxException, UnsupportedEncodingException {
        MockHttpRequest request = MockHttpRequest.get("/v2/pull-requests/space/X/config");

        dispatcher.invoke(request, response);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getContentAsString()).contains("\"self\":{\"href\":\"/v2/pull-requests/space/X/config\"}");
      }

      @Test
      void shouldCreateUpdateLink() throws URISyntaxException, UnsupportedEncodingException {
        MockHttpRequest request = MockHttpRequest.get("/v2/pull-requests/space/X/config");

        dispatcher.invoke(request, response);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getContentAsString()).contains("\"update\":{\"href\":\"/v2/pull-requests/space/X/config\"}");
      }

      @Test
      void shouldGetConfig() throws URISyntaxException, UnsupportedEncodingException {
        MockHttpRequest request = MockHttpRequest.get("/v2/pull-requests/space/X/config");

        dispatcher.invoke(request, response);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getContentAsString()).contains("\"restrictBranchWriteAccess\":false");
      }

      @Test
      void shouldSetConfig() throws URISyntaxException, UnsupportedEncodingException {
        MockHttpRequest request = MockHttpRequest.put("/v2/pull-requests/space/X/config")
          .content("{\"restrictBranchWriteAccess\": true, \"protectedBranchPatterns\": [\"feature/*\"]}".getBytes())
          .contentType(MediaType.APPLICATION_JSON);

        dispatcher.invoke(request, response);

        assertThat(response.getStatus()).isEqualTo(204);
        verify(configService)
          .setRepositoryPullRequestConfig(eq(REPOSITORY), argThat(argument -> {
            assertThat(argument.isRestrictBranchWriteAccess()).isTrue();
            assertThat(argument.getProtectedBranchPatterns()).contains("feature/*");
            return true;
          }));
      }
    }

    @Test
    void shouldHandleUnknownRepository() throws URISyntaxException, UnsupportedEncodingException {
      MockHttpRequest request = MockHttpRequest.get("/v2/pull-requests/space/X/config");

      dispatcher.invoke(request, response);

      assertThat(response.getStatus()).isEqualTo(404);
    }
  }

  @Nested
  class WithoutPermission {
    @BeforeEach
    void bindSubject() {
      Subject subject = mock(Subject.class);
      doThrow(new AuthorizationException("not permitted")).when(subject).checkPermission("repository:configurePullRequest:1");
      ThreadContext.bind(subject);
    }

    @BeforeEach
    void initRepositoryManager() {
      when(repositoryManager.get(new NamespaceAndName("space", "X"))).thenReturn(REPOSITORY);
      lenient().when(configService.getRepositoryPullRequestConfig(REPOSITORY)).thenReturn(new PullRequestConfig());
    }

    @AfterEach
    void unbindSubject() {
      ThreadContext.unbindSubject();
    }

    @Test
    void shouldNotReturnConfig() throws URISyntaxException, UnsupportedEncodingException {
      MockHttpRequest request = MockHttpRequest.get("/v2/pull-requests/space/X/config");

      dispatcher.invoke(request, response);

      assertThat(response.getStatus()).isEqualTo(403);
    }

    @Test
    void shouldNotSetConfig() throws URISyntaxException, UnsupportedEncodingException {
      MockHttpRequest request = MockHttpRequest.put("/v2/pull-requests/space/X/config")
        .content("{\"enabled\": true}".getBytes())
        .contentType(MediaType.APPLICATION_JSON);

      dispatcher.invoke(request, response);

      assertThat(response.getStatus()).isEqualTo(403);
      verify(configService, never())
        .setRepositoryPullRequestConfig(any(), any());
    }
  }
}