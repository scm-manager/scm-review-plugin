package com.cloudogu.scm.review.comment.api;

import com.cloudogu.scm.review.ExceptionMessageMapper;
import com.cloudogu.scm.review.RepositoryResolver;
import com.cloudogu.scm.review.comment.service.Comment;
import com.cloudogu.scm.review.comment.service.CommentService;
import com.cloudogu.scm.review.comment.service.CommentTransition;
import com.cloudogu.scm.review.comment.service.Location;
import com.cloudogu.scm.review.comment.service.Reply;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import sonia.scm.repository.NamespaceAndName;
import sonia.scm.repository.Repository;
import sonia.scm.user.UserDisplayManager;

import javax.inject.Provider;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;

import static com.cloudogu.scm.review.comment.service.Comment.createComment;
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

  @Mock
  private UserDisplayManager userDisplayManager;

  private CommentPathBuilder commentPathBuilder = CommentPathBuilderMock.createMock();

  @InjectMocks
  private ReplyMapperImpl replyMapper;
  @InjectMocks
  private ExecutedTransitionMapperImpl executedTransitionMapper;
  @InjectMocks
  private CommentMapperImpl commentMapper;

  @Before
  public void init() {
    when(repository.getId()).thenReturn(REPOSITORY_ID);
    when(repository.getName()).thenReturn(REPOSITORY_NAME);
    when(repository.getNamespace()).thenReturn(REPOSITORY_NAMESPACE);
    when(repository.getNamespaceAndName()).thenReturn(new NamespaceAndName(REPOSITORY_NAMESPACE, REPOSITORY_NAME));
    when(repositoryResolver.resolve(any())).thenReturn(repository);
    commentMapper.setReplyMapper(replyMapper);
    commentMapper.setExecutedTransitionMapper(executedTransitionMapper);
    replyMapper.setExecutedTransitionMapper(executedTransitionMapper);
    CommentRootResource resource = new CommentRootResource(commentMapper, repositoryResolver, service, commentResourceProvider, commentPathBuilder);
    when(uriInfo.getAbsolutePathBuilder()).thenReturn(UriBuilder.fromPath("/scm"));
    dispatcher = MockDispatcherFactory.createDispatcher();
    dispatcher.getProviderFactory().register(new ExceptionMessageMapper());
    PullRequestRootResource pullRequestRootResource = new PullRequestRootResource(new PullRequestMapperImpl(), null,
      Providers.of(new PullRequestResource(new PullRequestMapperImpl(), null, Providers.of(resource), commentService)));
    dispatcher.getRegistry().addSingletonResource(pullRequestRootResource);
  }

  @Test
  @SubjectAware(username = "slarti", password = "secret")
  public void shouldCreateNewComment() throws URISyntaxException {
    when(service.add(eq(REPOSITORY_NAMESPACE), eq(REPOSITORY_NAME), eq("1"), argThat(t -> t.getComment().equals("this is my comment")))).thenReturn("1");
    byte[] commentJson = "{\"comment\" : \"this is my comment\"}".getBytes();
    MockHttpRequest request =
      MockHttpRequest
        .post("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/space/name/1/comments")
        .content(commentJson)
        .contentType(MediaType.APPLICATION_JSON);

    dispatcher.invoke(request, response);

    assertEquals(HttpServletResponse.SC_CREATED, response.getStatus());
    assertThat(response.getOutputHeaders().getFirst("Location").toString()).isEqualTo("/v2/pull-requests/space/name/1/comments/1");
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

    assertThat(comment_1.get("_links").get("reply").get("href").asText()).isEqualTo("/v2/pull-requests/space/name/1/comments/1/reply");
    assertThat(comment_2.get("_links").get("reply")).isNull(); // respond only to latest reply

    JsonNode replies = comment_2.get("_embedded").get("replies");
    JsonNode reply_1 = replies.path(0);
    JsonNode reply_2 = replies.path(1);

    assertThat(reply_1.get("_links").get("self").get("href").asText()).isEqualTo("/v2/pull-requests/space/name/1/comments/2/replies/2_1");
    assertThat(reply_2.get("_links").get("self").get("href").asText()).isEqualTo("/v2/pull-requests/space/name/1/comments/2/replies/2_2");

    assertThat(reply_1.get("_links").get("update").get("href").asText()).isEqualTo("/v2/pull-requests/space/name/1/comments/2/replies/2_1");
    assertThat(reply_2.get("_links").get("update").get("href").asText()).isEqualTo("/v2/pull-requests/space/name/1/comments/2/replies/2_2");

    assertThat(reply_1.get("_links").get("delete").get("href").asText()).isEqualTo("/v2/pull-requests/space/name/1/comments/2/replies/2_1");
    assertThat(reply_2.get("_links").get("delete").get("href").asText()).isEqualTo("/v2/pull-requests/space/name/1/comments/2/replies/2_2");

    assertThat(reply_1.get("_links").get("reply")).isNull(); // respond only to latest reply
    assertThat(reply_2.get("_links").get("reply").get("href").asText()).isEqualTo("/v2/pull-requests/space/name/1/comments/2/reply");
  }

  @Test
  @SubjectAware(username = "createCommentUser", password = "secret")
  public void shouldGetOnlyTheSelfLinkÍfTheAuthorIsNotTheCurrentUser() throws URISyntaxException, IOException {
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
    assertThat(reply_2.get("_links").get("reply").get("href").asText()).isEqualTo("/v2/pull-requests/space/name/1/comments/2/reply");
  }

  @Test
  @SubjectAware(username = "slarti", password = "secret")
  public void shouldEmbedTransitions() throws URISyntaxException, IOException {
    mockExistingComments();
    Comment commentWithTransitions = service.get("space", "name", "1", "1");
    commentWithTransitions.addTransition(CommentTransition.MAKE_TASK, "slarti");
    commentWithTransitions.addTransition(CommentTransition.SET_DONE, "dent");

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

  private void mockExistingComments() {
    Comment comment1 = createComment("1", "1. comment", "author", new Location("", "", ""));
    Comment comment2 = createComment("2", "2. comment", "author", new Location("", "", ""));

    Reply reply1 = createReply("2_1", "1. reply", "author");
    Reply reply2 = createReply("2_2", "2. reply", "author");

    comment2.setReplies(asList(reply1, reply2));

    ArrayList<Comment> list = Lists.newArrayList(comment1, comment2);
    when(service.getAll("space", "name", "1")).thenReturn(list);
    when(service.get("space", "name", "1", "1")).thenReturn(comment1);
    when(service.get("space", "name", "1", "2")).thenReturn(comment2);
  }
}
