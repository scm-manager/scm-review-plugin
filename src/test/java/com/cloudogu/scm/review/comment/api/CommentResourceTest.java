package com.cloudogu.scm.review.comment.api;

import com.cloudogu.scm.review.ExceptionMessageMapper;
import com.cloudogu.scm.review.RepositoryResolver;
import com.cloudogu.scm.review.comment.service.CommentService;
import com.cloudogu.scm.review.comment.service.Location;
import com.cloudogu.scm.review.comment.service.Comment;
import com.cloudogu.scm.review.comment.service.Reply;
import com.cloudogu.scm.review.pullrequest.api.PullRequestResource;
import com.cloudogu.scm.review.pullrequest.api.PullRequestRootResource;
import com.cloudogu.scm.review.pullrequest.dto.PullRequestMapperImpl;
import com.google.inject.util.Providers;
import org.jboss.resteasy.core.Dispatcher;
import org.jboss.resteasy.mock.MockDispatcherFactory;
import org.jboss.resteasy.mock.MockHttpRequest;
import org.jboss.resteasy.mock.MockHttpResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import sonia.scm.repository.Repository;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.net.URISyntaxException;

import static com.cloudogu.scm.review.comment.service.Comment.createComment;
import static java.net.URI.create;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.Silent.class)
public class CommentResourceTest {

  public static final Comment EXISTING_ROOT_COMMENT = createComment("1", "1. comment", "slarti", new Location());
  private final Repository repository = mock(Repository.class);
  private final UriInfo uriInfo = mock(UriInfo.class);

  private Dispatcher dispatcher;

  private final MockHttpResponse response = new MockHttpResponse();

  @Mock
  private RepositoryResolver repositoryResolver;

  @Mock
  private CommentService service;
  @Mock
  private CommentService commentService;
  private CommentPathBuilder commentPathBuilder = CommentPathBuilderMock.createMock("https://scm-manager.org/scm/api/v2");

  @Before
  public void init() {
    when(repositoryResolver.resolve(any())).thenReturn(repository);
    CommentResource resource = new CommentResource(service, repositoryResolver, new PullRequestCommentMapperImpl(), new ReplyMapperImpl(), commentPathBuilder);
    when(uriInfo.getAbsolutePathBuilder()).thenReturn(UriBuilder.fromPath("/scm"));
    dispatcher = MockDispatcherFactory.createDispatcher();
    dispatcher.getProviderFactory().register(new ExceptionMessageMapper());
    PullRequestRootResource pullRequestRootResource = new PullRequestRootResource(new PullRequestMapperImpl(), null,
      Providers.of(new PullRequestResource(new PullRequestMapperImpl(), null,
        Providers.of(new CommentRootResource(new PullRequestCommentMapperImpl(), repositoryResolver, service, Providers.of(resource), commentPathBuilder)), commentService)));
    dispatcher.getRegistry().addSingletonResource(pullRequestRootResource);

    when(service.get("space", "name", "1", "1")).thenReturn(EXISTING_ROOT_COMMENT);
  }

  @Test
  public void shouldDeleteComment() throws URISyntaxException {
    MockHttpRequest request =
      MockHttpRequest
        .delete("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/space/name/1/comments/1")
        .contentType(MediaType.APPLICATION_JSON);

    dispatcher.invoke(request, response);

    verify(service).delete("space", "name", "1", "1");
    assertEquals(HttpServletResponse.SC_NO_CONTENT, response.getStatus());
  }

  @Test
  public void shouldDeleteReply() throws URISyntaxException {
    MockHttpRequest request =
      MockHttpRequest
        .delete("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/space/name/1/comments/1/replies/x")
        .contentType(MediaType.APPLICATION_JSON);

    dispatcher.invoke(request, response);

    verify(service).delete("space", "name", "1", "x");
    assertEquals(HttpServletResponse.SC_NO_CONTENT, response.getStatus());
  }

  @Test
  public void shouldUpdateComment() throws URISyntaxException {
    String newComment = "haha";
    byte[] pullRequestCommentJson = ("{\"comment\" : \"" + newComment + "\"}").getBytes();
    MockHttpRequest request =
      MockHttpRequest
        .put("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/space/name/1/comments/1")
        .content(pullRequestCommentJson)
        .contentType(MediaType.APPLICATION_JSON);

    dispatcher.invoke(request, response);

    assertEquals(HttpServletResponse.SC_NO_CONTENT, response.getStatus());
    verify(service).modifyComment(eq("space"), eq("name"), eq("1"), eq("1"),
      argThat((Comment t) -> t.getComment().equals(newComment)));
  }

  @Test
  public void shouldUpdateReply() throws URISyntaxException {
    String newComment = "haha";
    byte[] pullRequestCommentJson = ("{\"comment\" : \"" + newComment + "\"}").getBytes();
    MockHttpRequest request =
      MockHttpRequest
        .put("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/space/name/1/comments/1/replies/x")
        .content(pullRequestCommentJson)
        .contentType(MediaType.APPLICATION_JSON);

    dispatcher.invoke(request, response);

    assertEquals(HttpServletResponse.SC_NO_CONTENT, response.getStatus());
    verify(service).modifyReply(eq("space"), eq("name"), eq("1"), eq("x"),
      argThat((Reply r) -> r.getComment().equals(newComment)));
  }

  @Test
  public void shouldReply() throws URISyntaxException {
    String newComment = "haha ";
    when(service.reply(eq("space"), eq("name"), eq("1"), eq("1"), argThat(t -> t.getComment().equals(newComment))))
      .thenReturn("new");
    byte[] pullRequestCommentJson = ("{\"comment\" : \"" + newComment + "\"}").getBytes();
    MockHttpRequest request =
      MockHttpRequest
        .post("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/space/name/1/comments/1/replies")
        .content(pullRequestCommentJson)
        .contentType(MediaType.APPLICATION_JSON);

    dispatcher.invoke(request, response);

    verify(service).reply(eq("space"), eq("name"), eq("1"), any(), any());
    assertEquals(create("https://scm-manager.org/scm/api/v2/pull-requests/space/name/1/comments/new"), response.getOutputHeaders().getFirst("Location"));
    assertEquals(HttpServletResponse.SC_CREATED, response.getStatus());
  }
}
