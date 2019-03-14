package com.cloudogu.scm.review.comment.api;

import com.cloudogu.scm.review.ExceptionMessageMapper;
import com.cloudogu.scm.review.RepositoryResolver;
import com.cloudogu.scm.review.comment.dto.PullRequestCommentMapperImpl;
import com.cloudogu.scm.review.comment.service.CommentService;
import com.cloudogu.scm.review.comment.service.Location;
import com.cloudogu.scm.review.comment.service.PullRequestComment;
import com.cloudogu.scm.review.pullrequest.api.PullRequestResource;
import com.cloudogu.scm.review.pullrequest.api.PullRequestRootResource;
import com.cloudogu.scm.review.pullrequest.dto.PullRequestMapperImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.sdorra.shiro.ShiroRule;
import com.github.sdorra.shiro.SubjectAware;
import com.google.common.collect.Lists;
import com.google.inject.util.Providers;
import org.jboss.resteasy.core.Dispatcher;
import org.jboss.resteasy.mock.MockDispatcherFactory;
import org.jboss.resteasy.mock.MockHttpRequest;
import org.jboss.resteasy.mock.MockHttpResponse;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import sonia.scm.repository.NamespaceAndName;
import sonia.scm.repository.Repository;

import javax.inject.Provider;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.ArrayList;

import static com.cloudogu.scm.review.ExceptionMessageMapper.assertExceptionFrom;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SubjectAware(configuration = "classpath:com/cloudogu/scm/review/shiro.ini")
@RunWith(MockitoJUnitRunner.Silent.class)
public class CommentRootResourceTest {

  @Rule
  public final ShiroRule shiroRule = new ShiroRule();

  private final Repository repository = mock(Repository.class);
  private final UriInfo uriInfo = mock(UriInfo.class);

  private Dispatcher dispatcher;

  private final MockHttpResponse response = new MockHttpResponse();

  private static final String REPOSITORY_NAME = "name";
  private static final String REPOSITORY_ID = "repo_ID";
  private static final String REPOSITORY_NAMESPACE = "space";

  @Mock
  private RepositoryResolver repositoryResolver;

  @Mock
  private CommentService service;

  @Mock
  private Provider<CommentResource> commentResourceProvider;

  @Mock
  private CommentService commentService;


  @Before
  public void init() {
    when(repository.getId()).thenReturn(REPOSITORY_ID);
    when(repository.getName()).thenReturn(REPOSITORY_NAME);
    when(repository.getNamespace()).thenReturn(REPOSITORY_NAMESPACE);
    when(repository.getNamespaceAndName()).thenReturn(new NamespaceAndName(REPOSITORY_NAMESPACE, REPOSITORY_NAME));
    when(repositoryResolver.resolve(any())).thenReturn(repository);
    CommentRootResource resource = new CommentRootResource(new PullRequestCommentMapperImpl(), repositoryResolver, service, commentResourceProvider);
    when(uriInfo.getAbsolutePathBuilder()).thenReturn(UriBuilder.fromPath("/scm"));
    dispatcher = MockDispatcherFactory.createDispatcher();
    dispatcher.getProviderFactory().register(new ExceptionMessageMapper());
    PullRequestRootResource pullRequestRootResource = new PullRequestRootResource(new PullRequestMapperImpl(), null,
      Providers.of(new PullRequestResource(new PullRequestMapperImpl(), null, Providers.of(resource), commentService)));
    dispatcher.getRegistry().addSingletonResource(pullRequestRootResource);
  }

  @Test
  @SubjectAware(username = "slarti", password = "secret")
  public void shouldCreateNewPullRequestComment() throws URISyntaxException {
    when(service.add(eq("space"), eq("name"), eq("1"), argThat(t -> t.getAuthor().equals("slarti") && t.getDate() != null))).thenReturn("1");
    byte[] pullRequestCommentJson = "{\"comment\" : \"this is my comment\"}".getBytes();
    MockHttpRequest request =
      MockHttpRequest
        .post("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/space/name/1/comments")
        .content(pullRequestCommentJson)
        .contentType(MediaType.APPLICATION_JSON);

    dispatcher.invoke(request, response);

    assertEquals(HttpServletResponse.SC_CREATED, response.getStatus());
    assertThat(response.getOutputHeaders().getFirst("Location").toString()).isEqualTo("/v2/pull-requests/space/name/1/comments/1");
  }

