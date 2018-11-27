package com.cloudogu.scm.review;

import com.github.sdorra.shiro.ShiroRule;
import com.github.sdorra.shiro.SubjectAware;
import com.google.common.io.Resources;
import org.jboss.resteasy.core.Dispatcher;
import org.jboss.resteasy.mock.MockDispatcherFactory;
import org.jboss.resteasy.mock.MockHttpRequest;
import org.jboss.resteasy.mock.MockHttpResponse;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import sonia.scm.NotFoundException;
import sonia.scm.repository.NamespaceAndName;
import sonia.scm.repository.Repository;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Instant;
import java.util.Optional;

import static com.cloudogu.scm.review.ExceptionMessageMapper.assertExceptionFrom;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SubjectAware(configuration = "classpath:com/cloudogu/scm/review/shiro.ini")
public class PullRequestResourceTest {

  @Rule
  public final ShiroRule shiroRule = new ShiroRule();

  private final RepositoryResolver repositoryResolver = mock(RepositoryResolver.class);
  private final BranchResolver branchResolver = mock(BranchResolver.class);
  private final PullRequestStoreFactory storeFactory = mock(PullRequestStoreFactory.class);
  private final PullRequestStore store = mock(PullRequestStore.class);
  private final Repository repository = mock(Repository.class);
  private final UriInfo uriInfo = mock(UriInfo.class);
  private final PullRequestResource pullRequestResource = new PullRequestResource(repositoryResolver, branchResolver, storeFactory);
  private final ArgumentCaptor<PullRequest> pullRequestStoreCaptor = ArgumentCaptor.forClass(PullRequest.class);

  private Dispatcher dispatcher;

  private final MockHttpResponse response = new MockHttpResponse();

  @Before
  public void init() {
    when(uriInfo.getAbsolutePathBuilder()).thenReturn(UriBuilder.fromPath("/scm"));
    when(storeFactory.create(null)).thenReturn(store);
    when(storeFactory.create(repository)).thenReturn(store);
    when(store.add(any(), pullRequestStoreCaptor.capture())).thenReturn("1");
    dispatcher = MockDispatcherFactory.createDispatcher();
    dispatcher.getProviderFactory().register(new ExceptionMessageMapper());
    dispatcher.getRegistry().addSingletonResource(pullRequestResource);
  }

  @Test
  @SubjectAware(username = "trillian", password = "secret")
  public void shouldCreateNewValidPullRequest() throws URISyntaxException, IOException {
    byte[] pullRequestJson = loadJson("com/cloudogu/scm/review/pullRequest.json");
    MockHttpRequest request =
      MockHttpRequest
        .post("/" + PullRequestResource.PULL_REQUESTS_PATH_V2 + "/space/name")
        .content(pullRequestJson)
        .contentType(MediaType.APPLICATION_JSON);

    dispatcher.invoke(request, response);

    assertEquals(HttpServletResponse.SC_CREATED, response.getStatus());
    assertThat(response.getOutputHeaders().getFirst("Location").toString()).isEqualTo("/v2/pull-requests/space/name/1");
    PullRequest pullRequest = pullRequestStoreCaptor.getValue();
    assertThat(pullRequest.getAuthor()).isEqualTo("trillian");
  }

  @Test
  @SubjectAware(username = "trillian", password = "secret")
  public void shouldRejectInvalidPullRequest() throws URISyntaxException, IOException {
    byte[] pullRequestJson = loadJson("com/cloudogu/scm/review/pullRequest_invalid.json");
    MockHttpRequest request =
      MockHttpRequest
        .post("/" + PullRequestResource.PULL_REQUESTS_PATH_V2 + "/space/name")
        .content(pullRequestJson)
        .contentType(MediaType.APPLICATION_JSON);

    dispatcher.invoke(request, response);

    assertEquals(HttpServletResponse.SC_BAD_REQUEST, response.getStatus());
    verify(store, never()).add(any(), any());
  }

  @Test
  @SubjectAware(username = "trillian", password = "secret")
  public void shouldRejectSameBranches() throws URISyntaxException, IOException {
    byte[] pullRequestJson = loadJson("com/cloudogu/scm/review/pullRequest_sameBranches.json");
    MockHttpRequest request =
      MockHttpRequest
        .post("/" + PullRequestResource.PULL_REQUESTS_PATH_V2 + "/space/name")
        .content(pullRequestJson)
        .contentType(MediaType.APPLICATION_JSON);

    dispatcher.invoke(request, response);

    assertEquals(HttpServletResponse.SC_BAD_REQUEST, response.getStatus());
    verify(store, never()).add(any(), any());
  }

  @Test
  @SubjectAware(username = "trillian", password = "secret")
  public void shouldHandleMissingRepository() throws URISyntaxException, IOException {
    when(repositoryResolver.resolve(new NamespaceAndName("space", "X")))
      .thenThrow(new NotFoundException("x", "y"));
    byte[] pullRequestJson = loadJson("com/cloudogu/scm/review/pullRequest.json");
    MockHttpRequest request =
      MockHttpRequest
        .post("/" + PullRequestResource.PULL_REQUESTS_PATH_V2 + "/space/X")
        .content(pullRequestJson)
        .contentType(MediaType.APPLICATION_JSON);

    dispatcher.invoke(request, response);

    assertExceptionFrom(response)
      .isOffClass(NotFoundException.class)
      .hasMessageMatching("could not find.*");
  }

  @Test
  @SubjectAware(username = "trillian", password = "secret")
  public void shouldHandleMissingBranch() throws URISyntaxException, IOException {
    Repository repository = new Repository();
    when(repositoryResolver.resolve(new NamespaceAndName("space", "X")))
      .thenReturn(repository);
    when(branchResolver.resolve(repository, "sourceBranch")).thenThrow(new NotFoundException("x", "y"));
    byte[] pullRequestJson = loadJson("com/cloudogu/scm/review/pullRequest.json");
    MockHttpRequest request =
      MockHttpRequest
        .post("/" + PullRequestResource.PULL_REQUESTS_PATH_V2 + "/space/X")
        .content(pullRequestJson)
        .contentType(MediaType.APPLICATION_JSON);

    dispatcher.invoke(request, response);

    assertExceptionFrom(response)
      .isOffClass(NotFoundException.class)
      .hasMessageMatching("could not find.*");
  }

  @Test
  @SubjectAware(username = "trillian", password = "secret")
  public void shouldGetPullRequest() throws URISyntaxException {
    when(repository.getNamespace()).thenReturn("foo");
    when(repository.getName()).thenReturn("bar");
    when(repositoryResolver.resolve(new NamespaceAndName("foo", "bar"))).thenReturn(repository);
    PullRequest pullRequest = new PullRequest("source", "target", "title");
    pullRequest.setAuthor("A. U. Thor");
    pullRequest.setId("id");
    pullRequest.setCreationDate(Instant.MIN);
    when(store.get(repository, "123")).thenReturn(pullRequest);
    MockHttpRequest request = MockHttpRequest.get("/" + PullRequestResource.PULL_REQUESTS_PATH_V2 + "/foo/bar/123");
    dispatcher.invoke(request, response);
    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getContentAsString()).contains("_links");
  }


  private byte[] loadJson(String s) throws IOException {
    URL url = Resources.getResource(s);
    return Resources.toByteArray(url);
  }
}
