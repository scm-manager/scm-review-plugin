package com.cloudogu.scm.review;

import com.github.sdorra.shiro.ShiroRule;
import com.github.sdorra.shiro.SubjectAware;
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

  @Rule
  public final ShiroRule shiroRule = new ShiroRule();

  private final PullRequestStoreFactory storeFactory = mock(PullRequestStoreFactory.class);
  private final PullRequestStore store = mock(PullRequestStore.class);

  @Test
  @SubjectAware(username = "trillian", password = "secret")
  public void shouldGenerateCorrectResultOnSuccess() {
    UriInfo uriInfo = mock(UriInfo.class);
    when(uriInfo.getAbsolutePathBuilder()).thenReturn(UriBuilder.fromPath("/scm"));
    when(storeFactory.create(new NamespaceAndName("space", "name"))).thenReturn(store);
    when(store.add(any())).thenReturn("1");
    PullRequestResource pullRequestResource = new PullRequestResource(storeFactory);
    Response response = pullRequestResource.create(uriInfo, "space", "name", new PullRequest());
    assertThat(response.getStatus()).isEqualTo(201);
    assertThat(response.getHeaderString("Location")).isEqualTo("/scm/1");
  }
}