  @Test
  @SubjectAware(username = "trillian", password = "secret")
  public void shouldGetUnauthorizedExceptionWhenMissingPermissionOnCreatePRComment() throws URISyntaxException, UnsupportedEncodingException {
    byte[] pullRequestCommentJson = "{\"comment\" : \"this is my comment\"}".getBytes();
    MockHttpRequest request =
      MockHttpRequest
        .post("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/space/name/1/comments")
        .content(pullRequestCommentJson)
        .contentType(MediaType.APPLICATION_JSON);
    dispatcher.invoke(request, response);
    assertExceptionFrom(response).hasMessageMatching("Subject does not have permission \\[repository:commentPullRequest:repo_ID\\]");
  }


  @Test
  @SubjectAware(username = "slarti", password = "secret")
  public void shouldGetAllPullRequestComments() throws URISyntaxException, IOException {
    PullRequestComment comment1 = new PullRequestComment("1", "1. comment", "author", new Location("","",""), Instant.now());
    PullRequestComment comment2 = new PullRequestComment("2", "2. comment", "author", new Location("","",""), Instant.now());
    PullRequestComment comment3 = new PullRequestComment("3", "3. comment", "author", new Location("","",""), Instant.now());
    ArrayList<PullRequestComment> list = Lists.newArrayList(comment1, comment2, comment3);
    when(service.getAll("space", "name", "1")).thenReturn(list);
    when(service.get("space", "name", "1", "1")).thenReturn(comment1);
    when(service.get("space", "name", "1", "2")).thenReturn(comment2);
    when(service.get("space", "name", "1", "3")).thenReturn(comment3);
    MockHttpRequest request =
      MockHttpRequest
        .get("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/space/name/1/comments")
        .contentType(MediaType.APPLICATION_JSON);

    dispatcher.invoke(request, response);

    assertEquals(HttpServletResponse.SC_OK, response.getStatus());
    ObjectMapper mapper = new ObjectMapper();
    JsonNode jsonNode = mapper.readValue(response.getContentAsString(), JsonNode.class);
    JsonNode prNode = jsonNode.get("_embedded").get("pullRequestComments");
    JsonNode comment_1 = prNode.path(0);
    JsonNode comment_2 = prNode.path(1);
    JsonNode comment_3 = prNode.path(2);
    assertThat(comment_1.get("comment").asText()).isEqualTo("1. comment");
    assertThat(comment_2.get("comment").asText()).isEqualTo("2. comment");
    assertThat(comment_3.get("comment").asText()).isEqualTo("3. comment");
  }


  @Test
  @SubjectAware(username = "trillian", password = "secret")
  public void shouldGetUnauthorizedExceptionWhenMissingPermissionOnGetAllPRComment() throws URISyntaxException, IOException {
    PullRequestComment comment1 = new PullRequestComment("1", "1. comment", "author", new Location("","",""), Instant.now());
    PullRequestComment comment2 = new PullRequestComment("2", "2. comment", "author", new Location("","",""), Instant.now());
    PullRequestComment comment3 = new PullRequestComment("3", "3. comment", "author", new Location("","",""), Instant.now());
    ArrayList<PullRequestComment> list = Lists.newArrayList(comment1, comment2, comment3);
    when(service.getAll("space", "name", "1")).thenReturn(list);
    when(service.get("space", "name", "1", "1")).thenReturn(comment1);
    when(service.get("space", "name", "1", "2")).thenReturn(comment2);
    when(service.get("space", "name", "1", "3")).thenReturn(comment3);
    MockHttpRequest request =
      MockHttpRequest
        .get("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/space/name/1/comments")
        .contentType(MediaType.APPLICATION_JSON);

    dispatcher.invoke(request, response);
    assertExceptionFrom(response).hasMessageMatching( "User is not permitted to read pull requests");
  }

