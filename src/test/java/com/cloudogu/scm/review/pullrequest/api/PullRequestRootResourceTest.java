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

package com.cloudogu.scm.review.pullrequest.api;

import com.cloudogu.scm.review.BranchResolver;
import com.cloudogu.scm.review.PullRequestMediaType;
import com.cloudogu.scm.review.comment.service.CommentService;
import com.cloudogu.scm.review.comment.service.CommentType;
import com.cloudogu.scm.review.config.service.BasePullRequestConfig;
import com.cloudogu.scm.review.config.service.ConfigService;
import com.cloudogu.scm.review.config.service.RepositoryPullRequestConfig;
import com.cloudogu.scm.review.pullrequest.dto.PullRequestCheckResultDto;
import com.cloudogu.scm.review.pullrequest.dto.PullRequestMapperImpl;
import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import com.cloudogu.scm.review.pullrequest.service.PullRequestChange;
import com.cloudogu.scm.review.pullrequest.service.PullRequestChangeService;
import com.cloudogu.scm.review.pullrequest.service.PullRequestService;
import com.cloudogu.scm.review.pullrequest.service.PullRequestStatus;
import com.cloudogu.scm.review.pullrequest.service.ReviewMark;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.sdorra.shiro.ShiroRule;
import com.github.sdorra.shiro.SubjectAware;
import com.google.common.io.Resources;
import com.google.inject.util.Providers;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.assertj.core.util.Lists;
import org.jboss.resteasy.mock.MockHttpRequest;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import sonia.scm.NotFoundException;
import sonia.scm.api.v2.resources.BranchLinkProvider;
import sonia.scm.repository.Changeset;
import sonia.scm.repository.ChangesetPagingResult;
import sonia.scm.repository.NamespaceAndName;
import sonia.scm.repository.Person;
import sonia.scm.repository.Repository;
import sonia.scm.repository.api.LogCommandBuilder;
import sonia.scm.repository.api.RepositoryService;
import sonia.scm.repository.api.RepositoryServiceFactory;
import sonia.scm.sse.ChannelRegistry;
import sonia.scm.user.DisplayUser;
import sonia.scm.user.User;
import sonia.scm.user.UserDisplayManager;
import sonia.scm.web.JsonMockHttpRequest;
import sonia.scm.web.JsonMockHttpResponse;
import sonia.scm.web.RestDispatcher;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.cloudogu.scm.review.TestData.createPullRequest;
import static com.cloudogu.scm.review.pullrequest.service.PullRequestRejectedEvent.RejectionCause.REJECTED_BY_USER;
import static com.cloudogu.scm.review.pullrequest.service.PullRequestStatus.MERGED;
import static com.cloudogu.scm.review.pullrequest.service.PullRequestStatus.OPEN;
import static com.cloudogu.scm.review.pullrequest.service.PullRequestStatus.REJECTED;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SubjectAware(
  configuration = "classpath:com/cloudogu/scm/review/shiro.ini",
  password = "secret"
)
@RunWith(MockitoJUnitRunner.class)
public class PullRequestRootResourceTest {

  @Rule
  public ShiroRule shiroRule = new ShiroRule();

  private RestDispatcher dispatcher;

  private final JsonMockHttpResponse response = new JsonMockHttpResponse();
  private final Subject subject = mock(Subject.class);

  private static final String REPOSITORY_ID = "repo_ID";
  private static final String REPOSITORY_NAMESPACE = "ns";
  private static final String REPOSITORY_NAME = "repo";
  private static final Repository repository = new Repository(REPOSITORY_ID, "git", REPOSITORY_NAMESPACE, REPOSITORY_NAME);

  @Mock
  private UserDisplayManager userDisplayManager;
  @Mock
  private PullRequestService pullRequestService;
  @Mock
  private RepositoryServiceFactory repositoryServiceFactory;
  @Mock
  private RepositoryService repositoryService;
  @Mock(answer = Answers.RETURNS_SELF)
  private LogCommandBuilder logCommandBuilder;
  @Mock
  private CommentService commentService;
  @Mock
  private ChannelRegistry channelRegistry;
  @Mock
  private ConfigService configService;
  @Mock
  private PullRequestChangeService pullRequestChangeService;
  @Mock
  private BranchResolver branchResolver;
  @Mock
  private BranchLinkProvider branchLinkProvider;
  @InjectMocks
  private PullRequestMapperImpl mapper;

  private int nextPullRequestId = 1;

  @Before
  public void init() {
    PullRequestRootResource pullRequestRootResource = new PullRequestRootResource(
      mapper,
      pullRequestService,
      commentService,
      repositoryServiceFactory,
      Providers.of(new PullRequestResource(
        mapper,
        pullRequestService,
        null,
        null,
        channelRegistry,
        pullRequestChangeService
      )),
      configService,
      userDisplayManager
    );
    when(branchLinkProvider.get(any(NamespaceAndName.class), anyString())).thenReturn("");
    dispatcher = new RestDispatcher();
    dispatcher.addSingletonResource(pullRequestRootResource);
    lenient().when(repositoryServiceFactory.create(any(Repository.class))).thenReturn(repositoryService);
    lenient().when(userDisplayManager.get("reviewer")).thenReturn(Optional.of(DisplayUser.from(new User("reviewer", "reviewer", ""))));
    lenient().when(configService.evaluateConfig(repository)).thenReturn(new BasePullRequestConfig());
    when(pullRequestService.getRepository(REPOSITORY_NAMESPACE, REPOSITORY_NAME))
      .thenReturn(repository);
    doAnswer(invocationOnMock -> Integer.toString(nextPullRequestId++))
      .when(pullRequestService).add(any(), any());
  }

  @After
  public void unbindSubject() {
    ThreadContext.unbindSubject();
  }

