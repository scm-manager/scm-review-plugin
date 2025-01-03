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
import com.cloudogu.scm.review.TestData;
import com.cloudogu.scm.review.comment.service.Comment;
import com.cloudogu.scm.review.comment.service.CommentService;
import com.cloudogu.scm.review.comment.service.CommentTransition;
import com.cloudogu.scm.review.comment.service.ExecutedTransition;
import com.cloudogu.scm.review.comment.service.InlineContext;
import com.cloudogu.scm.review.comment.service.Location;
import com.cloudogu.scm.review.comment.service.MockedDiffLine;
import com.cloudogu.scm.review.comment.service.PullRequestImageService;
import com.cloudogu.scm.review.comment.service.Reply;
import com.cloudogu.scm.review.config.service.ConfigService;
import com.cloudogu.scm.review.pullrequest.api.PullRequestResource;
import com.cloudogu.scm.review.pullrequest.api.PullRequestRootResource;
import com.cloudogu.scm.review.pullrequest.dto.BranchRevisionResolver;
import com.cloudogu.scm.review.pullrequest.dto.PullRequestMapperImpl;
import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import com.cloudogu.scm.review.pullrequest.service.PullRequestChangeService;
import com.cloudogu.scm.review.pullrequest.service.PullRequestService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.sdorra.shiro.ShiroRule;
import com.github.sdorra.shiro.SubjectAware;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.inject.util.Providers;
import org.jboss.resteasy.mock.MockHttpRequest;
import org.jboss.resteasy.mock.MockHttpResponse;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import sonia.scm.repository.Repository;
import sonia.scm.repository.api.RepositoryServiceFactory;
import sonia.scm.sse.ChannelRegistry;
import sonia.scm.store.Blob;
import sonia.scm.user.UserDisplayManager;
import sonia.scm.web.RestDispatcher;

import jakarta.inject.Provider;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;

