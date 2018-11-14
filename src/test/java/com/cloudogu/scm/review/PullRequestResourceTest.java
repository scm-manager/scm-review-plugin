package com.cloudogu.scm.review;

import com.github.sdorra.shiro.ShiroRule;
import com.github.sdorra.shiro.SubjectAware;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import sonia.scm.repository.NamespaceAndName;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
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

  @Before
  public void init() {
    when(uriInfo.getAbsolutePathBuilder()).thenReturn(UriBuilder.fromPath("/scm"));
    when(storeFactory.create(NAMESPACE_AND_NAME)).thenReturn(store);
  }

  @Test
  @SubjectAware(username = "trillian", password = "secret")
  public void shouldGenerateCorrectResultOnSuccess() {
    when(store.add(any())).thenReturn("1");

    PullRequest pullRequest = new PullRequest("b1", "b2", "title", "description", null, null);
    Response response =
      pullRequestResource.create(uriInfo, NAMESPACE_AND_NAME.getNamespace(), NAMESPACE_AND_NAME.getName(), pullRequest);

    assertThat(response.getStatus()).isEqualTo(201);
    assertThat(response.getHeaderString("Location")).isEqualTo("/scm/1");
    assertThat(pullRequest.getAuthor()).isEqualTo("trillian");
    assertThat(pullRequest.getCreationDate()).isNotNull();
  }
}