  @Test
  @SubjectAware(username = "slarti")
  public void shouldCreateNewValidPullRequest() throws URISyntaxException, IOException {
    mockPrincipal();

    byte[] pullRequestJson = loadJson("com/cloudogu/scm/review/pullRequest.json");
    MockHttpRequest request =
      MockHttpRequest
        .post("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/ns/repo")
        .content(pullRequestJson)
        .contentType(PullRequestMediaType.PULL_REQUEST);

    dispatcher.invoke(request, response);

    assertEquals(HttpServletResponse.SC_CREATED, response.getStatus());
    assertThat(response.getOutputHeaders().getFirst("Location")).hasToString("/v2/pull-requests/ns/repo/1");
    verify(pullRequestService).add(
      argThat(repository ->
        repository.getNamespace().equals(REPOSITORY_NAMESPACE)
          && repository.getName().equals(REPOSITORY_NAME)),
      argThat(pullRequest ->
        pullRequest.getSource().equals("sourceBranch")
          && pullRequest.getTarget().equals("targetBranch")
          && pullRequest.getAuthor().equals("user1")
          && pullRequest.getStatus().equals(OPEN))
    );
  }

  @Test
  @SubjectAware(username = "slarti")
  public void shouldCreateNewPullRequestWithDefaultStatusOpen() throws URISyntaxException, IOException {
    mockPrincipal();

    byte[] pullRequestJson = loadJson("com/cloudogu/scm/review/pullRequest_without_status.json");
    MockHttpRequest request =
      MockHttpRequest
        .post("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/" + REPOSITORY_NAMESPACE + "/" + REPOSITORY_NAME)
        .content(pullRequestJson)
        .contentType(PullRequestMediaType.PULL_REQUEST);

    dispatcher.invoke(request, response);

    assertEquals(HttpServletResponse.SC_CREATED, response.getStatus());
    assertThat(response.getOutputHeaders().getFirst("Location")).hasToString("/v2/pull-requests/" + REPOSITORY_NAMESPACE + "/" + REPOSITORY_NAME + "/1");
    verify(pullRequestService).add(
      argThat(repository ->
        repository.getNamespace().equals(REPOSITORY_NAMESPACE)
          && repository.getName().equals(REPOSITORY_NAME)),
      argThat(pullRequest ->
        pullRequest.getSource().equals("sourceBranch")
          && pullRequest.getTarget().equals("targetBranch")
          && pullRequest.getAuthor().equals("user1")
          && pullRequest.getStatus().equals(OPEN))
    );
  }

  @Test
  @SubjectAware(username = "trillian")
  public void shouldGetUnauthorizedExceptionWhenMissingPermissionOnGetPR() throws URISyntaxException {
    MockHttpRequest request = MockHttpRequest.get("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/" + REPOSITORY_NAMESPACE + "/" + REPOSITORY_NAME + "/123");
    dispatcher.invoke(request, response);

    assertEquals(HttpServletResponse.SC_FORBIDDEN, response.getStatus());
  }

  @Test
  @SubjectAware(username = "trillian")
  public void shouldGetUnauthorizedExceptionWhenMissingPermissionOnGetAllPR() throws URISyntaxException {
    MockHttpRequest request = MockHttpRequest.get("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/" + REPOSITORY_NAMESPACE + "/" + REPOSITORY_NAME);
    dispatcher.invoke(request, response);

    assertEquals(HttpServletResponse.SC_FORBIDDEN, response.getStatus());
  }

  @Test
  @SubjectAware(username = "trillian")
  public void shouldGetUnauthorizedExceptionWhenMissingPermissionOnCreatePR() throws URISyntaxException, IOException {
    byte[] pullRequestJson = loadJson("com/cloudogu/scm/review/pullRequest.json");
    MockHttpRequest request = MockHttpRequest.post("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/" + REPOSITORY_NAMESPACE + "/" + REPOSITORY_NAME)
      .content(pullRequestJson)
      .contentType(PullRequestMediaType.PULL_REQUEST);
    dispatcher.invoke(request, response);

    assertEquals(HttpServletResponse.SC_FORBIDDEN, response.getStatus());
  }