  @Test
  @SubjectAware(username = "slarti", password = "secret")
  public void shouldGetAllLinksÍfTheAuthorIsTheCurrentUser() throws URISyntaxException, IOException {
    PullRequestComment comment1 = new PullRequestComment("1", "1. comment", "author", new Location("","",""), Instant.now());
    PullRequestComment comment2 = new PullRequestComment("2", "2. comment", "author", new Location("","",""), Instant.now());
    PullRequestComment comment3 = new PullRequestComment("3", "3. comment", "author", new Location("","",""), Instant.now());
    ArrayList<PullRequestComment> list = Lists.newArrayList(comment1, comment2, comment3);
    when(service.getAll("space", "name", "1")).thenReturn(list);
    when(service.get("space", "name", "1", "1")).thenReturn(comment1);
    when(service.get("space", "name", "1", "2")).thenReturn(comment2);
    when(service.get("space", "name", "1", "3")).thenReturn(comment3);
    MockHttpRequest request =
      MockHttpRequest
        .get("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/space/name/1/comments")
        .contentType(MediaType.APPLICATION_JSON);

    dispatcher.invoke(request, response);

    assertEquals(HttpServletResponse.SC_OK, response.getStatus());
    ObjectMapper mapper = new ObjectMapper();
    JsonNode jsonNode = mapper.readValue(response.getContentAsString(), JsonNode.class);
    JsonNode prNode = jsonNode.get("_embedded").get("pullRequestComments");
    JsonNode comment_1 = prNode.path(0);
    JsonNode comment_2 = prNode.path(1);
    JsonNode comment_3 = prNode.path(2);

    assertThat(comment_1.get("_links").get("self").get("href").asText()).isEqualTo("/v2/pull-requests/space/name/1/comments/1");
    assertThat(comment_2.get("_links").get("self").get("href").asText()).isEqualTo("/v2/pull-requests/space/name/1/comments/2");
    assertThat(comment_3.get("_links").get("self").get("href").asText()).isEqualTo("/v2/pull-requests/space/name/1/comments/3");

    assertThat(comment_1.get("_links").get("update").get("href").asText()).isEqualTo("/v2/pull-requests/space/name/1/comments/1");
    assertThat(comment_2.get("_links").get("update").get("href").asText()).isEqualTo("/v2/pull-requests/space/name/1/comments/2");
    assertThat(comment_3.get("_links").get("update").get("href").asText()).isEqualTo("/v2/pull-requests/space/name/1/comments/3");

    assertThat(comment_1.get("_links").get("delete").get("href").asText()).isEqualTo("/v2/pull-requests/space/name/1/comments/1");
    assertThat(comment_2.get("_links").get("delete").get("href").asText()).isEqualTo("/v2/pull-requests/space/name/1/comments/2");
    assertThat(comment_3.get("_links").get("delete").get("href").asText()).isEqualTo("/v2/pull-requests/space/name/1/comments/3");
  }

  @Test
  @SubjectAware(username = "other", password = "secret")
  public void shouldGetOnlyTheSelfLinkÍfTheAuthorIsNotTheCurrentUser() throws URISyntaxException, IOException {
    PullRequestComment comment1 = new PullRequestComment("1", "1. comment", "author", new Location("","",""), Instant.now());
    PullRequestComment comment2 = new PullRequestComment("2", "2. comment", "author", new Location("","",""), Instant.now());
    PullRequestComment comment3 = new PullRequestComment("3", "3. comment", "author", new Location("","",""), Instant.now());
    ArrayList<PullRequestComment> list = Lists.newArrayList(comment1, comment2, comment3);
    when(service.getAll("space", "name", "1")).thenReturn(list);
    when(service.get("space", "name", "1", "1")).thenReturn(comment1);
    when(service.get("space", "name", "1", "2")).thenReturn(comment2);
    when(service.get("space", "name", "1", "3")).thenReturn(comment3);
    MockHttpRequest request =
      MockHttpRequest
        .get("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/space/name/1/comments")
        .contentType(MediaType.APPLICATION_JSON);

    dispatcher.invoke(request, response);

    assertEquals(HttpServletResponse.SC_OK, response.getStatus());
    ObjectMapper mapper = new ObjectMapper();
    JsonNode jsonNode = mapper.readValue(response.getContentAsString(), JsonNode.class);
    JsonNode prNode = jsonNode.get("_embedded").get("pullRequestComments");
    JsonNode comment_1 = prNode.path(0);
    JsonNode comment_2 = prNode.path(1);
    JsonNode comment_3 = prNode.path(2);

    assertThat(comment_1.get("_links").get("self").get("href").asText()).isEqualTo("/v2/pull-requests/space/name/1/comments/1");
    assertThat(comment_2.get("_links").get("self").get("href").asText()).isEqualTo("/v2/pull-requests/space/name/1/comments/2");
    assertThat(comment_3.get("_links").get("self").get("href").asText()).isEqualTo("/v2/pull-requests/space/name/1/comments/3");

    assertThat(comment_1.get("_links").get("update")).isNull();
    assertThat(comment_2.get("_links").get("update")).isNull();
    assertThat(comment_3.get("_links").get("update")).isNull();

    assertThat(comment_1.get("_links").get("delete")).isNull();
    assertThat(comment_2.get("_links").get("delete")).isNull();
    assertThat(comment_3.get("_links").get("delete")).isNull();
  }


}