import static com.cloudogu.scm.review.comment.service.Comment.createComment;
import static com.cloudogu.scm.review.comment.service.ContextLine.copy;
import static com.cloudogu.scm.review.comment.service.Reply.createReply;
import static java.util.Arrays.asList;
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

  public static final PullRequest PULL_REQUEST = TestData.createPullRequest();
  @Rule
  public final ShiroRule shiroRule = new ShiroRule();

  private final Repository repository = new Repository(REPOSITORY_ID, "git", REPOSITORY_NAMESPACE, REPOSITORY_NAME);
  private final UriInfo uriInfo = mock(UriInfo.class);

  private RestDispatcher dispatcher;

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
  @Mock
  private PullRequestService pullRequestService;
  @Mock
  private RepositoryServiceFactory serviceFactory;
  @Mock
  private BranchRevisionResolver branchRevisionResolver;
  @Mock
  private MentionMapper mentionMapper;

  @Mock
  private UserDisplayManager userDisplayManager;

  @Mock
  private ChannelRegistry channelRegistry;
  @Mock
  private ConfigService configService;

  @Mock
  private PullRequestChangeService pullRequestChangeService;

  @Mock
  private PullRequestImageService pullRequestImageService;

  private CommentPathBuilder commentPathBuilder = CommentPathBuilderMock.createMock();

  @InjectMocks
  private ReplyMapperImpl replyMapper;
  @InjectMocks
  private ExecutedTransitionMapperImpl executedTransitionMapper;
  @InjectMocks
  private PossibleTransitionMapper possibleTransitionMapper;
  @InjectMocks
  private CommentMapperImpl commentMapper;

  @Before
  public void init() {
    when(repositoryResolver.resolve(any())).thenReturn(repository);
    commentMapper.setReplyMapper(replyMapper);
    commentMapper.setExecutedTransitionMapper(executedTransitionMapper);
    commentMapper.setPossibleTransitionMapper(possibleTransitionMapper);
    replyMapper.setExecutedTransitionMapper(executedTransitionMapper);
    CommentRootResource resource = new CommentRootResource(commentMapper, repositoryResolver, service, commentResourceProvider, commentPathBuilder, pullRequestService, branchRevisionResolver, pullRequestImageService);
    when(uriInfo.getAbsolutePathBuilder()).thenReturn(UriBuilder.fromPath("/scm"));
    dispatcher = new RestDispatcher();
    PullRequestRootResource pullRequestRootResource = new PullRequestRootResource(new PullRequestMapperImpl(), null,
            commentService, serviceFactory, Providers.of(new PullRequestResource(new PullRequestMapperImpl(), null, Providers.of(resource), null, channelRegistry, pullRequestChangeService)), configService, userDisplayManager);
    dispatcher.addSingletonResource(pullRequestRootResource);
    when(branchRevisionResolver.getRevisions(any(), any(), any())).thenReturn(new BranchRevisionResolver.RevisionResult("source", "target"));
    when(branchRevisionResolver.getRevisions(any(), any())).thenReturn(new BranchRevisionResolver.RevisionResult("source", "target"));
  }

  @Test
  @SubjectAware(username = "slarti", password = "secret")
  public void shouldCreateNewComment() throws URISyntaxException {
    when(pullRequestService.get(any(), any(), any())).thenReturn(PULL_REQUEST);
    when(service.add(eq(REPOSITORY_NAMESPACE), eq(REPOSITORY_NAME), eq("1"), argThat(t -> t.getComment().equals("this is my comment")))).thenReturn("1");
    byte[] commentJson = "{\"comment\" : \"this is my comment\"}".getBytes();
    MockHttpRequest request =
      MockHttpRequest
        .post("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/space/name/1/comments?sourceRevision=source&targetRevision=target")
        .content(commentJson)
        .contentType(MediaType.APPLICATION_JSON);

    dispatcher.invoke(request, response);

    assertEquals(HttpServletResponse.SC_CREATED, response.getStatus());
    assertThat(response.getOutputHeaders().getFirst("Location")).hasToString("/v2/pull-requests/space/name/1/comments/1");
  }

  @Test
  public void shouldCreateCommentWithImage() throws URISyntaxException, IOException {
    when(pullRequestService.get(any(), any(), any())).thenReturn(PULL_REQUEST);
    when(service.add(eq(REPOSITORY_NAMESPACE), eq(REPOSITORY_NAME), eq("1"), argThat(t -> t.getComment().equals("this is my comment")))).thenReturn("1");
    String commentJson = "{\"comment\" : \"this is my comment\", \"filetypes\": {\"file0\": \"image/png\"}}";
    MockHttpRequest request =
      MockHttpRequest
        .post("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/space/name/1/comments/images?sourceRevision=source&targetRevision=target");

    MultipartUploadHelper.multipartRequest(request, Collections.singletonMap("file0", new ByteArrayInputStream("content".getBytes())), commentJson);
    dispatcher.invoke(request, response);

    assertEquals(HttpServletResponse.SC_CREATED, response.getStatus());
    assertThat(response.getOutputHeaders().getFirst("Location")).hasToString("/v2/pull-requests/space/name/1/comments/1");
  }

  @Test
  public void shouldGetCommentImage() throws URISyntaxException, IOException {
    Blob blob = mock(Blob.class);
    when(blob.getInputStream()).thenReturn(new ByteArrayInputStream("image/png\0content".getBytes()));
    PullRequestImageService.ImageStream imageStream = new PullRequestImageService.ImageStream(blob);
    when(pullRequestImageService.getPullRequestImage(any(), any(), any())).thenReturn(imageStream);

    MockHttpRequest request =
      MockHttpRequest
        .get("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/space/name/1/comments/images/dummyHash");

    dispatcher.invoke(request, response);

    assertEquals(HttpServletResponse.SC_OK, response.getStatus());
    assertThat(response.getOutput()).isEqualTo("content".getBytes());
  }

  @Test
  @SubjectAware(username = "slarti", password = "secret")
  public void shouldThrowConflictExceptionIfSourceRevisionNotEqualPullRequestSourceRevision() throws URISyntaxException, UnsupportedEncodingException {
    when(pullRequestService.get(any(), any(), any())).thenReturn(PULL_REQUEST);
    when(branchRevisionResolver.getRevisions(any(), eq(PULL_REQUEST))).thenReturn(new BranchRevisionResolver.RevisionResult("source", "target"));

    byte[] commentJson = "{\"comment\" : \"this is my comment\"}".getBytes();
    MockHttpRequest request =
      MockHttpRequest
        .post("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/space/name/1/comments?sourceRevision=other&targetRevision=target")
        .content(commentJson)
        .contentType(MediaType.APPLICATION_JSON);

    dispatcher.invoke(request, response);
    assertEquals(HttpServletResponse.SC_CONFLICT, response.getStatus());
    assertThat(response.getContentAsString()).contains("modified concurrently");
  }

  @Test
  @SubjectAware(username = "slarti", password = "secret")
  public void shouldThrowConflictExceptionIfTargetRevisionNotEqualPullRequestTargetRevision() throws URISyntaxException, UnsupportedEncodingException {
    when(pullRequestService.get(any(), any(), any())).thenReturn(PULL_REQUEST);
    when(branchRevisionResolver.getRevisions(any(), eq(PULL_REQUEST))).thenReturn(new BranchRevisionResolver.RevisionResult("source", "target"));

    byte[] commentJson = "{\"comment\" : \"this is my comment\"}".getBytes();
    MockHttpRequest request =
      MockHttpRequest
        .post("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/space/name/1/comments?sourceRevision=source&targetRevision=other")
        .content(commentJson)
        .contentType(MediaType.APPLICATION_JSON);

    dispatcher.invoke(request, response);
    assertEquals(HttpServletResponse.SC_CONFLICT, response.getStatus());
    assertThat(response.getContentAsString()).contains("modified concurrently");
  }

  @Test
  @SubjectAware(username = "slarti", password = "secret")
  public void shouldNotThrowConflictExceptionIfNoExpectationsInUrl() throws URISyntaxException, UnsupportedEncodingException {
    when(pullRequestService.get(any(), any(), any())).thenReturn(PULL_REQUEST);
    when(branchRevisionResolver.getRevisions(any(), eq(PULL_REQUEST))).thenReturn(new BranchRevisionResolver.RevisionResult("source", "target"));

    byte[] commentJson = "{\"comment\" : \"this is my comment\"}".getBytes();
    MockHttpRequest request =
      MockHttpRequest
        .post("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/space/name/1/comments")
        .content(commentJson)
        .contentType(MediaType.APPLICATION_JSON);

    dispatcher.invoke(request, response);
    assertEquals(HttpServletResponse.SC_CREATED, response.getStatus());
  }

  @Test
  @SubjectAware(username = "slarti", password = "secret")
  public void shouldNotThrowConflictExceptionIfOnlyOneExpectationInUrl() throws URISyntaxException, UnsupportedEncodingException {
    when(pullRequestService.get(any(), any(), any())).thenReturn(PULL_REQUEST);
    when(branchRevisionResolver.getRevisions(any(), eq(PULL_REQUEST))).thenReturn(new BranchRevisionResolver.RevisionResult("source", "target"));

    byte[] commentJson = "{\"comment\" : \"this is my comment\"}".getBytes();
    MockHttpRequest request =
      MockHttpRequest
        .post("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/space/name/1/comments?sourceRevision=source")
        .content(commentJson)
        .contentType(MediaType.APPLICATION_JSON);

    dispatcher.invoke(request, response);
    assertEquals(HttpServletResponse.SC_CREATED, response.getStatus());
  }

  @Test
  @SubjectAware(username = "slarti", password = "secret")
  public void shouldThrowConflictExceptionIfSingleExpectationInUrlIsWrong() throws URISyntaxException, UnsupportedEncodingException {
    when(pullRequestService.get(any(), any(), any())).thenReturn(PULL_REQUEST);
    when(branchRevisionResolver.getRevisions(any(), eq(PULL_REQUEST))).thenReturn(new BranchRevisionResolver.RevisionResult("source", "target"));

    byte[] commentJson = "{\"comment\" : \"this is my comment\"}".getBytes();
    MockHttpRequest request =
      MockHttpRequest
        .post("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/space/name/1/comments?sourceRevision=other")
        .content(commentJson)
        .contentType(MediaType.APPLICATION_JSON);

    dispatcher.invoke(request, response);
    assertEquals(HttpServletResponse.SC_CONFLICT, response.getStatus());
    assertThat(response.getContentAsString()).contains("modified concurrently");
  }

  @Test
  @SubjectAware(username = "slarti", password = "secret")
  public void shouldGetAllComments() throws URISyntaxException, IOException {
    mockExistingComments();

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
    assertThat(comment_1.get("comment").asText()).isEqualTo("1. comment");
    assertThat(comment_1.get("context").get("lines").size()).isEqualTo(3);
    assertThat(comment_2.get("comment").asText()).isEqualTo("2. comment");

    JsonNode replies = comment_2.get("_embedded").get("replies");
    JsonNode reply_1 = replies.path(0);
    JsonNode reply_2 = replies.path(1);

    assertThat(reply_1.get("comment").asText()).isEqualTo("1. reply");
    assertThat(reply_2.get("comment").asText()).isEqualTo("2. reply");
  }

  @Test
  @SubjectAware(username = "slarti", password = "secret")
  public void shouldGetAllLinks() throws URISyntaxException, IOException {
    mockExistingComments();

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

    assertThat(comment_1.get("_links").get("self").get("href").asText()).isEqualTo("/v2/pull-requests/space/name/1/comments/1");
    assertThat(comment_2.get("_links").get("self").get("href").asText()).isEqualTo("/v2/pull-requests/space/name/1/comments/2");

    assertThat(comment_1.get("_links").get("update").get("href").asText()).isEqualTo("/v2/pull-requests/space/name/1/comments/1");
    assertThat(comment_2.get("_links").get("update").get("href").asText()).isEqualTo("/v2/pull-requests/space/name/1/comments/2");

    assertThat(comment_1.get("_links").get("delete").get("href").asText()).isEqualTo("/v2/pull-requests/space/name/1/comments/1");
    assertThat(comment_2.get("_links").get("delete")).isNull(); // must not delete comment with responses

    // root comments have reply links
    assertThat(comment_1.get("_links").get("reply").get("href").asText()).isEqualTo("/v2/pull-requests/space/name/1/comments/1/reply");
    assertThat(comment_2.get("_links").get("reply").get("href").asText()).isEqualTo("/v2/pull-requests/space/name/1/comments/2/reply");

    JsonNode replies = comment_2.get("_embedded").get("replies");
    JsonNode reply_1 = replies.path(0);
    JsonNode reply_2 = replies.path(1);

    assertThat(reply_1.get("_links").get("self").get("href").asText()).isEqualTo("/v2/pull-requests/space/name/1/comments/2/replies/2_1");
    assertThat(reply_2.get("_links").get("self").get("href").asText()).isEqualTo("/v2/pull-requests/space/name/1/comments/2/replies/2_2");

    assertThat(reply_1.get("_links").get("update").get("href").asText()).isEqualTo("/v2/pull-requests/space/name/1/comments/2/replies/2_1");
    assertThat(reply_2.get("_links").get("update").get("href").asText()).isEqualTo("/v2/pull-requests/space/name/1/comments/2/replies/2_2");

    assertThat(reply_1.get("_links").get("delete").get("href").asText()).isEqualTo("/v2/pull-requests/space/name/1/comments/2/replies/2_1");
    assertThat(reply_2.get("_links").get("delete").get("href").asText()).isEqualTo("/v2/pull-requests/space/name/1/comments/2/replies/2_2");

    // replies no longer have a reply link
    assertThat(reply_1.get("_links").get("reply")).isNull();
    assertThat(reply_2.get("_links").get("reply")).isNull();
  }

  @Test
  @SubjectAware(username = "createCommentUser", password = "secret")
  public void shouldGetOnlyTheSelfLinkIfTheAuthorIsNotTheCurrentUser() throws URISyntaxException, IOException {
    mockExistingComments();

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

    assertThat(comment_1.get("_links").get("self").get("href").asText()).isEqualTo("/v2/pull-requests/space/name/1/comments/1");
    assertThat(comment_2.get("_links").get("self").get("href").asText()).isEqualTo("/v2/pull-requests/space/name/1/comments/2");

    assertThat(comment_1.get("_links").get("update")).isNull();
    assertThat(comment_2.get("_links").get("update")).isNull();

    assertThat(comment_1.get("_links").get("delete")).isNull();
    assertThat(comment_2.get("_links").get("delete")).isNull();

    JsonNode replies = comment_2.get("_embedded").get("replies");
    JsonNode reply_1 = replies.path(0);
    JsonNode reply_2 = replies.path(1);

    assertThat(reply_1.get("_links").get("self").get("href").asText()).isEqualTo("/v2/pull-requests/space/name/1/comments/2/replies/2_1");
    assertThat(reply_2.get("_links").get("self").get("href").asText()).isEqualTo("/v2/pull-requests/space/name/1/comments/2/replies/2_2");

    assertThat(reply_1.get("_links").get("update")).isNull();
    assertThat(reply_2.get("_links").get("update")).isNull();

    assertThat(reply_1.get("_links").get("delete")).isNull();
    assertThat(reply_2.get("_links").get("delete")).isNull();

    assertThat(reply_1.get("_links").get("reply")).isNull();
    assertThat(reply_2.get("_links").get("reply")).isNull();
  }

  @Test
  @SubjectAware(username = "slarti", password = "secret")
  public void shouldEmbedTransitions() throws URISyntaxException, IOException {
    mockExistingComments();
    Comment commentWithTransitions = service.get("space", "name", "1", "1");
    commentWithTransitions.addCommentTransition(new ExecutedTransition<>("1", CommentTransition.MAKE_TASK, System.currentTimeMillis(), "slarti"));
    commentWithTransitions.addCommentTransition(new ExecutedTransition<>("2", CommentTransition.SET_DONE, System.currentTimeMillis(), "dent"));

    MockHttpRequest request =
      MockHttpRequest
        .get("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/space/name/1/comments")
        .contentType(MediaType.APPLICATION_JSON);

    dispatcher.invoke(request, response);

    assertEquals(HttpServletResponse.SC_OK, response.getStatus());
    System.out.println(response.getContentAsString());
    ObjectMapper mapper = new ObjectMapper();
    JsonNode jsonNode = mapper.readValue(response.getContentAsString(), JsonNode.class);
    JsonNode prNode = jsonNode.get("_embedded").get("pullRequestComments").get(0);
    JsonNode embeddedTransitionsNode = prNode.get("_embedded").get("transitions");
    JsonNode transition_1 = embeddedTransitionsNode.path(0);
    JsonNode transition_2 = embeddedTransitionsNode.path(1);
    assertThat(transition_1.get("transition").asText()).isEqualTo("MAKE_TASK");
    assertThat(transition_1.get("user").get("id").asText()).isEqualTo("slarti");
    assertThat(transition_2.get("transition").asText()).isEqualTo("SET_DONE");
    assertThat(transition_2.get("user").get("id").asText()).isEqualTo("dent");
  }

  @Test
  @SubjectAware(username = "slarti", password = "secret")
  public void shouldGetCreateLinkWithRevisions() throws URISyntaxException, IOException {
    mockExistingComments();

    MockHttpRequest request =
      MockHttpRequest
        .get("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/space/name/1/comments")
        .contentType(MediaType.APPLICATION_JSON);

    dispatcher.invoke(request, response);

    assertEquals(HttpServletResponse.SC_OK, response.getStatus());
    ObjectMapper mapper = new ObjectMapper();
    JsonNode jsonNode = mapper.readValue(response.getContentAsString(), JsonNode.class);
    JsonNode createLinkNode = jsonNode.get("_links").get("create").get("href");

    assertThat(createLinkNode.asText()).isEqualTo("/v2/pull-requests/space/name/1/comments/?sourceRevision=source&targetRevision=target");
  }

  private void mockExistingComments() {
    Comment comment1 = createComment("1", "1. comment", "author", new Location("file.txt", "123", 0, 3));
    Comment comment2 = createComment("2", "2. comment", "author", new Location("", "", 0, 0));

    comment1.setContext(new InlineContext(ImmutableList.of(
      copy(new MockedDiffLine.Builder().newLineNumber(1).get()),
      copy(new MockedDiffLine.Builder().newLineNumber(2).get()),
      copy(new MockedDiffLine.Builder().newLineNumber(3).get())
    )));

    Reply reply1 = createReply("2_1", "1. reply", "author");
    Reply reply2 = createReply("2_2", "2. reply", "author");

    comment2.setReplies(asList(reply1, reply2));

    ArrayList<Comment> list = Lists.newArrayList(comment1, comment2);
    when(service.getAll("space", "name", "1")).thenReturn(list);
    when(service.get("space", "name", "1", "1")).thenReturn(comment1);
    when(service.get("space", "name", "1", "2")).thenReturn(comment2);
  }
}