  @Test
  @SubjectAware(username = "slarti")
  public void shouldGetAlreadyExistsExceptionOnCreatePR() throws URISyntaxException, IOException {
    PullRequest pr = new PullRequest("id", "source", "target");
    pr.setStatus(PullRequestStatus.OPEN);
    pr.setTitle("title");
    when(pullRequestService.getInProgress(repository, "source", "target")).thenReturn(Optional.of(pr));

    byte[] pullRequestJson = "{\"source\": \"source\", \"target\": \"target\", \"title\": \"pull request\", \"status\": \"OPEN\"}".getBytes();
    MockHttpRequest request = MockHttpRequest
      .post("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/" + REPOSITORY_NAMESPACE + "/" + REPOSITORY_NAME)
      .content(pullRequestJson)
      .contentType(PullRequestMediaType.PULL_REQUEST);
    dispatcher.invoke(request, response);

    assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_CONFLICT);
    assertThat(response.getContentAsString()).containsPattern("pull request with id id in repository with id .* already exists");
  }

  @Test
  @SubjectAware(username = "dent")
  public void shouldRejectInvalidPullRequest() throws URISyntaxException, IOException {
    byte[] pullRequestJson = loadJson("com/cloudogu/scm/review/pullRequest_invalid.json");
    MockHttpRequest request =
      MockHttpRequest
        .post("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/space/name")
        .content(pullRequestJson)
        .contentType(PullRequestMediaType.PULL_REQUEST);

    dispatcher.invoke(request, response);

    assertEquals(HttpServletResponse.SC_BAD_REQUEST, response.getStatus());
    verify(pullRequestService, never()).add(any(), any());
  }

  @Test
  @SubjectAware(username = "slarti")
  public void shouldRejectSameBranches() throws URISyntaxException, IOException {
    byte[] pullRequestJson = loadJson("com/cloudogu/scm/review/pullRequest_sameBranches.json");
    MockHttpRequest request =
      MockHttpRequest
        .post("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/" + REPOSITORY_NAMESPACE + "/" + REPOSITORY_NAME)
        .content(pullRequestJson)
        .contentType(PullRequestMediaType.PULL_REQUEST);

    dispatcher.invoke(request, response);

    assertEquals(HttpServletResponse.SC_BAD_REQUEST, response.getStatus());
    verify(pullRequestService, never()).add(any(), any());
  }

  @Test
  @SubjectAware(username = "trillian")
  public void shouldHandleMissingRepository() throws URISyntaxException, IOException {
    when(pullRequestService.getRepository("space", "X"))
      .thenThrow(new NotFoundException("x", "y"));
    byte[] pullRequestJson = loadJson("com/cloudogu/scm/review/pullRequest.json");
    MockHttpRequest request =
      MockHttpRequest
        .post("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/space/X")
        .content(pullRequestJson)
        .contentType(PullRequestMediaType.PULL_REQUEST);

    dispatcher.invoke(request, response);

    assertThat(response.getContentAsString()).containsPattern("could not find.*");
  }

  @Test
  @SubjectAware(username = "rr")
  public void shouldGetPullRequest() throws URISyntaxException, UnsupportedEncodingException {
    PullRequest pullRequest = createPullRequest();
    when(commentService.getCount(REPOSITORY_NAMESPACE, REPOSITORY_NAME, pullRequest.getId(), CommentType.TASK_TODO)).thenReturn(1);
    when(commentService.getCount(REPOSITORY_NAMESPACE, REPOSITORY_NAME, pullRequest.getId(), CommentType.TASK_DONE)).thenReturn(2);
    when(pullRequestService.get(REPOSITORY_NAMESPACE, REPOSITORY_NAME, "123")).thenReturn(pullRequest);

    MockHttpRequest request = MockHttpRequest.get("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/" + REPOSITORY_NAMESPACE + "/" + REPOSITORY_NAME + "/123");
    dispatcher.invoke(request, response);

    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getContentAsString()).contains("_links");
    assertThat(response.getContentAsString()).contains("\"tasks\":{\"todo\":1");
    assertThat(response.getContentAsString()).contains("\"done\":2");
  }

  @Test
  @SubjectAware(username = "rr")
  public void shouldSortPullRequestsByLastModified() throws URISyntaxException {
    when(pullRequestService.count(eq(REPOSITORY_NAMESPACE), eq(REPOSITORY_NAME), any())).thenReturn(10);
    MockHttpRequest request = MockHttpRequest.get("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/" + REPOSITORY_NAMESPACE + "/" + REPOSITORY_NAME);
    dispatcher.invoke(request, response);

    verify(pullRequestService)
      .getAll(
        eq(REPOSITORY_NAMESPACE),
        eq(REPOSITORY_NAME),
        argThat(requestParameters ->
          requestParameters.offset() == 0 &&
            requestParameters.limit() == 10 &&
            requestParameters.pullRequestSelector().equals(PullRequestSelector.IN_PROGRESS) &&
            requestParameters.pullRequestSortSelector().equals(PullRequestSortSelector.LAST_MOD_DESC)
        )
      );
  }

  @Test
  @SubjectAware(username = "rr")
  public void shouldGetAllPullRequests() throws URISyntaxException, UnsupportedEncodingException {
    String id_1 = "id_1";
    String id_2 = "ABC ID 2";
    List<PullRequest> pullRequests = Lists.newArrayList(createPullRequest(id_1), createPullRequest(id_2));
    when(pullRequestService.getAll(eq(REPOSITORY_NAMESPACE), eq(REPOSITORY_NAME), any())).thenReturn(pullRequests);
    when(pullRequestService.count(eq(REPOSITORY_NAMESPACE), eq(REPOSITORY_NAME), any())).thenReturn(2);

    // request all PRs without filter
    MockHttpRequest request = MockHttpRequest.get("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/" + REPOSITORY_NAMESPACE + "/" + REPOSITORY_NAME);
    dispatcher.invoke(request, response);
    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getContentAsString()).contains("\"id\":\"" + id_1 + "\"");
    assertThat(response.getContentAsString()).contains("\"id\":\"" + id_2 + "\"");

    // request all PRs with filter: status=ALL
    request = MockHttpRequest.get("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/" + REPOSITORY_NAMESPACE + "/" + REPOSITORY_NAME + "?status=ALL");
    response.reset();
    dispatcher.invoke(request, response);
    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getContentAsString()).contains("\"id\":\"" + id_1 + "\"");
    assertThat(response.getContentAsString()).contains("\"id\":\"" + id_2 + "\"");
  }

  @Test
  @SubjectAware(username = "dent")
  public void shouldGetCreateLinkOnEmptyPullRequests() throws URISyntaxException {
    when(pullRequestService.count(eq(REPOSITORY_NAMESPACE), eq(REPOSITORY_NAME), any()))
      .thenReturn(0);

    // request all PRs without filter
    MockHttpRequest request = MockHttpRequest.get("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/" + REPOSITORY_NAMESPACE + "/" + REPOSITORY_NAME);
    dispatcher.invoke(request, response);

    assertThat(response.getContentAsJson().get("_links").get("create").get("href").asText()).isEqualTo("/v2/pull-requests/ns/repo");
  }

  @Test
  @SubjectAware(username = "rr")
  public void shouldGetAllPullRequestsSortedByIdAscending() throws URISyntaxException {
    when(pullRequestService.count(eq(REPOSITORY_NAMESPACE), eq(REPOSITORY_NAME), any())).thenReturn(10);

    MockHttpRequest request = MockHttpRequest.get("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/" + REPOSITORY_NAMESPACE + "/" + REPOSITORY_NAME + "?status=ALL" + "&sortBy=ID_ASC");
    dispatcher.invoke(request, response);

    verify(pullRequestService)
      .getAll(
        eq(REPOSITORY_NAMESPACE),
        eq(REPOSITORY_NAME),
        argThat(requestParameters ->
          requestParameters.offset() == 0 &&
            requestParameters.limit() == 10 &&
            requestParameters.pullRequestSelector().equals(PullRequestSelector.ALL) &&
            requestParameters.pullRequestSortSelector().equals(PullRequestSortSelector.ID_ASC)
        )
      );
  }

  @Test
  @SubjectAware(username = "rr")
  public void shouldGetAllPullRequestsWithPagination() throws URISyntaxException, UnsupportedEncodingException {
    String id_1 = "id_1";
    String id_2 = "ABC ID 2";
    when(pullRequestService.getAll(eq(REPOSITORY_NAMESPACE), eq(REPOSITORY_NAME), argThat(params -> params.offset() == 0)))
      .thenReturn(List.of(createPullRequest(id_1)));
    when(pullRequestService.getAll(eq(REPOSITORY_NAMESPACE), eq(REPOSITORY_NAME), argThat(params -> params.offset() == 1)))
      .thenReturn(List.of(createPullRequest(id_2)));
    when(pullRequestService.count(eq(REPOSITORY_NAMESPACE), eq(REPOSITORY_NAME), any())).thenReturn(2);

    // Results for first page
    MockHttpRequest request = MockHttpRequest.get("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/" + REPOSITORY_NAMESPACE + "/" + REPOSITORY_NAME + "?page=0&pageSize=1");
    dispatcher.invoke(request, response);
    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getContentAsString()).contains("\"id\":\"" + id_1 + "\"");
    assertThat(response.getContentAsString()).contains("\"page\":0");
    assertThat(response.getContentAsString()).contains("\"pageTotal\":2");
    verify(pullRequestService)
      .getAll(
        eq(REPOSITORY_NAMESPACE),
        eq(REPOSITORY_NAME),
        argThat(requestParameters ->
          requestParameters.offset() == 0 &&
            requestParameters.limit() == 1
        )
      );

    // Results for second page
    request = MockHttpRequest.get("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/" + REPOSITORY_NAMESPACE + "/" + REPOSITORY_NAME + "?page=1&pageSize=1");
    response.reset();
    dispatcher.invoke(request, response);
    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getContentAsString()).contains("\"id\":\"" + id_2 + "\"");
    assertThat(response.getContentAsString()).contains("\"page\":1");
    assertThat(response.getContentAsString()).contains("\"pageTotal\":2");
    verify(pullRequestService)
      .getAll(
        eq(REPOSITORY_NAMESPACE),
        eq(REPOSITORY_NAME),
        argThat(requestParameters ->
          requestParameters.offset() == 1 &&
            requestParameters.limit() == 1
        )
      );
  }

  @Test
  @SubjectAware(username = "slarti", password = "secret")
  public void shouldGetEventsLink() throws URISyntaxException, IOException {
    initRepoWithPRs();
    MockHttpRequest request = MockHttpRequest.get("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/ns/repo");
    dispatcher.invoke(request, response);
    assertThat(response.getStatus()).isEqualTo(200);
    ObjectMapper mapper = new ObjectMapper();
    JsonNode jsonNode = mapper.readValue(response.getContentAsString(), JsonNode.class);
    JsonNode prNode = jsonNode.get("_embedded").get("pullRequests");
    prNode.elements().forEachRemaining(node -> {
      String actualCollectionLink = node.path("_links").path("events").path("href").asText();
      assertThat(actualCollectionLink).isEqualTo("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/ns/repo/" + node.get("id").asText() + "/events");
    });
  }

  @Test
  @SubjectAware(username = "rr")
  public void shouldGetSelfLink() throws URISyntaxException, IOException {
    initRepoWithPRs();
    MockHttpRequest request = MockHttpRequest.get("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/ns/repo");
    dispatcher.invoke(request, response);
    assertThat(response.getStatus()).isEqualTo(200);
    ObjectMapper mapper = new ObjectMapper();
    JsonNode jsonNode = mapper.readValue(response.getContentAsString(), JsonNode.class);
    JsonNode prNode = jsonNode.get("_embedded").get("pullRequests");
    prNode.elements().forEachRemaining(node -> {
      String actual = node.path("_links").path("self").path("href").asText();
      assertThat(actual).isEqualTo("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/ns/repo/" + node.get("id").asText());
    });
  }

  @Test
  @SubjectAware(username = "rr")
  public void shouldNotGetUpdateLinkForUserWithoutPushPermission() throws URISyntaxException, IOException {
    initRepoWithPRs();
    MockHttpRequest request = MockHttpRequest.get("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/ns/repo");
    dispatcher.invoke(request, response);
    assertThat(response.getStatus()).isEqualTo(200);
    ObjectMapper mapper = new ObjectMapper();
    JsonNode jsonNode = mapper.readValue(response.getContentAsString(), JsonNode.class);
    JsonNode prNode = jsonNode.get("_embedded").get("pullRequests");
    prNode.elements().forEachRemaining(node -> assertThat(node.path("_links").get("update")).isNull());
  }

  @Test
  @SubjectAware(username = "slarti")
  public void shouldGetCommentLink() throws URISyntaxException, IOException {
    initRepoWithPRs();
    MockHttpRequest request = MockHttpRequest.get("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/ns/repo");
    dispatcher.invoke(request, response);
    assertThat(response.getStatus()).isEqualTo(200);
    ObjectMapper mapper = new ObjectMapper();
    JsonNode jsonNode = mapper.readValue(response.getContentAsString(), JsonNode.class);
    JsonNode prNode = jsonNode.get("_embedded").get("pullRequests");
    prNode.elements().forEachRemaining(node -> {
      String actualCollectionLink = node.path("_links").path("comments").path("href").asText();
      assertThat(actualCollectionLink).isEqualTo("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/ns/repo/" + node.get("id").asText() + "/comments/");
    });
  }

  @Test
  @SubjectAware(username = "slarti")
  public void shouldGetChangesLink() throws URISyntaxException, IOException {
    initRepoWithPRs();
    MockHttpRequest request = MockHttpRequest.get("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/ns/repo");
    dispatcher.invoke(request, response);
    assertThat(response.getStatus()).isEqualTo(200);
    ObjectMapper mapper = new ObjectMapper();
    JsonNode jsonNode = mapper.readValue(response.getContentAsString(), JsonNode.class);
    JsonNode prNode = jsonNode.get("_embedded").get("pullRequests");
    prNode.elements().forEachRemaining(node -> {
      String actualCollectionLink = node.path("_links").path("changes").path("href").asText();
      assertThat(actualCollectionLink).isEqualTo("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/ns/repo/" + node.get("id").asText() + "/changes/");
    });
  }

  @Test
  @SubjectAware(username = "slarti")
  public void shouldGetUpdateLink() throws URISyntaxException, IOException {
    initRepoWithPRs(OPEN);
    MockHttpRequest request = MockHttpRequest.get("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/ns/repo");

    dispatcher.invoke(request, response);

    assertThat(response.getStatus()).isEqualTo(200);

    ObjectMapper mapper = new ObjectMapper();
    JsonNode jsonNode = mapper.readValue(response.getContentAsString(), JsonNode.class);
    JsonNode prNode = jsonNode.get("_embedded").get("pullRequests");
    prNode.elements().forEachRemaining(node -> {
      String actual = node.path("_links").path("update").path("href").asText();
      assertThat(actual).isEqualTo("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/ns/repo/" + node.get("id").asText());
    });
  }

  @Test
  @SubjectAware(username = "slarti")
  public void shouldUpdatePullRequestSuccessfully() throws URISyntaxException {
    PullRequest existingPullRequest = new PullRequest();
    existingPullRequest.setAuthor("somebody");
    existingPullRequest.setStatus(PullRequestStatus.OPEN);
    mockSinglePullRequest("1", existingPullRequest);

    MockHttpRequest request = MockHttpRequest
      .put("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/ns/repo/1")
      .content("{\"title\": \"new Title\", \"description\": \"new description\", \"status\": \"OPEN\"}".getBytes())
      .contentType(PullRequestMediaType.PULL_REQUEST);
    dispatcher.invoke(request, response);

    assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_NO_CONTENT);
    verify(pullRequestService).update(
      eq(repository),
      eq("1"),
      argThat(pullRequest -> {
          assertThat(pullRequest.getTitle()).isEqualTo("new Title");
          assertThat(pullRequest.getDescription()).isEqualTo("new description");
          return true;
        }
      )
    );
  }

  @Test
  @SubjectAware(username = "slarti")
  public void shouldFailOnUpdatingNonExistingPullRequest() throws URISyntaxException, IOException {
    initRepoWithPRs();
    when(pullRequestService.get(REPOSITORY_NAMESPACE, REPOSITORY_NAME, "opened_1"))
      .thenThrow(new NotFoundException("x", "y"));
    MockHttpRequest request = MockHttpRequest
      .put("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/ns/repo/opened_1")
      .content("{\"title\": \"new Title\", \"description\": \"new description\"}".getBytes())
      .contentType(PullRequestMediaType.PULL_REQUEST);

    dispatcher.invoke(request, response);

    assertThat(response.getContentAsString()).containsPattern("could not find.*");
    verify(pullRequestService, never()).update(any(), any(), any());
  }

  @Test
  @SubjectAware(username = "rr")
  public void shouldFailUpdatingOnMissingModifyPushPermission() throws URISyntaxException {
    MockHttpRequest request = MockHttpRequest
      .put("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/ns/repo/1")
      .content("{\"title\": \"new Title\", \"description\": \"new description\"}".getBytes())
      .contentType(PullRequestMediaType.PULL_REQUEST);
    mockSinglePullRequest("1", createPullRequest());

    dispatcher.invoke(request, response);

    assertEquals(403, response.getStatus());

    verify(pullRequestService, never()).update(any(), any(), any());
  }

  private void mockSinglePullRequest(String id, PullRequest existingPullRequest) {
    when(pullRequestService.get(REPOSITORY_NAMESPACE, REPOSITORY_NAME, id)).thenReturn(existingPullRequest);
    when(pullRequestService.get(repository, id)).thenReturn(existingPullRequest);
  }

  @Test
  public void shouldGetTheSubscriptionLink() throws URISyntaxException, IOException {
    // the PR has no subscriber
    mockSinglePullRequest("1", createPullRequest());

    mockLoggedInUser(new User("user1", "User 1", "email@d.de"));

    MockHttpRequest request = MockHttpRequest
      .get("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/ns/repo/1/subscription");
    dispatcher.invoke(request, response);
    assertThat(response.getStatus()).isEqualTo(200);
    ObjectMapper mapper = new ObjectMapper();
    JsonNode jsonNode = mapper.readValue(response.getContentAsString(), JsonNode.class);
    assertThat(jsonNode.path("_links").get("subscribe")).isNotNull();
    assertThat(jsonNode.path("_links").get("unsubscribe")).isNull();
  }

  @Test
  public void shouldGetTheApproveAndSubscriptionLink() throws URISyntaxException, IOException {
    mockSinglePullRequest("1", createPullRequest());

    mockLoggedInUser(new User("user1", "User 1", "email@d.de"));

    MockHttpRequest request = MockHttpRequest
      .get("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/ns/repo/1");
    dispatcher.invoke(request, response);
    assertThat(response.getStatus()).isEqualTo(200);
    ObjectMapper mapper = new ObjectMapper();
    JsonNode jsonNode = mapper.readValue(response.getContentAsString(), JsonNode.class);
    assertThat(jsonNode.path("_links").get("approve")).isNotNull();
    assertThat(jsonNode.path("_links").get("subscription")).isNotNull();
  }

  @Test
  @SubjectAware(username = "slarti")
  public void shouldGetTheApproveButNoSubscriptionLinkWithoutMail() throws URISyntaxException, IOException {
    mockSinglePullRequest("1", createPullRequest());

    mockLoggedInUser(new User("user1", "User 1", null));

    MockHttpRequest request = MockHttpRequest
      .get("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/ns/repo/1");
    dispatcher.invoke(request, response);
    assertThat(response.getStatus()).isEqualTo(200);
    ObjectMapper mapper = new ObjectMapper();
    JsonNode jsonNode = mapper.readValue(response.getContentAsString(), JsonNode.class);
    assertThat(jsonNode.path("_links").get("approve")).isNotNull();
    assertThat(jsonNode.path("_links").get("subscription")).isNull();
  }

  @Test
  public void shouldNotReturnAnySubscriptionLinkOnMissingUserMail() throws URISyntaxException, IOException {
    mockLoggedInUser(new User("user1", "User 1", null));

    MockHttpRequest request = MockHttpRequest
      .get("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/ns/repo/1/subscription");
    dispatcher.invoke(request, response);
    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getContentAsString()).isNullOrEmpty();
  }

  @Test
  public void shouldGetTheUnsubscribeLink() throws URISyntaxException, IOException {
    when(pullRequestService.isUserSubscribed(repository, "1"))
      .thenReturn(true);
    mockLoggedInUser(new User("user1", "User 1", "email@d.de"));

    MockHttpRequest request = MockHttpRequest
      .get("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/ns/repo/1/subscription");
    dispatcher.invoke(request, response);
    assertThat(response.getStatus()).isEqualTo(200);
    ObjectMapper mapper = new ObjectMapper();
    JsonNode jsonNode = mapper.readValue(response.getContentAsString(), JsonNode.class);
    assertThat(jsonNode.path("_links").get("unsubscribe"))
      .isNotNull();
    assertThat(jsonNode.path("_links").get("subscribe"))
      .isNull();
  }

  private void initRepoWithPRs(PullRequestStatus... status) {
    List<PullRequest> pullRequests = new ArrayList<>();
    if (status.length == 0 || List.of(status).contains(OPEN)) {
      PullRequest openedPR1 = createPullRequest("opened_1", PullRequestStatus.OPEN);
      PullRequest openedPR2 = createPullRequest("opened_2", PullRequestStatus.OPEN);
      openedPR2.setAuthor("author");
      openedPR1.setReviewer(singletonMap("reviewer", false));
      pullRequests.add(openedPR1);
      pullRequests.add(openedPR2);
    }
    if (status.length == 0 || List.of(status).contains(MERGED)) {
      PullRequest mergedPR1 = createPullRequest("merged_1", PullRequestStatus.MERGED);
      PullRequest mergedPR2 = createPullRequest("merged_2", PullRequestStatus.MERGED);
      mergedPR2.setAuthor("author");
      mergedPR1.setReviewer(singletonMap("reviewer", true));
      pullRequests.add(mergedPR1);
      pullRequests.add(mergedPR2);
    }
    if (status.length == 0 || List.of(status).contains(REJECTED)) {
      PullRequest rejectedPR1 = createPullRequest("rejected_1", REJECTED);
      PullRequest rejectedPR2 = createPullRequest("rejected_2", REJECTED);
      pullRequests.add(rejectedPR1);
      pullRequests.add(rejectedPR2);
    }
  }

  private byte[] loadJson(String s) throws IOException {
    URL url = Resources.getResource(s);
    return Resources.toByteArray(url);
  }

  @Test
  @SubjectAware(username = "rr")
  public void shouldReturnCollectionWithOnlySelfLink() throws URISyntaxException, IOException {
    JsonNode links = invokeAndReturnLinks();

    assertThat(links.has("self")).isTrue();
    assertThat(links.has("create")).isFalse();
  }

  @Test
  @SubjectAware(username = "slarti")
  public void shouldReturnCollectionWithCreateLink() throws URISyntaxException, IOException {
    JsonNode links = invokeAndReturnLinks();

    assertThat(links.has("create")).isTrue();
  }

  private JsonNode invokeAndReturnLinks() throws URISyntaxException, IOException {
    initRepoWithPRs();

    String uri = "/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/ns/repo";

    MockHttpRequest request = MockHttpRequest.get(uri);
    dispatcher.invoke(request, response);

    ObjectMapper mapper = new ObjectMapper();
    JsonNode jsonNode = mapper.readValue(response.getContentAsString(), JsonNode.class);
    return jsonNode.get("_links");
  }

  @Test
  public void shouldSetPullRequestToStatusRejected() throws URISyntaxException {
    mockSinglePullRequest("1", createPullRequest("opened_1", PullRequestStatus.OPEN));

    MockHttpRequest request = MockHttpRequest
      .post("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/ns/repo/1/reject");
    dispatcher.invoke(request, response);
    assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_NO_CONTENT);
    verify(pullRequestService)
      .reject(repository, "1", REJECTED_BY_USER);
  }

  @Test
  public void shouldSetPullRequestToStatusRejectedWithMessage() throws URISyntaxException {
    mockSinglePullRequest("1", createPullRequest("opened_1", PullRequestStatus.OPEN));

    JsonMockHttpRequest request = JsonMockHttpRequest
      .post("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/ns/repo/1/rejectWithMessage")
      .json("{'message':'boring'}");
    dispatcher.invoke(request, response);
    assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_NO_CONTENT);
    verify(pullRequestService)
      .reject(repository, "1", "boring");
  }

  @Test
  @SubjectAware(username = "dent")
  public void shouldApprove() throws URISyntaxException {
    MockHttpRequest request = MockHttpRequest
      .post("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/ns/repo/1/approve");
    dispatcher.invoke(request, response);

    verify(pullRequestService).approve(new NamespaceAndName("ns", "repo"), "1");
    assertThat(response.getStatus()).isEqualTo(204);
  }

  @Test
  @SubjectAware(username = "dent")
  public void shouldDisapprove() throws URISyntaxException {
    MockHttpRequest request = MockHttpRequest
      .post("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/ns/repo/1/disapprove");
    dispatcher.invoke(request, response);
    verify(pullRequestService).disapprove(new NamespaceAndName("ns", "repo"), "1");
    assertThat(response.getStatus()).isEqualTo(204);
  }

  @Test
  @SubjectAware(username = "dent")
  public void shouldMarkAsReviewed() throws URISyntaxException {
    PullRequest pr = new PullRequest("1", "source", "target");
    pr.setStatus(PullRequestStatus.OPEN);
    pr.setTitle("title");
    mockSinglePullRequest("1", pr);

    MockHttpRequest request = MockHttpRequest
      .post("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/ns/repo/1/review-mark/some/file");
    dispatcher.invoke(request, response);

    verify(pullRequestService).markAsReviewed(repository, "1", "some/file");
    assertThat(response.getStatus()).isEqualTo(204);
  }

  @Test
  @SubjectAware(username = "dent")
  public void shouldMarkAsNotReviewed() throws URISyntaxException {
    MockHttpRequest request = MockHttpRequest
      .delete("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/ns/repo/1/review-mark/some/file");
    dispatcher.invoke(request, response);

    verify(pullRequestService).markAsNotReviewed(repository, "1", "some/file");
    assertThat(response.getStatus()).isEqualTo(204);
  }

  @Test
  @SubjectAware(username = "dent")
  public void shouldGetMarkAsReviewedLink() throws URISyntaxException, IOException {
    mockSinglePullRequest("1", createPullRequest());

    MockHttpRequest request = MockHttpRequest
      .get("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/ns/repo/1");
    dispatcher.invoke(request, response);

    assertThat(response.getStatus()).isEqualTo(200);
    ObjectMapper mapper = new ObjectMapper();
    JsonNode jsonNode = mapper.readValue(response.getContentAsString(), JsonNode.class);
    assertThat(jsonNode.path("_links").get("reviewMark")).isNotNull();
  }

  @Test
  @SubjectAware(username = "dent")
  public void shouldGetMarkedAsReviewedPaths() throws URISyntaxException, IOException {
    mockLoggedInUser(new User("dent"));

    PullRequest pullRequest = createPullRequest();
    pullRequest.setAuthor("slarti");
    pullRequest.setReviewMarks(Set.of(
      new ReviewMark("/some/file", "dent"),
      new ReviewMark("/some/other/file", "trillian")
    ));

    mockSinglePullRequest("1", pullRequest);

    MockHttpRequest request = MockHttpRequest
      .get("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/ns/repo/1");
    dispatcher.invoke(request, response);
    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getContentAsString()).contains("\"markedAsReviewed\":[\"/some/file\"]");
  }

  @Test
  @SubjectAware(username = "dent")
  public void shouldReturnPullRequestIsValidResultForNewPullRequest() throws URISyntaxException, IOException {
    when(pullRequestService.checkIfPullRequestIsValid(repository, "feature", "master"))
      .thenReturn(PullRequestCheckResultDto.PullRequestCheckStatus.PR_VALID);

    MockHttpRequest request = MockHttpRequest
      .get("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/ns/repo/check?source=feature&target=master");

    dispatcher.invoke(request, response);
    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getContentAsString()).contains("\"status\":\"PR_VALID\"");
    assertThat(response.getContentAsString()).contains("\"_links\":{\"self\":{\"href\":\"/v2/pull-requests/ns/repo/check?source=feature&target=master\"}}");
  }

  @Test
  @SubjectAware(username = "dent")
  public void shouldReturnIllegalRequestIfSourceBranchIsEmpty() throws URISyntaxException {
    MockHttpRequest request = MockHttpRequest
      .get("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/ns/repo/check?source=&target=master");

    dispatcher.invoke(request, response);
    assertThat(response.getStatus()).isEqualTo(400);
  }

  @Test
  @SubjectAware(username = "dent")
  public void shouldReturnIllegalRequestIfTargetBranchIsEmpty() throws URISyntaxException {
    PullRequest pullRequest = createPullRequest();
    pullRequest.setSource("");

    MockHttpRequest request = MockHttpRequest
      .get("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/ns/repo/check?source=feature&target=");

    dispatcher.invoke(request, response);
    assertThat(response.getStatus()).isEqualTo(400);
  }

  @Test
  @SubjectAware(username = "dent")
  public void shouldReturnPullRequestTemplate() throws URISyntaxException, IOException {
    RepositoryPullRequestConfig config = new RepositoryPullRequestConfig();
    config.setDefaultReviewers(List.of("dent"));
    config.setDefaultTasks(List.of("first task", "second"));

    when(configService.evaluateConfig(repository)).thenReturn(config);

    MockHttpRequest request = MockHttpRequest
      .get("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/ns/repo/template");

    dispatcher.invoke(request, response);
    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getContentAsString()).contains("\"defaultReviewers\":[{\"id\":\"dent\",\"displayName\":\"dent\"}]");
    assertThat(response.getContentAsString()).contains("\"_links\":{\"self\":{\"href\":\"/v2/pull-requests/ns/repo/template\"}}");
    assertThat(response.getContentAsString()).contains("\"title\":\"\"");
    assertThat(response.getContentAsString()).contains("\"description\":\"\"");
    assertThat(response.getContentAsString()).contains("\"availableLabels\":[]");
    assertThat(response.getContentAsString()).contains("\"defaultTasks\":[\"first task\",\"second\"]");
  }

  @Test
  @SubjectAware(username = "dent")
  public void shouldReturnPullRequestTemplateWithTitleAndDescriptionForSingleChangeset() throws URISyntaxException, IOException {
    RepositoryPullRequestConfig config = new RepositoryPullRequestConfig();

    when(configService.evaluateConfig(repository)).thenReturn(config);
    when(repositoryServiceFactory.create(repository)).thenReturn(repositoryService);
    when(repositoryService.getLogCommand()).thenReturn(logCommandBuilder);
    when(logCommandBuilder.getChangesets()).thenReturn(new ChangesetPagingResult(0, List.of(new Changeset("hashtag", Instant.now().getEpochSecond(), new Person("Test"), "Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet.\nMore Text"))));

    MockHttpRequest request = MockHttpRequest
      .get("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/ns/repo/template?source=develop&target=master");

    dispatcher.invoke(request, response);
    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getContentAsString()).contains("\"title\":\"");
    assertThat(response.getContentAsString()).contains("\"description\":\"...");
  }

  @Test
  @SubjectAware(username = "dent")
  public void shouldReturnPullRequestTemplateWithTitleAndDescriptionForMultipleChangesets() throws URISyntaxException, IOException {
    RepositoryPullRequestConfig config = new RepositoryPullRequestConfig();

    when(configService.evaluateConfig(repository)).thenReturn(config);
    when(repositoryServiceFactory.create(repository)).thenReturn(repositoryService);
    when(repositoryService.getLogCommand()).thenReturn(logCommandBuilder);
    when(logCommandBuilder.getChangesets()).thenReturn(new ChangesetPagingResult(0, List.of(
      new Changeset("hashtag", Instant.now().getEpochSecond(), new Person("Test"), "Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet.\nMore Text"),
      new Changeset("another", Instant.now().getEpochSecond(), new Person("someguy"), "Another important description")
    )));

    MockHttpRequest request = MockHttpRequest
      .get("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/ns/repo/template?source=develop&target=master");

    dispatcher.invoke(request, response);
    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getContentAsString()).contains("\"title\":\"\"");
    assertThat(response.getContentAsString()).contains("\"description\":\"\"");
  }

  @Test
  @SubjectAware(username = "trillian")
  public void shouldGetUnauthorizedBecauseOfMissingReadPullRequestPermission() throws URISyntaxException {
    MockHttpRequest request = MockHttpRequest
      .get("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/ns/repo/1/changes");

    dispatcher.invoke(request, response);
    assertThat(response.getStatus()).isEqualTo(403);
  }

  @Test
  @SubjectAware(username = "dent")
  public void shouldGetHistoryOfPullRequest() throws URISyntaxException {
    List<PullRequestChange> changes = List.of(
      new PullRequestChange(
        "1",
        "dent",
        "Dent",
        "dent@mail.com",
        Instant.now(),
        "previous",
        "current",
        "Property",
        Map.of("key", "value")
      ),
      new PullRequestChange(
        "1",
        "dent",
        "Dent",
        "dent@mail.com",
        Instant.now(),
        "other previous value",
        "other current value",
        "Other Property",
        Map.of("Other key", "Other value")
      )
    );

    when(pullRequestChangeService.getAllChangesOfPullRequest(repository.getNamespaceAndName(), "1"))
      .thenReturn(changes);

    MockHttpRequest request = MockHttpRequest
      .get("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/ns/repo/1/changes");

    dispatcher.invoke(request, response);
    assertThat(response.getStatus()).isEqualTo(200);

    JsonNode node = response.getContentAsJson();
    assertThat(node.isArray()).isTrue();
    assertThat(node.size()).isEqualTo(2);

    assertThat(node.get(0).get("prId").asText()).isEqualTo(changes.get(0).getPrId());
    assertThat(node.get(0).get("username").asText()).isEqualTo(changes.get(0).getUsername());
    assertThat(node.get(0).get("displayName").asText()).isEqualTo(changes.get(0).getDisplayName());
    assertThat(node.get(0).get("mail").asText()).isEqualTo(changes.get(0).getMail());
    assertThat(node.get(0).get("changedAt").asText()).isEqualTo(changes.get(0).getChangedAt().toString());
    assertThat(node.get(0).get("previousValue").asText()).isEqualTo(changes.get(0).getPreviousValue());
    assertThat(node.get(0).get("currentValue").asText()).isEqualTo(changes.get(0).getCurrentValue());
    assertThat(node.get(0).get("property").asText()).isEqualTo(changes.get(0).getProperty());
    assertThat(node.get(0).get("additionalInfo").get("key").asText()).isEqualTo(changes.get(0).getAdditionalInfo().get("key"));

    assertThat(node.get(1).get("prId").asText()).isEqualTo(changes.get(1).getPrId());
    assertThat(node.get(1).get("username").asText()).isEqualTo(changes.get(1).getUsername());
    assertThat(node.get(1).get("displayName").asText()).isEqualTo(changes.get(1).getDisplayName());
    assertThat(node.get(1).get("mail").asText()).isEqualTo(changes.get(1).getMail());
    assertThat(node.get(1).get("changedAt").asText()).isEqualTo(changes.get(1).getChangedAt().toString());
    assertThat(node.get(1).get("previousValue").asText()).isEqualTo(changes.get(1).getPreviousValue());
    assertThat(node.get(1).get("currentValue").asText()).isEqualTo(changes.get(1).getCurrentValue());
    assertThat(node.get(1).get("property").asText()).isEqualTo(changes.get(1).getProperty());
    assertThat(node.get(1).get("additionalInfo").get("Other key").asText()).isEqualTo(changes.get(1).getAdditionalInfo().get("Other key"));
  }

  private void mockLoggedInUser(User user) {
    ThreadContext.bind(subject);

    PrincipalCollection principals = mock(PrincipalCollection.class);
    when(subject.getPrincipals()).thenReturn(principals);
    when(subject.isPermitted(any(String.class))).thenReturn(true);
    when(principals.oneByType(User.class)).thenReturn(user);
  }

  private void mockPrincipal() {
    ThreadContext.bind(subject);
    PrincipalCollection principals = mock(PrincipalCollection.class);
    when(subject.getPrincipals()).thenReturn(principals);
    User user1 = new User();
    user1.setName("user1");
    user1.setDisplayName("User 1");
    when(principals.oneByType(User.class)).thenReturn(user1);
  }
}
