/*
 * Copyright (c) 2020 - present Cloudogu GmbH
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Affero General Public License as published by the Free
 * Software Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see https://www.gnu.org/licenses/.
 */

package com.cloudogu.scm.review.comment.api;

import com.cloudogu.scm.review.RepositoryResolver;
import com.cloudogu.scm.review.comment.service.Comment;
import com.cloudogu.scm.review.comment.service.CommentService;
import com.cloudogu.scm.review.comment.service.Location;
import com.cloudogu.scm.review.comment.service.PullRequestImageService;
import com.cloudogu.scm.review.comment.service.Reply;
import com.cloudogu.scm.review.config.service.ConfigService;
import com.cloudogu.scm.review.pullrequest.api.PullRequestResource;
import com.cloudogu.scm.review.pullrequest.api.PullRequestRootResource;
import com.cloudogu.scm.review.pullrequest.dto.BranchRevisionResolver;
import com.cloudogu.scm.review.pullrequest.dto.PullRequestMapperImpl;
import com.cloudogu.scm.review.pullrequest.service.PullRequestChangeService;
import com.cloudogu.scm.review.pullrequest.service.PullRequestCreator;
import com.cloudogu.scm.review.pullrequest.service.PullRequestService;
import com.google.inject.util.Providers;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;
import org.assertj.core.api.Assertions;
import org.jboss.resteasy.mock.MockHttpRequest;
import org.jboss.resteasy.mock.MockHttpResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import sonia.scm.repository.Repository;
import sonia.scm.repository.api.RepositoryServiceFactory;
import sonia.scm.sse.ChannelRegistry;
import sonia.scm.store.Blob;
import sonia.scm.user.UserDisplayManager;
import sonia.scm.web.RestDispatcher;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.util.Collections;

import static com.cloudogu.scm.review.comment.service.Comment.createComment;
import static java.net.URI.create;
import static org.assertj.core.api.Assertions.assertThat;
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
  private RepositoryServiceFactory serviceFactory;
  @Mock
  private BranchRevisionResolver branchRevisionResolver;
  @Mock
  private ChannelRegistry channelRegistry;
  @Mock
  private ConfigService configService;
  @Mock
  private CommentService commentService;
  @Mock
  private UserDisplayManager userDisplayManager;

  @Mock
  private PullRequestImageService pullRequestImageService;

  private PullRequestChangeService pullRequestChangeService;

  private CommentPathBuilder commentPathBuilder = CommentPathBuilderMock.createMock("https://scm-manager.org/scm/api/v2");

  @Before
  public void init() {
    when(branchRevisionResolver.getRevisions("space", "name", "1")).thenReturn(new BranchRevisionResolver.RevisionResult("source", "target"));
    when(repositoryResolver.resolve(any())).thenReturn(repository);
    CommentResource resource = new CommentResource(service, repositoryResolver, new CommentMapperImpl(), new ReplyMapperImpl(), commentPathBuilder, new ExecutedTransitionMapperImpl(), pullRequestImageService, branchRevisionResolver);
    when(uriInfo.getAbsolutePathBuilder()).thenReturn(UriBuilder.fromPath("/scm"));
    dispatcher = new RestDispatcher();
    PullRequestRootResource pullRequestRootResource = new PullRequestRootResource(
      new PullRequestMapperImpl(),
      null,
      new PullRequestCreator(pullRequestService, commentService),
      serviceFactory,
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
              branchRevisionResolver,
              pullRequestImageService
            )
          ),
          null,
          channelRegistry,
          pullRequestChangeService
        )
      ),
      configService, userDisplayManager);
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
  public void shouldUpdateCommentWithImage() throws URISyntaxException, IOException {
    String newComment = "haha";
    String pullRequestCommentJson = ("{\"comment\" : \"" + newComment + "\", \"filetypes\": {\"file0\": \"image/png\"}}");
    MockHttpRequest request =
      MockHttpRequest
        .put("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/space/name/1/comments/1/images");

    MultipartUploadHelper.multipartRequest(
      request, Collections.singletonMap("file0", new ByteArrayInputStream("content".getBytes())), pullRequestCommentJson
    );
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
  public void shouldUpdateReplyWithImage() throws URISyntaxException, IOException {
    String newComment = "haha";
    String pullRequestCommentJson = ("{\"comment\" : \"" + newComment + "\", \"filetypes\": {\"file0\": \"image/png\"}}");
    MockHttpRequest request =
      MockHttpRequest
        .put("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/space/name/1/comments/1/replies/x/images");

    MultipartUploadHelper.multipartRequest(
      request, Collections.singletonMap("file0", new ByteArrayInputStream("content".getBytes())), pullRequestCommentJson
    );
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
  public void shouldReplyWithImage() throws URISyntaxException, IOException {
    String newComment = "haha";
    when(service.reply(eq("space"), eq("name"), eq("1"), eq("1"), argThat(t -> t.getComment().equals(newComment))))
      .thenReturn("new");
    String pullRequestCommentJson = ("{\"comment\" : \"" + newComment + "\", \"filetypes\": {\"file0\": \"image/png\"}}");
    MockHttpRequest request =
      MockHttpRequest
        .post("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/space/name/1/comments/1/replies/images");

    MultipartUploadHelper.multipartRequest(
      request, Collections.singletonMap("file0", new ByteArrayInputStream("content".getBytes())), pullRequestCommentJson
    );
    dispatcher.invoke(request, response);

    verify(service).reply(eq("space"), eq("name"), eq("1"), any(), any());
    assertEquals(create("https://scm-manager.org/scm/api/v2/pull-requests/space/name/1/comments/1/replies/new"),
      response.getOutputHeaders().getFirst("Location"));
    assertEquals(HttpServletResponse.SC_CREATED, response.getStatus());
  }

  @Test
  public void shouldGetReplyImage() throws URISyntaxException, IOException {
    Blob blob = mock(Blob.class);
    when(blob.getInputStream()).thenReturn(new ByteArrayInputStream("image/png\0content".getBytes()));
    PullRequestImageService.ImageStream imageStream = new PullRequestImageService.ImageStream(blob);
    when(pullRequestImageService.getPullRequestImage(any(), any(), any())).thenReturn(imageStream);

    MockHttpRequest request =
      MockHttpRequest
        .get("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/space/name/1/comments/1/replies/images/dummyHash");

    dispatcher.invoke(request, response);

    assertEquals(HttpServletResponse.SC_OK, response.getStatus());
    assertThat(response.getOutput()).isEqualTo("content".getBytes());
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
