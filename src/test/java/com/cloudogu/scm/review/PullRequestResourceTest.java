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
import sonia.scm.repository.NamespaceAndName;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SubjectAware(configuration = "classpath:com/cloudogu/scm/review/shiro.ini")
public class PullRequestResourceTest {

  private static final NamespaceAndName NAMESPACE_AND_NAME = new NamespaceAndName("space", "name");

  @Rule
  public final ShiroRule shiroRule = new ShiroRule();

  private final PullRequestStoreFactory storeFactory = mock(PullRequestStoreFactory.class);
  private final PullRequestStore store = mock(PullRequestStore.class);
  private final UriInfo uriInfo = mock(UriInfo.class);
  private final PullRequestResource pullRequestResource = new PullRequestResource(storeFactory);
  private final ArgumentCaptor<PullRequest> pullRequestStoreCaptor = ArgumentCaptor.forClass(PullRequest.class);

  private Dispatcher dispatcher;

  @Before
  public void init() {
    when(uriInfo.getAbsolutePathBuilder()).thenReturn(UriBuilder.fromPath("/scm"));
    when(storeFactory.create(NAMESPACE_AND_NAME)).thenReturn(store);
    when(store.add(pullRequestStoreCaptor.capture())).thenReturn("1");
    dispatcher = MockDispatcherFactory.createDispatcher();
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
    MockHttpResponse response = new MockHttpResponse();

    dispatcher.invoke(request, response);

    assertEquals(HttpServletResponse.SC_CREATED, response.getStatus());
    assertThat(response.getOutputHeaders().getFirst("Location").toString()).isEqualTo("/v2/pull-requests/space/name/1");
    PullRequest pullRequest = pullRequestStoreCaptor.getValue();
    assertThat(pullRequest.getAuthor()).isEqualTo("trillian");
    assertThat(pullRequest.getCreationDate()).isNotNull();
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
    MockHttpResponse response = new MockHttpResponse();

    dispatcher.invoke(request, response);

    assertEquals(HttpServletResponse.SC_BAD_REQUEST, response.getStatus());
    verify(store, never()).add(any());
  }

  private byte[] loadJson(String s) throws IOException {
    URL url = Resources.getResource(s);
    return Resources.toByteArray(url);
  }
}
