/**
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
package com.cloudogu.scm.review.comment.api;

import com.cloudogu.scm.review.RepositoryResolver;
import com.cloudogu.scm.review.comment.service.Comment;
import com.cloudogu.scm.review.comment.service.CommentService;
import com.cloudogu.scm.review.comment.service.Location;
import com.cloudogu.scm.review.comment.service.Reply;
import com.cloudogu.scm.review.events.ChannelRegistry;
import com.cloudogu.scm.review.pullrequest.api.PullRequestResource;
import com.cloudogu.scm.review.pullrequest.api.PullRequestRootResource;
import com.cloudogu.scm.review.pullrequest.dto.BranchRevisionResolver;
import com.cloudogu.scm.review.pullrequest.dto.PullRequestMapperImpl;
import com.cloudogu.scm.review.pullrequest.service.PullRequestService;
import com.google.inject.util.Providers;
import org.assertj.core.api.Assertions;
import org.jboss.resteasy.mock.MockHttpRequest;
import org.jboss.resteasy.mock.MockHttpResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import sonia.scm.repository.Repository;
import sonia.scm.web.RestDispatcher;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;

import static com.cloudogu.scm.review.comment.service.Comment.createComment;
import static java.net.URI.create;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.Silent.class)
public class CommentResourceTest {

  private static final Comment EXISTING_ROOT_COMMENT = createComment("1", "1. comment", "slarti", new Location());
  private final Repository repository = mock(Repository.class);
  private final UriInfo uriInfo = mock(UriInfo.class);

  private RestDispatcher dispatcher;

  private final MockHttpResponse response = new MockHttpResponse();

  @Mock
  private RepositoryResolver repositoryResolver;

  @Mock
  private CommentService service;
  @Mock
  private PullRequestService pullRequestService;
  @Mock
  private BranchRevisionResolver branchRevisionResolver;
  @Mock
  private ChannelRegistry channelRegistry;

  private CommentPathBuilder commentPathBuilder = CommentPathBuilderMock.createMock("https://scm-manager.org/scm/api/v2");

  @Before
  public void init() {
    when(branchRevisionResolver.getRevisions("space", "name", "1")).thenReturn(new BranchRevisionResolver.RevisionResult("source", "target"));
    when(repositoryResolver.resolve(any())).thenReturn(repository);
    CommentResource resource = new CommentResource(service, repositoryResolver, new CommentMapperImpl(), new ReplyMapperImpl(), commentPathBuilder, new ExecutedTransitionMapperImpl(), branchRevisionResolver);
    when(uriInfo.getAbsolutePathBuilder()).thenReturn(UriBuilder.fromPath("/scm"));
    dispatcher = new RestDispatcher();
    PullRequestRootResource pullRequestRootResource = new PullRequestRootResource(
      new PullRequestMapperImpl(),
      null,
      Providers.of(
        new PullRequestResource(
          new PullRequestMapperImpl(),
          null,
          Providers.of(
            new CommentRootResource(
              new CommentMapperImpl(),
              repositoryResolver,
              service,
              Providers.of(resource),
              commentPathBuilder,
              pullRequestService,
              branchRevisionResolver
            )
          ),
          channelRegistry
        )
      )
    );
    dispatcher.addSingletonResource(pullRequestRootResource);

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
        .delete("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/space/name/1/comments/1/replies/x?sourceRevision=source&targetRevision=target")
        .contentType(MediaType.APPLICATION_JSON);

    dispatcher.invoke(request, response);

    verify(service).delete("space", "name", "1", "x");
    assertEquals(HttpServletResponse.SC_NO_CONTENT, response.getStatus());
  }

  @Test
  public void shouldNotDeleteReplyIfPullRequestHasChanged() throws URISyntaxException, UnsupportedEncodingException {
    MockHttpRequest request =
      MockHttpRequest
        .delete("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/space/name/1/comments/1/replies/x?sourceRevision=wrong")
        .contentType(MediaType.APPLICATION_JSON);

    dispatcher.invoke(request, response);

    verify(service, never()).delete(any(), any(), any(), any());
    Assertions.assertThat(response.getContentAsString()).contains("modified concurrently");
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
  public void shouldNotUpdateCommentIfPullRequestHasChanged() throws URISyntaxException, UnsupportedEncodingException {
    String newComment = "haha";
    byte[] pullRequestCommentJson = ("{\"comment\" : \"" + newComment + "\"}").getBytes();
    MockHttpRequest request =
      MockHttpRequest
        .put("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/space/name/1/comments/1?sourceRevision=wrong")
        .content(pullRequestCommentJson)
        .contentType(MediaType.APPLICATION_JSON);

    dispatcher.invoke(request, response);

    verify(service, never()).modifyComment(any(), any(), any(), any(), any());
    Assertions.assertThat(response.getContentAsString()).contains("modified concurrently");
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
  public void shouldNotUpdateReplyIfPullRequestHasChanged() throws URISyntaxException, UnsupportedEncodingException {
    String newComment = "haha";
    byte[] pullRequestCommentJson = ("{\"comment\" : \"" + newComment + "\"}").getBytes();
    MockHttpRequest request =
      MockHttpRequest
        .put("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/space/name/1/comments/1/replies/x?sourceRevision=wrong")
        .content(pullRequestCommentJson)
        .contentType(MediaType.APPLICATION_JSON);

    dispatcher.invoke(request, response);

    verify(service, never()).modifyReply(any(), any(), any(), any(), any());
    Assertions.assertThat(response.getContentAsString()).contains("modified concurrently");
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
    assertEquals(create("https://scm-manager.org/scm/api/v2/pull-requests/space/name/1/comments/1/replies/new"), response.getOutputHeaders().getFirst("Location"));
    assertEquals(HttpServletResponse.SC_CREATED, response.getStatus());
  }

  @Test
  public void shouldNotReplyIfPullRequestHasChanged() throws URISyntaxException, UnsupportedEncodingException {
    String newComment = "haha ";
    when(service.reply(eq("space"), eq("name"), eq("1"), eq("1"), argThat(t -> t.getComment().equals(newComment))))
      .thenReturn("new");
    byte[] pullRequestCommentJson = ("{\"comment\" : \"" + newComment + "\"}").getBytes();
    MockHttpRequest request =
      MockHttpRequest
        .post("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/space/name/1/comments/1/replies?sourceRevision=wrong")
        .content(pullRequestCommentJson)
        .contentType(MediaType.APPLICATION_JSON);

    dispatcher.invoke(request, response);

    verify(service, never()).reply(any(), any(), any(), any(), any());
    Assertions.assertThat(response.getContentAsString()).contains("modified concurrently");
  }
}
