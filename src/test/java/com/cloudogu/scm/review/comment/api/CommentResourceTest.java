package com.cloudogu.scm.review.comment.api;

import com.cloudogu.scm.review.ExceptionMessageMapper;
import com.cloudogu.scm.review.RepositoryResolver;
import com.cloudogu.scm.review.comment.service.CommentService;
import com.cloudogu.scm.review.comment.service.Location;
import com.cloudogu.scm.review.comment.service.PullRequestComment;
import com.cloudogu.scm.review.comment.service.PullRequestRootComment;
import com.cloudogu.scm.review.pullrequest.api.PullRequestResource;
import com.cloudogu.scm.review.pullrequest.api.PullRequestRootResource;
import com.cloudogu.scm.review.pullrequest.dto.PullRequestMapperImpl;
import com.github.sdorra.shiro.ShiroRule;
import com.github.sdorra.shiro.SubjectAware;
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
import sonia.scm.event.ScmEventBus;
import sonia.scm.repository.NamespaceAndName;
import sonia.scm.repository.Repository;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.time.Instant;

import static com.cloudogu.scm.review.comment.service.PullRequestRootComment.createComment;
import static java.net.URI.create;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SubjectAware(configuration = "classpath:com/cloudogu/scm/review/shiro.ini")
@RunWith(MockitoJUnitRunner.Silent.class)
public class CommentResourceTest {
//
//  @Rule
//  public final ShiroRule shiroRule = new ShiroRule();
//
//  private final Repository repository = mock(Repository.class);
//  private final UriInfo uriInfo = mock(UriInfo.class);
//
//  private Dispatcher dispatcher;
//
//  private final MockHttpResponse response = new MockHttpResponse();
//
//  private static final String REPOSITORY_NAME = "name";
//  private static final String REPOSITORY_ID = "repo_ID";
//  private static final String REPOSITORY_NAMESPACE = "space";
//
//  @Mock
//  private RepositoryResolver repositoryResolver;
//
//  @Mock
//  private CommentService service;
//  @Mock
//  private ScmEventBus eventBus;
//  @Mock
//  private CommentService commentService;
//  private CommentPathBuilder commentPathBuilder = CommentPathBuilderMock.createMock("https://scm-manager.org/scm/api/v2");
//
//  @Before
//  public void init() {
//    when(repository.getId()).thenReturn(REPOSITORY_ID);
//    when(repository.getName()).thenReturn(REPOSITORY_NAME);
//    when(repository.getNamespace()).thenReturn(REPOSITORY_NAMESPACE);
//    when(repository.getNamespaceAndName()).thenReturn(new NamespaceAndName(REPOSITORY_NAMESPACE, REPOSITORY_NAME));
//    when(repositoryResolver.resolve(any())).thenReturn(repository);
//    CommentResource resource = new CommentResource(service, repositoryResolver, new PullRequestCommentMapperImpl(), commentPathBuilder);
//    when(uriInfo.getAbsolutePathBuilder()).thenReturn(UriBuilder.fromPath("/scm"));
//    dispatcher = MockDispatcherFactory.createDispatcher();
//    dispatcher.getProviderFactory().register(new ExceptionMessageMapper());
//    PullRequestRootResource pullRequestRootResource = new PullRequestRootResource(new PullRequestMapperImpl(), null,
//      Providers.of(new PullRequestResource(new PullRequestMapperImpl(), null,
//        Providers.of(new CommentRootResource(new PullRequestCommentMapperImpl(), repositoryResolver, service, Providers.of(resource), commentPathBuilder)), commentService,  eventBus)));
//    dispatcher.getRegistry().addSingletonResource(pullRequestRootResource);
//  }
//
//  @Test
//  @SubjectAware(username = "slarti", password = "secret")
//  public void shouldDeleteCommentÍfTheAuthorIsTheCurrentUser() throws URISyntaxException {
//    PullRequestComment comment = createComment("1", "1. comment", "slarti", new Location());
//    when(service.get("space", "name", "1", "1")).thenReturn(comment);
//    doNothing().when(service).delete(repository, "1", "1");
//    MockHttpRequest request =
//      MockHttpRequest
//        .delete("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/space/name/1/comments/1")
//        .contentType(MediaType.APPLICATION_JSON);
//
//    dispatcher.invoke(request, response);
//
//    assertEquals(HttpServletResponse.SC_NO_CONTENT, response.getStatus());
//  }
//
//  @Test
//  @SubjectAware(username = "slarti", password = "secret")
//  public void shouldDeleteCommentÍfTheCurrentUserIsDifferentWithAuthorButHasPushPermission() throws URISyntaxException {
//    PullRequestComment comment = createComment("1", "1. comment", "author", new Location());
//    when(service.get("space", "name", "1", "1")).thenReturn(comment);
//    doNothing().when(service).delete(repository, "1", "1");
//    MockHttpRequest request =
//      MockHttpRequest
//        .delete("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/space/name/1/comments/1")
//        .contentType(MediaType.APPLICATION_JSON);
//
//    dispatcher.invoke(request, response);
//
//    assertEquals(HttpServletResponse.SC_NO_CONTENT, response.getStatus());
//  }
//
//  @Test
//  @SubjectAware(username = "trillian", password = "secret")
//  public void shouldGetUnauthorizedExceptionWhenMissingPermissionOnDeletePRComment() throws URISyntaxException, UnsupportedEncodingException {
//    PullRequestComment comment = createComment("1", "1. comment", "slarti", new Location());
//    when(service.get("space", "name", "1", "1")).thenReturn(comment);
//    doNothing().when(service).delete(repository, "1", "1");
//    MockHttpRequest request =
//      MockHttpRequest
//        .delete("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/space/name/1/comments/1")
//        .contentType(MediaType.APPLICATION_JSON);
//
//    dispatcher.invoke(request, response);
//    assertEquals(HttpServletResponse.SC_FORBIDDEN, response.getStatus());
//  }
//
//
//  @Test
//  @SubjectAware(username = "rr", password = "secret")
//  public void shouldForbiddenDeleteÍfTheAuthorIsNotTheCurrentUserAndThePushPermissionIsMissed() throws URISyntaxException {
//    PullRequestComment comment = createComment("1", "1. comment", "author", new Location());
//    when(service.get("space", "name", "1", "1")).thenReturn(comment);
//    doNothing().when(service).delete(repository, "1", "1");
//    MockHttpRequest request =
//      MockHttpRequest
//        .delete("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/space/name/1/comments/1")
//        .contentType(MediaType.APPLICATION_JSON);
//
//    dispatcher.invoke(request, response);
//
//    assertEquals(HttpServletResponse.SC_FORBIDDEN, response.getStatus());
//  }
//
//  @Test
//  @SubjectAware(username = "slarti", password = "secret")
//  public void shouldUpdateCommentÍfTheAuthorIsTheCurrentUser() throws URISyntaxException {
//    PullRequestComment comment = createComment("1", "1. comment", "slarti", new Location());
//    when(service.get("space", "name", "1", "1")).thenReturn(comment);
//    doNothing().when(service).delete(repository, "1", "1");
//    String newComment = "haha ";
//    when(service.add(eq(repository), eq("1"),
//      argThat(t -> t.getAuthor().equals("slarti")
//        && t.getDate() != null
//        && t.getComment().equals(newComment)
//      ))).thenReturn("1");
//    byte[] pullRequestCommentJson = ("{\"comment\" : \"" + newComment + "\"}").getBytes();
//    MockHttpRequest request =
//      MockHttpRequest
//        .put("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/space/name/1/comments/1")
//        .content(pullRequestCommentJson)
//        .contentType(MediaType.APPLICATION_JSON);
//
//    dispatcher.invoke(request, response);
//
//    assertEquals(HttpServletResponse.SC_NO_CONTENT, response.getStatus());
//  }
//
//  @Test
//  @SubjectAware(username = "slarti", password = "secret")
//  public void shouldUpdateCommentIfTheCurrentUserIsDifferentWithAuthorButHasPushPermission() throws URISyntaxException {
//    PullRequestComment comment = createComment("1", "1. comment", "author", new Location());
//    when(service.get("space", "name", "1", "1")).thenReturn(comment);
//    doNothing().when(service).delete(repository, "1", "1");
//    String newComment = "haha ";
//    when(service.add(eq(repository), eq("1"),
//      argThat(t -> t.getAuthor().equals("slarti")
//        && t.getDate() != null
//        && t.getComment().equals(newComment)
//      ))).thenReturn("1");
//    byte[] pullRequestCommentJson = ("{\"comment\" : \"" + newComment + "\"}").getBytes();
//    MockHttpRequest request =
//      MockHttpRequest
//        .put("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/space/name/1/comments/1")
//        .content(pullRequestCommentJson)
//        .contentType(MediaType.APPLICATION_JSON);
//
//    dispatcher.invoke(request, response);
//
//    assertEquals(HttpServletResponse.SC_NO_CONTENT, response.getStatus());
//  }
//
//  @Test
//  @SubjectAware(username = "rr", password = "secret")
//  public void shouldForbiddenUpdateIfTheAuthorIsNotTheCurrentUserAndThePushPermissionIsMissed() throws URISyntaxException {
//    PullRequestComment comment = createComment("1", "1. comment", "author", new Location());
//    when(service.get("space", "name", "1", "1")).thenReturn(comment);
//    doNothing().when(service).delete(repository, "1", "1");
//    String newComment = "haha ";
//    when(service.add(eq(repository), eq("1"),
//      argThat(t -> t.getAuthor().equals("author")
//        && t.getDate() != null
//        && t.getComment().equals(newComment)
//      ))).thenReturn("1");
//    byte[] pullRequestCommentJson = ("{\"comment\" : \"" + newComment + "\"}").getBytes();
//    MockHttpRequest request =
//      MockHttpRequest
//        .put("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/space/name/1/comments/1")
//        .content(pullRequestCommentJson)
//        .contentType(MediaType.APPLICATION_JSON);
//
//    dispatcher.invoke(request, response);
//
//    assertEquals(HttpServletResponse.SC_FORBIDDEN, response.getStatus());
//  }
//
//  @Test
//  @SubjectAware(username = "trillian", password = "secret")
//  public void shouldGetUnauthorizedExceptionWhenMissingPermissionOnUpdatePRComment() throws URISyntaxException, UnsupportedEncodingException {
//    PullRequestComment comment = createComment("1", "1. comment", "author", new Location());
//    when(service.get("space", "name", "1", "1")).thenReturn(comment);
//    doNothing().when(service).delete(repository, "1", "1");
//    String newComment = "haha ";
//    when(service.add(eq(repository), eq("1"),
//      argThat(t -> t.getAuthor().equals("author")
//        && t.getDate() != null
//        && t.getComment().equals(newComment)
//      ))).thenReturn("1");
//    byte[] pullRequestCommentJson = ("{\"comment\" : \"" + newComment + "\"}").getBytes();
//    MockHttpRequest request =
//      MockHttpRequest
//        .put("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/space/name/1/comments/1")
//        .content(pullRequestCommentJson)
//        .contentType(MediaType.APPLICATION_JSON);
//
//    dispatcher.invoke(request, response);
//    assertEquals(HttpServletResponse.SC_FORBIDDEN, response.getStatus());
//  }
//
//  @Test
//  @SubjectAware(username = "slarti", password = "secret")
//  public void shouldReply() throws URISyntaxException, UnsupportedEncodingException {
//    PullRequestRootComment comment = createComment("1", "1. comment", "slarti", new Location());
//    when(service.get("space", "name", "1", "1")).thenReturn(comment);
//    when(service.reply(eq(repository), eq("1"), eq(comment), any())).thenReturn("new");
//    String newComment = "haha ";
//    byte[] pullRequestCommentJson = ("{\"comment\" : \"" + newComment + "\"}").getBytes();
//    MockHttpRequest request =
//      MockHttpRequest
//        .post("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/space/name/1/comments/1/reply")
//        .content(pullRequestCommentJson)
//        .contentType(MediaType.APPLICATION_JSON);
//
//    dispatcher.invoke(request, response);
//
//    verify(service).reply(eq(repository), eq("1"), any(), any());
//    assertEquals(create("https://scm-manager.org/scm/api/v2/pull-requests/space/name/1/comments/new"), response.getOutputHeaders().getFirst("Location"));
//    assertEquals(HttpServletResponse.SC_CREATED, response.getStatus());
//  }
//
//  @Test
//  @SubjectAware(username = "trillian", password = "secret")
//  public void shouldGetUnauthorizedExceptionWhenMissingPermissionOnCreateComment() throws URISyntaxException, UnsupportedEncodingException {
//    PullRequestComment comment = createComment("1", "1. comment", "slarti", new Location());
//    when(service.get("space", "name", "1", "1")).thenReturn(comment);
//    String newComment = "haha ";
//    byte[] pullRequestCommentJson = ("{\"comment\" : \"" + newComment + "\"}").getBytes();
//    MockHttpRequest request =
//      MockHttpRequest
//        .post("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/space/name/1/comments/1/reply")
//        .content(pullRequestCommentJson)
//        .contentType(MediaType.APPLICATION_JSON);
//
//    dispatcher.invoke(request, response);
//
//    verify(service, never()).reply(any(), any(), any(), any());
//    assertEquals(HttpServletResponse.SC_FORBIDDEN, response.getStatus());
//  }
}
