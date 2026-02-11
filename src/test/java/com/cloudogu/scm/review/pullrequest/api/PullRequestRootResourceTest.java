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
import com.cloudogu.scm.review.RepositoryResolver;
import com.cloudogu.scm.review.comment.service.Comment;
import com.cloudogu.scm.review.comment.service.CommentService;
import com.cloudogu.scm.review.comment.service.CommentType;
import com.cloudogu.scm.review.comment.service.Location;
import com.cloudogu.scm.review.config.service.BasePullRequestConfig;
import com.cloudogu.scm.review.config.service.ConfigService;
import com.cloudogu.scm.review.config.service.RepositoryPullRequestConfig;
import com.cloudogu.scm.review.pullrequest.dto.PullRequestMapperImpl;
import com.cloudogu.scm.review.pullrequest.service.DefaultPullRequestService;
import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import com.cloudogu.scm.review.pullrequest.service.PullRequestChange;
import com.cloudogu.scm.review.pullrequest.service.PullRequestChangeService;
import com.cloudogu.scm.review.pullrequest.service.PullRequestRejectedEvent;
import com.cloudogu.scm.review.pullrequest.service.PullRequestStatus;
import com.cloudogu.scm.review.pullrequest.service.PullRequestStore;
import com.cloudogu.scm.review.pullrequest.service.PullRequestStoreFactory;
import com.cloudogu.scm.review.pullrequest.service.ReviewMark;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.sdorra.shiro.ShiroRule;
import com.github.sdorra.shiro.SubjectAware;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import sonia.scm.NotFoundException;
import sonia.scm.api.v2.resources.BranchLinkProvider;
import sonia.scm.event.ScmEventBus;
import sonia.scm.repository.Branch;
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

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.cloudogu.scm.review.TestData.createPullRequest;
import static com.cloudogu.scm.review.pullrequest.service.PullRequestStatus.OPEN;
import static com.cloudogu.scm.review.pullrequest.service.PullRequestStatus.REJECTED;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
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

  private final RepositoryResolver repositoryResolver = mock(RepositoryResolver.class);
  private final BranchResolver branchResolver = mock(BranchResolver.class);
  private final PullRequestStoreFactory storeFactory = mock(PullRequestStoreFactory.class);
  private final PullRequestStore store = mock(PullRequestStore.class);
  private final Repository repository = mock(Repository.class);
  private final BranchLinkProvider branchLinkProvider = mock(BranchLinkProvider.class);
  private final ArgumentCaptor<PullRequest> pullRequestStoreCaptor = ArgumentCaptor.forClass(PullRequest.class);

  private RestDispatcher dispatcher;

  private final JsonMockHttpResponse response = new JsonMockHttpResponse();
  private final Subject subject = mock(Subject.class);

  private static final String REPOSITORY_NAME = "repo";
  private static final String REPOSITORY_ID = "repo_ID";
  private static final String REPOSITORY_NAMESPACE = "ns";

  @Mock
  private UserDisplayManager userDisplayManager;
  @Mock
  private DefaultPullRequestService pullRequestService;
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
  @InjectMocks
  private PullRequestMapperImpl mapper;

  private final ScmEventBus eventBus = mock(ScmEventBus.class);

  @Before
  public void init() {
    when(repository.getId()).thenReturn(REPOSITORY_ID);
    when(repository.getName()).thenReturn(REPOSITORY_NAME);
    when(repository.getNamespace()).thenReturn(REPOSITORY_NAMESPACE);
    when(repository.getNamespaceAndName()).thenReturn(new NamespaceAndName(REPOSITORY_NAMESPACE, REPOSITORY_NAME));
    when(repositoryResolver.resolve(any())).thenReturn(repository);
    when(pullRequestService.getRepository(repository.getNamespace(), repository.getName())).thenReturn(repository);
    DefaultPullRequestService service = new DefaultPullRequestService(repositoryResolver, branchResolver, storeFactory, eventBus, repositoryServiceFactory);
    PullRequestRootResource pullRequestRootResource = new PullRequestRootResource(
      mapper,
      service,
      commentService,
      repositoryServiceFactory,
      Providers.of(new PullRequestResource(
        mapper,
        service,
        null,
        null,
        channelRegistry,
        pullRequestChangeService
      )),
      configService,
      userDisplayManager
    );
    when(storeFactory.create(null)).thenReturn(store);
    when(storeFactory.create(any())).thenReturn(store);
    when(store.add(pullRequestStoreCaptor.capture())).thenReturn("1");
    when(branchLinkProvider.get(any(NamespaceAndName.class), anyString())).thenReturn("");
    dispatcher = new RestDispatcher();
    dispatcher.addSingletonResource(pullRequestRootResource);
    lenient().when(repositoryServiceFactory.create(any(Repository.class))).thenReturn(repositoryService);
    lenient().when(userDisplayManager.get("reviewer")).thenReturn(Optional.of(DisplayUser.from(new User("reviewer", "reviewer", ""))));
    when(configService.evaluateConfig(repository)).thenReturn(new BasePullRequestConfig());
  }

  @After
  public void unbindSubject() {
    ThreadContext.unbindSubject();
  }

  @Test
  @SubjectAware(username = "slarti")
  public void shouldCreateNewValidPullRequest() throws URISyntaxException, IOException {
    mockPrincipal();
    mockChangesets("sourceBranch", "targetBranch", new Changeset());

    byte[] pullRequestJson = loadJson("com/cloudogu/scm/review/pullRequest.json");
    MockHttpRequest request =
      MockHttpRequest
        .post("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/space/name")
        .content(pullRequestJson)
        .contentType(PullRequestMediaType.PULL_REQUEST);

    dispatcher.invoke(request, response);

    assertEquals(HttpServletResponse.SC_CREATED, response.getStatus());
    assertThat(response.getOutputHeaders().getFirst("Location")).hasToString("/v2/pull-requests/space/name/1");
    PullRequest pullRequest = pullRequestStoreCaptor.getValue();
    assertThat(pullRequest.getAuthor()).isEqualTo("user1");
  }

  @Test
  @SubjectAware(username = "slarti")
  public void shouldCreateNewPullRequestWithDefaultStatusOpen() throws URISyntaxException, IOException {
    mockPrincipal();
    mockChangesets("sourceBranch", "targetBranch", new Changeset());

    byte[] pullRequestJson = loadJson("com/cloudogu/scm/review/pullRequest_without_status.json");
    MockHttpRequest request =
      MockHttpRequest
        .post("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/space/name")
        .content(pullRequestJson)
        .contentType(PullRequestMediaType.PULL_REQUEST);

    dispatcher.invoke(request, response);

    assertEquals(HttpServletResponse.SC_CREATED, response.getStatus());
    assertThat(response.getOutputHeaders().getFirst("Location")).hasToString("/v2/pull-requests/space/name/1");
    PullRequest pullRequest = pullRequestStoreCaptor.getValue();
    assertThat(pullRequest.getAuthor()).isEqualTo("user1");
    assertThat(pullRequest.getStatus()).isEqualTo(OPEN);
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
    MockHttpRequest request = MockHttpRequest.get("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/" + REPOSITORY_NAMESPACE + "/" + REPOSITORY_NAME + "");
    dispatcher.invoke(request, response);

    assertEquals(HttpServletResponse.SC_FORBIDDEN, response.getStatus());
  }

  @Test
  @SubjectAware(username = "trillian")
  public void shouldGetUnauthorizedExceptionWhenMissingPermissionOnCreatePR() throws URISyntaxException, IOException {
    byte[] pullRequestJson = loadJson("com/cloudogu/scm/review/pullRequest.json");
    MockHttpRequest request = MockHttpRequest.post("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/space/name")
      .content(pullRequestJson)
      .contentType(PullRequestMediaType.PULL_REQUEST);
    dispatcher.invoke(request, response);

    assertEquals(HttpServletResponse.SC_FORBIDDEN, response.getStatus());
  }

  @Test
  @SubjectAware(username = "slarti")
  public void shouldGetAlreadyExistsExceptionOnCreatePR() throws URISyntaxException, IOException {
    PullRequest pr = new PullRequest();
    pr.setStatus(PullRequestStatus.OPEN);
    pr.setSource("source");
    pr.setTarget("target");
    pr.setTitle("title");
    pr.setId("id");
    when(store.getAll()).thenReturn(Lists.newArrayList(pr));

    byte[] pullRequestJson = "{\"source\": \"source\", \"target\": \"target\", \"title\": \"pull request\", \"status\": \"OPEN\"}".getBytes();
    when(repositoryResolver.resolve(new NamespaceAndName(REPOSITORY_NAMESPACE, REPOSITORY_NAME))).thenReturn(repository);
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
    verify(store, never()).add(any());
  }

  @Test
  @SubjectAware(username = "slarti")
  public void shouldRejectSameBranches() throws URISyntaxException, IOException {
    byte[] pullRequestJson = loadJson("com/cloudogu/scm/review/pullRequest_sameBranches.json");
    MockHttpRequest request =
      MockHttpRequest
        .post("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/space/name")
        .content(pullRequestJson)
        .contentType(PullRequestMediaType.PULL_REQUEST);

    dispatcher.invoke(request, response);

    assertEquals(HttpServletResponse.SC_BAD_REQUEST, response.getStatus());
    verify(store, never()).add(any());
  }

  @Test
  @SubjectAware(username = "slarti")
  public void shouldRejectWithoutDiff() throws URISyntaxException, IOException {
    mockPrincipal();
    mockChangesets("sourceBranch", "targetBranch");

    byte[] pullRequestJson = loadJson("com/cloudogu/scm/review/pullRequest.json");
    MockHttpRequest request =
      MockHttpRequest
        .post("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/space/name")
        .content(pullRequestJson)
        .contentType(PullRequestMediaType.PULL_REQUEST);

    dispatcher.invoke(request, response);

    assertEquals(HttpServletResponse.SC_BAD_REQUEST, response.getStatus());
    verify(store, never()).add(any());
  }

  @Test
  @SubjectAware(username = "trillian")
  public void shouldHandleMissingRepository() throws URISyntaxException, IOException {
    when(repositoryResolver.resolve(new NamespaceAndName("space", "X")))
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
  @SubjectAware(username = "slarti")
  public void shouldHandleMissingBranch() throws URISyntaxException, IOException {
    when(branchResolver.resolve(repository, "sourceBranch")).thenThrow(new NotFoundException("x", "y"));
    byte[] pullRequestJson = loadJson("com/cloudogu/scm/review/pullRequest.json");
    MockHttpRequest request =
      MockHttpRequest
        .post("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/" + REPOSITORY_NAMESPACE + "/" + REPOSITORY_NAME)
        .content(pullRequestJson)
        .contentType(PullRequestMediaType.PULL_REQUEST);

    dispatcher.invoke(request, response);

    assertThat(response.getContentAsString()).containsPattern("could not find.*");
  }

  @Test
  @SubjectAware(username = "rr")
  public void shouldGetPullRequest() throws URISyntaxException, UnsupportedEncodingException {
    when(repositoryResolver.resolve(new NamespaceAndName(REPOSITORY_NAMESPACE, REPOSITORY_NAME))).thenReturn(repository);
    PullRequest pullRequest = createPullRequest();
    List<Comment> comments = new ArrayList<>();
    comments.add(createCommentWithType(CommentType.TASK_TODO));
    when(commentService.getAll(REPOSITORY_NAMESPACE, REPOSITORY_NAME, pullRequest.getId())).thenReturn(comments);
    when(store.get("123")).thenReturn(pullRequest);
    MockHttpRequest request = MockHttpRequest.get("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/" + REPOSITORY_NAMESPACE + "/" + REPOSITORY_NAME + "/123");
    dispatcher.invoke(request, response);
    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getContentAsString()).contains("_links");
    assertThat(response.getContentAsString()).contains("\"tasks\":{\"todo\":1");
    assertThat(response.getContentAsString()).contains("\"done\":0");
  }

  private Comment createCommentWithType(CommentType commentType) {
    Comment comment = Comment.createComment("1", "trillian", "tricia", new Location());
    comment.setType(commentType);
    return comment;
  }

  @Test
  @SubjectAware(username = "rr")
  public void shouldSortPullRequestsByLastModified() throws URISyntaxException, IOException {
    when(repositoryResolver.resolve(new NamespaceAndName(REPOSITORY_NAMESPACE, REPOSITORY_NAME))).thenReturn(repository);
    String firstPR = "first_PR";
    String toDayUpdatedPR = "to_day_updated_PR";
    String lastPR = "last_PR";

    Instant firstCreation = Instant.MIN;
    Instant firstCreationPlusOneDay = firstCreation.plus(Duration.ofDays(1));
    Instant lastCreation = Instant.MAX;

    PullRequest firstPullRequest = createPullRequest(firstPR);
    firstPullRequest.setCreationDate(firstCreationPlusOneDay);
    firstPullRequest.setLastModified(firstCreationPlusOneDay);

    PullRequest toDayUpdatedPullRequest = createPullRequest(toDayUpdatedPR);
    toDayUpdatedPullRequest.setCreationDate(firstCreation);
    toDayUpdatedPullRequest.setLastModified(Instant.now());

    PullRequest lastPullRequest = createPullRequest(lastPR);
    lastPullRequest.setCreationDate(lastCreation);
    lastPullRequest.setLastModified(lastCreation);

    when(store.getAll()).thenReturn(Lists.newArrayList(lastPullRequest, firstPullRequest, toDayUpdatedPullRequest));
    MockHttpRequest request = MockHttpRequest.get("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/" + REPOSITORY_NAMESPACE + "/" + REPOSITORY_NAME + "");
    dispatcher.invoke(request, response);
    assertThat(response.getStatus()).isEqualTo(200);
    ObjectMapper mapper = new ObjectMapper();
    JsonNode jsonNode = mapper.readValue(response.getContentAsString(), JsonNode.class);
    JsonNode prNode = jsonNode.get("_embedded").get("pullRequests");
    JsonNode pr_1 = prNode.path(0);
    JsonNode pr_2 = prNode.path(1);
    JsonNode pr_3 = prNode.path(2);

    assertThat(pr_1.get("id").asText()).isEqualTo(lastPR);
    assertThat(pr_2.get("id").asText()).isEqualTo(toDayUpdatedPR);
    assertThat(pr_3.get("id").asText()).isEqualTo(firstPR);

    // Check with default values
    assertThat(jsonNode.get("page").asText()).isEqualTo("0");
    assertThat(jsonNode.get("pageTotal").asText()).isEqualTo("1");
  }

  @Test
  @SubjectAware(username = "rr")
  public void shouldGetAllPullRequests() throws URISyntaxException, UnsupportedEncodingException {
    when(repositoryResolver.resolve(new NamespaceAndName(REPOSITORY_NAMESPACE, REPOSITORY_NAME))).thenReturn(repository);
    String id_1 = "id_1";
    String id_2 = "ABC ID 2";
    List<PullRequest> pullRequests = Lists.newArrayList(createPullRequest(id_1), createPullRequest(id_2));
    when(store.getAll()).thenReturn(pullRequests);

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
    when(repositoryResolver.resolve(new NamespaceAndName(REPOSITORY_NAMESPACE, REPOSITORY_NAME))).thenReturn(repository);
    when(store.getAll()).thenReturn(emptyList());

    // request all PRs without filter
    MockHttpRequest request = MockHttpRequest.get("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/" + REPOSITORY_NAMESPACE + "/" + REPOSITORY_NAME);
    dispatcher.invoke(request, response);

    assertThat(response.getContentAsJson().get("_links").get("create").get("href").asText()).isEqualTo("/v2/pull-requests/ns/repo");
  }

  @Test
  @SubjectAware(username = "rr")
  public void shouldGetAllPullRequestsSortedByIdAscending() throws URISyntaxException, UnsupportedEncodingException {
    when(repositoryResolver.resolve(new NamespaceAndName(REPOSITORY_NAMESPACE, REPOSITORY_NAME))).thenReturn(repository);
    List<String> expectedIds = new ArrayList<>(){{add("1"); add("2"); add("3");}};
    List<PullRequest> pullRequests = Lists.newArrayList(createPullRequest(expectedIds.get(1)), createPullRequest(expectedIds.get(2)), createPullRequest(expectedIds.get(0)));
    when(store.getAll()).thenReturn(pullRequests);

    MockHttpRequest request = MockHttpRequest.get("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/" + REPOSITORY_NAMESPACE + "/" + REPOSITORY_NAME + "?status=ALL" + "&sortBy=ID_ASC");
    JsonMockHttpResponse jsonResponse = new JsonMockHttpResponse();
    dispatcher.invoke(request, jsonResponse);
    JsonNode contentAsJson = jsonResponse.getContentAsJson();
    JsonNode embedded = contentAsJson.get("_embedded").get("pullRequests");
    List<String> sortedIds = new ArrayList<>();
    for(JsonNode node : embedded) {
      sortedIds.add(node.get("id").asText());
    }
    assertThat(jsonResponse.getStatus()).isEqualTo(200);
    assertThat(sortedIds).isEqualTo(expectedIds);
  }

  @Test
  @SubjectAware(username = "rr")
  public void shouldGetAllPullRequestsSortedByIdDescending() throws URISyntaxException, UnsupportedEncodingException {
    when(repositoryResolver.resolve(new NamespaceAndName(REPOSITORY_NAMESPACE, REPOSITORY_NAME))).thenReturn(repository);
    List<String> ids = new ArrayList<>(){{add("3"); add("2"); add("1");}};
    List<PullRequest> pullRequests = Lists.newArrayList(createPullRequest(ids.get(1)), createPullRequest(ids.get(2)), createPullRequest(ids.get(0)));
    when(store.getAll()).thenReturn(pullRequests);

    MockHttpRequest request = MockHttpRequest.get("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/" + REPOSITORY_NAMESPACE + "/" + REPOSITORY_NAME + "?status=ALL" + "&sortBy=ID_DESC");
    JsonMockHttpResponse jsonResponse = new JsonMockHttpResponse();
    dispatcher.invoke(request, jsonResponse);
    JsonNode contentAsJson = jsonResponse.getContentAsJson();
    JsonNode embedded = contentAsJson.get("_embedded").get("pullRequests");
    List<String> sortedIds = new ArrayList<>();
    for(JsonNode node : embedded) {
      sortedIds.add(node.get("id").asText());
    }
    assertThat(jsonResponse.getStatus()).isEqualTo(200);
    assertThat(sortedIds).isEqualTo(ids);
  }

  @Test
  @SubjectAware(username = "rr")
  public void shouldGetAllPullRequestsWithPagination() throws URISyntaxException, UnsupportedEncodingException {
    when(repositoryResolver.resolve(new NamespaceAndName(REPOSITORY_NAMESPACE, REPOSITORY_NAME))).thenReturn(repository);
    String id_1 = "id_1";
    String id_2 = "ABC ID 2";
    List<PullRequest> pullRequests = Lists.newArrayList(createPullRequest(id_1), createPullRequest(id_2));
    when(store.getAll()).thenReturn(pullRequests);

    // Results for first page
    MockHttpRequest request = MockHttpRequest.get("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/" + REPOSITORY_NAMESPACE + "/" + REPOSITORY_NAME + "?page=0&pageSize=1");
    dispatcher.invoke(request, response);
    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getContentAsString()).contains("\"id\":\"" + id_1 + "\"");
    assertThat(response.getContentAsString()).contains("\"page\":0");
    assertThat(response.getContentAsString()).contains("\"pageTotal\":2");

    // Results for second page
    request = MockHttpRequest.get("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/" + REPOSITORY_NAMESPACE + "/" + REPOSITORY_NAME + "?page=1&pageSize=1");
    response.reset();
    dispatcher.invoke(request, response);
    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getContentAsString()).contains("\"id\":\"" + id_2 + "\"");
    assertThat(response.getContentAsString()).contains("\"page\":1");
    assertThat(response.getContentAsString()).contains("\"pageTotal\":2");
  }

  @Test
  @SubjectAware(username = "rr")
  public void shouldGetOpenedPullRequests() throws URISyntaxException, IOException {
    verifyFilteredPullRequests(PullRequestSelector.IN_PROGRESS.name(), PullRequestStatus.OPEN.name());
  }

  @Test
  @SubjectAware(username = "rr")
  public void shouldGetRejectedPullRequests() throws URISyntaxException, IOException {
    verifyFilteredPullRequests(PullRequestSelector.REJECTED.name());
  }

  @Test
  @SubjectAware(username = "rr")
  public void shouldGetMergedPullRequests() throws URISyntaxException, IOException {
    verifyFilteredPullRequests(PullRequestSelector.MERGED.name());
  }

  @Test
  @SubjectAware(username = "author")
  public void shouldGetMinePullRequests() throws URISyntaxException, IOException {
    initRepoWithPRs("ns", "repo");
    MockHttpRequest request = MockHttpRequest.get("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/ns/repo?status=MINE");
    dispatcher.invoke(request, response);
    assertThat(response.getStatus()).isEqualTo(200);
    JsonNode jsonNode = new ObjectMapper().readValue(response.getContentAsString(), JsonNode.class);
    JsonNode prNode = jsonNode.get("_embedded").get("pullRequests");
    assertThat(prNode.elements().hasNext()).isTrue();
    prNode.elements().forEachRemaining(node -> assertThat(node.get("author").get("id").asText()).isEqualTo("author"));
  }

  @Test
  @SubjectAware(username = "reviewer")
  public void shouldGetReviewerPullRequests() throws URISyntaxException, IOException {
    initRepoWithPRs("ns", "repo");
    MockHttpRequest request = MockHttpRequest.get("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/ns/repo?status=REVIEWER");
    dispatcher.invoke(request, response);
    assertThat(response.getStatus()).isEqualTo(200);
    JsonNode jsonNode = new ObjectMapper().readValue(response.getContentAsString(), JsonNode.class);
    JsonNode prNode = jsonNode.get("_embedded").get("pullRequests");
    assertThat(prNode.elements().hasNext()).isTrue();
    prNode.elements().forEachRemaining(
      node -> {
        assertThat(node.get("reviewer").get(0).get("id").asText()).isEqualTo("reviewer");
        assertThat(node.get("status").asText()).isEqualTo("OPEN");
      }
    );
  }

  @Test
  @SubjectAware(username = "slarti", password = "secret")
  public void shouldGetEventsLink() throws URISyntaxException, IOException {
    initRepoWithPRs("ns", "repo");
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
    initRepoWithPRs("ns", "repo");
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
    initRepoWithPRs("ns", "repo");
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
    initRepoWithPRs("ns", "repo");
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
    initRepoWithPRs("ns", "repo");
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
    initRepoWithPRs("ns", "repo");
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
    when(store.get("1")).thenReturn(existingPullRequest);

    MockHttpRequest request = MockHttpRequest
      .put("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/ns/repo/1")
      .content("{\"title\": \"new Title\", \"description\": \"new description\", \"status\": \"OPEN\"}".getBytes())
      .contentType(PullRequestMediaType.PULL_REQUEST);
    dispatcher.invoke(request, response);

    assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_NO_CONTENT);
    verify(store).update(argThat(pullRequest -> {
      assertThat(pullRequest.getTitle()).isEqualTo("new Title");
      assertThat(pullRequest.getDescription()).isEqualTo("new description");
      return true;
    }));
  }

  @Test
  @SubjectAware(username = "slarti")
  public void shouldFailOnUpdatingNonExistingPullRequest() throws URISyntaxException, IOException {
    initRepoWithPRs("ns", "repo");
    when(store.get("opened_1")).thenThrow(new NotFoundException("x", "y"));
    MockHttpRequest request = MockHttpRequest
      .put("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/ns/repo/opened_1")
      .content("{\"title\": \"new Title\", \"description\": \"new description\"}".getBytes())
      .contentType(PullRequestMediaType.PULL_REQUEST);

    dispatcher.invoke(request, response);

    assertThat(response.getContentAsString()).containsPattern("could not find.*");
    verify(store, never()).update(any());
  }

  @Test
  @SubjectAware(username = "rr")
  public void shouldFailUpdatingOnMissingModifyPushPermission() throws URISyntaxException {
    MockHttpRequest request = MockHttpRequest
      .put("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/ns/repo/1")
      .content("{\"title\": \"new Title\", \"description\": \"new description\"}".getBytes())
      .contentType(PullRequestMediaType.PULL_REQUEST);
    PullRequest pullRequest = createPullRequest();
    when(store.get("1")).thenReturn(pullRequest);

    dispatcher.invoke(request, response);

    assertEquals(403, response.getStatus());

    verify(store, never()).update(any());
  }


  @Test
  public void shouldGetTheSubscriptionLink() throws URISyntaxException, IOException {
    // the PR has no subscriber
    PullRequest pullRequest = createPullRequest();

    when(store.get("1")).thenReturn(pullRequest);

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
    PullRequest pullRequest = createPullRequest();

    when(store.get("1")).thenReturn(pullRequest);

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
  public void shouldGetTheApproveButNoSubscriptionLinkWithoutMail() throws URISyntaxException, IOException {
    PullRequest pullRequest = createPullRequest();

    when(store.get("1")).thenReturn(pullRequest);

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
    // the PR has no subscriber
    mockLoggedInUser(new User("user1", "User 1", null));

    MockHttpRequest request = MockHttpRequest
      .get("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/ns/repo/1/subscription");
    dispatcher.invoke(request, response);
    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getContentAsString()).isNullOrEmpty();
  }

  @Test
  public void shouldGetTheUnsubscribeLink() throws URISyntaxException, IOException {
    PullRequest pullRequest = createPullRequest();
    pullRequest.setSubscriber(singleton("user1"));

    when(store.get("1")).thenReturn(pullRequest);

    mockLoggedInUser(new User("user1", "User 1", "email@d.de"));

    MockHttpRequest request = MockHttpRequest
      .get("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/ns/repo/1/subscription");
    dispatcher.invoke(request, response);
    assertThat(response.getStatus()).isEqualTo(200);
    ObjectMapper mapper = new ObjectMapper();
    JsonNode jsonNode = mapper.readValue(response.getContentAsString(), JsonNode.class);
    assertThat(jsonNode.path("_links").get("subscribe"))
      .isNull();
    assertThat(jsonNode.path("_links").get("unsubscribe"))
      .isNotNull();
  }

  @Test
  @SubjectAware(username = "rr")
  public void shouldGetPullRequestsBySourceBranch() throws URISyntaxException, UnsupportedEncodingException {
    when(repositoryResolver.resolve(new NamespaceAndName(REPOSITORY_NAMESPACE, REPOSITORY_NAME))).thenReturn(repository);
    String id_1 = "1";
    String id_2 = "2";
    PullRequest pullRequest1 = createPullRequest(id_1);
    pullRequest1.setSource("feature/1");
    PullRequest pullRequest2 = createPullRequest(id_2);
    pullRequest2.setSource("feature/2");
    List<PullRequest> pullRequests = Lists.newArrayList(pullRequest1, pullRequest2);
    when(store.getAll()).thenReturn(pullRequests);

    // request all PRs filtered by source branch
    MockHttpRequest request = MockHttpRequest.get("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/" + REPOSITORY_NAMESPACE + "/" + REPOSITORY_NAME + "?source=feature/1");
    dispatcher.invoke(request, response);
    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getContentAsString()).contains("\"id\":\"1\"");
    assertThat(response.getContentAsString()).doesNotContain("\"id\":\"2\"");
  }

  @Test
  @SubjectAware(username = "rr")
  public void shouldGetPullRequestsByTargetBranch() throws URISyntaxException, UnsupportedEncodingException {
    when(repositoryResolver.resolve(new NamespaceAndName(REPOSITORY_NAMESPACE, REPOSITORY_NAME))).thenReturn(repository);
    String id_1 = "1";
    String id_2 = "2";
    PullRequest pullRequest1 = createPullRequest(id_1);
    pullRequest1.setTarget("develop");
    PullRequest pullRequest2 = createPullRequest(id_2);
    pullRequest2.setTarget("main");
    List<PullRequest> pullRequests = Lists.newArrayList(pullRequest1, pullRequest2);
    when(store.getAll()).thenReturn(pullRequests);

    // request all PRs filtered by target branch
    MockHttpRequest request = MockHttpRequest.get("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/" + REPOSITORY_NAMESPACE + "/" + REPOSITORY_NAME + "?target=develop");
    dispatcher.invoke(request, response);
    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getContentAsString()).contains("\"id\":\"1\"");
    assertThat(response.getContentAsString()).doesNotContain("\"id\":\"2\"");
  }

  private void verifyFilteredPullRequests(String status) throws URISyntaxException, IOException {
    verifyFilteredPullRequests(status, status);
  }

  private void verifyFilteredPullRequests(String filter, String expectedStatus) throws URISyntaxException, IOException {
    initRepoWithPRs("ns", "repo");
    MockHttpRequest request = MockHttpRequest.get("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/ns/repo?status=" + filter);
    dispatcher.invoke(request, response);
    assertThat(response.getStatus()).isEqualTo(200);
    ObjectMapper mapper = new ObjectMapper();
    JsonNode jsonNode = mapper.readValue(response.getContentAsString(), JsonNode.class);
    JsonNode prNode = jsonNode.get("_embedded").get("pullRequests");
    assertThat(prNode.elements().hasNext()).isTrue();
    prNode.elements().forEachRemaining(node -> assertThat(node.get("status").asText()).isEqualTo(expectedStatus));
  }

  private void initRepoWithPRs(String namespace, String name) throws IOException {
    mockChangesets("develop", "master", new Changeset());
    when(repository.getNamespace()).thenReturn(namespace);
    when(repository.getName()).thenReturn(name);
    PullRequest openedPR1 = createPullRequest("opened_1", PullRequestStatus.OPEN);
    PullRequest openedPR2 = createPullRequest("opened_2", PullRequestStatus.OPEN);
    PullRequest mergedPR1 = createPullRequest("merged_1", PullRequestStatus.MERGED);
    PullRequest mergedPR2 = createPullRequest("merged_2", PullRequestStatus.MERGED);
    PullRequest rejectedPR1 = createPullRequest("rejected_1", REJECTED);
    PullRequest rejectedPR2 = createPullRequest("rejected_2", REJECTED);
    openedPR2.setAuthor("author");
    mergedPR2.setAuthor("author");
    openedPR1.setReviewer(singletonMap("reviewer", false));
    mergedPR1.setReviewer(singletonMap("reviewer", true));
    when(store.getAll()).thenReturn(Lists.newArrayList(openedPR1, openedPR2, rejectedPR1, rejectedPR2, mergedPR1, mergedPR2));
    when(commentService.getAll(any(), any(), any())).thenReturn(Collections.emptyList());
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
    initRepoWithPRs("hitchhiker", "DeepThought");

    String uri = "/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/ns/repo";

    MockHttpRequest request = MockHttpRequest.get(uri);
    dispatcher.invoke(request, response);

    ObjectMapper mapper = new ObjectMapper();
    JsonNode jsonNode = mapper.readValue(response.getContentAsString(), JsonNode.class);
    return jsonNode.get("_links");
  }

  @Test
  public void shouldSetPullRequestToStatusRejected() throws URISyntaxException {
    Subject subject = mock(Subject.class, RETURNS_DEEP_STUBS);
    shiroRule.setSubject(subject);
    User currentUser = new User("currentUser");
    when(subject.getPrincipals().oneByType(User.class)).thenReturn(currentUser);

    when(store.get("1")).thenReturn(createPullRequest("opened_1", PullRequestStatus.OPEN));
    when(branchResolver.resolve(any(), any())).thenReturn(Branch.normalBranch("master", "123"));
    MockHttpRequest request = MockHttpRequest
      .post("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/ns/repo/1/reject");
    dispatcher.invoke(request, response);
    assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_NO_CONTENT);
    verify(store).update(argThat(pullRequest -> {
      assertThat(pullRequest.getStatus()).isEqualTo(REJECTED);
      return true;
    }));
  }

  @Test
  public void shouldSetPullRequestToStatusRejectedWithMessage() throws URISyntaxException {
    Subject subject = mock(Subject.class, RETURNS_DEEP_STUBS);
    shiroRule.setSubject(subject);
    User currentUser = new User("currentUser");
    when(subject.getPrincipals().oneByType(User.class)).thenReturn(currentUser);

    when(store.get("1")).thenReturn(createPullRequest("opened_1", PullRequestStatus.OPEN));
    when(branchResolver.resolve(any(), any())).thenReturn(Branch.normalBranch("master", "123"));
    JsonMockHttpRequest request = JsonMockHttpRequest
      .post("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/ns/repo/1/rejectWithMessage")
      .json("{'message':'boring'}");
    dispatcher.invoke(request, response);
    assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_NO_CONTENT);
    verify(store).update(argThat(pullRequest -> {
      assertThat(pullRequest.getStatus()).isEqualTo(REJECTED);
      return true;
    }));
    verify(eventBus).post(argThat(event -> {
      assertThat(event)
        .isInstanceOf(PullRequestRejectedEvent.class)
        .extracting("message")
        .isEqualTo("boring");
      return true;
    }));
  }

  @Test
  @SubjectAware(username = "slarti")
  public void shouldRejectChangingStatusOfMergedPullRequest() throws URISyntaxException {
    when(store.get("1")).thenReturn(createPullRequest("opened_1", PullRequestStatus.MERGED));
    MockHttpRequest request = MockHttpRequest
      .post("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/ns/repo/1/reject");
    dispatcher.invoke(request, response);
    assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_BAD_REQUEST);
    verify(store, never()).update(any());
  }

  @Test
  @SubjectAware(username = "dent")
  public void shouldApprove() throws URISyntaxException {
    initPullRequestRootResource();

    MockHttpRequest request = MockHttpRequest
      .post("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/ns/repo/1/approve");
    dispatcher.invoke(request, response);
    verify(pullRequestService).approve(new NamespaceAndName("ns", "repo"), "1");
    assertThat(response.getStatus()).isEqualTo(204);
  }


  @Test
  @SubjectAware(username = "dent")
  public void shouldDisapprove() throws URISyntaxException {
    initPullRequestRootResource();

    MockHttpRequest request = MockHttpRequest
      .post("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/ns/repo/1/disapprove");
    dispatcher.invoke(request, response);
    verify(pullRequestService).disapprove(any(), any());
    assertThat(response.getStatus()).isEqualTo(204);
  }

  @Test
  @SubjectAware(username = "dent")
  public void shouldMarkAsReviewed() throws URISyntaxException {
    initPullRequestRootResource();

    MockHttpRequest request = MockHttpRequest
      .post("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/ns/repo/1/review-mark/some/file");
    dispatcher.invoke(request, response);
    verify(pullRequestService).markAsReviewed(repository, "1", "some/file");
    assertThat(response.getStatus()).isEqualTo(204);
  }

  @Test
  @SubjectAware(username = "dent")
  public void shouldMarkAsNotReviewed() throws URISyntaxException {
    initPullRequestRootResource();

    MockHttpRequest request = MockHttpRequest
      .delete("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/ns/repo/1/review-mark/some/file");
    dispatcher.invoke(request, response);
    verify(pullRequestService).markAsNotReviewed(repository, "1", "some/file");
    assertThat(response.getStatus()).isEqualTo(204);
  }

  @Test
  @SubjectAware(username = "dent")
  public void shouldGetMarkAsReviewedLink() throws URISyntaxException, IOException {
    PullRequest pullRequest = createPullRequest();

    when(store.get("1")).thenReturn(pullRequest);

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
    pullRequest.setReviewMarks(ImmutableSet.of(
      new ReviewMark("/some/file", "dent"),
      new ReviewMark("/some/other/file", "trillian")
    ));

    when(store.get("1")).thenReturn(pullRequest);

    MockHttpRequest request = MockHttpRequest
      .get("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/ns/repo/1");
    dispatcher.invoke(request, response);
    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getContentAsString()).contains("\"markedAsReviewed\":[\"/some/file\"]");
  }

  @Test
  @SubjectAware(username = "dent")
  public void shouldReturnPullRequestIsValidResultForNewPullRequest() throws URISyntaxException, IOException {
    mockLoggedInUser(new User("dent"));
    mockLogCommandForPullRequestCheck(ImmutableList.of(new Changeset()));

    PullRequest pullRequest = createPullRequest();
    when(store.getAll()).thenReturn(ImmutableList.of(pullRequest));

    MockHttpRequest request = MockHttpRequest
      .get("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/ns/repo/check?source=feature&target=master");

    dispatcher.invoke(request, response);
    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getContentAsString()).contains("\"status\":\"PR_VALID\"");
    assertThat(response.getContentAsString()).contains("\"_links\":{\"self\":{\"href\":\"/v2/pull-requests/ns/repo/check?source=feature&target=master\"}}");
  }

  @Test
  @SubjectAware(username = "dent")
  public void shouldReturnPullRequestIsValidResultForExistingPullRequest() throws URISyntaxException, IOException {
    mockLoggedInUser(new User("dent"));
    mockLogCommandForPullRequestCheck(ImmutableList.of(new Changeset()));

    PullRequest pullRequest = createPullRequest();
    when(store.get(pullRequest.getId())).thenReturn(pullRequest);

    MockHttpRequest request = MockHttpRequest
      .get("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/ns/repo/id/check?target=master");

    dispatcher.invoke(request, response);
    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getContentAsJson().get("status").asText()).isEqualTo("PR_VALID");
    assertThat(response.getContentAsJson().get("_links").get("self").get("href").asText()).isEqualTo("/v2/pull-requests/ns/repo/id/check?target=master");
  }

  @Test
  @SubjectAware(username = "dent")
  public void shouldReturnBranchesNotDifferResultIfSameBranches() throws URISyntaxException, IOException {
    mockLoggedInUser(new User("dent"));
    mockLogCommandForPullRequestCheck(ImmutableList.of(new Changeset()));

    MockHttpRequest request = MockHttpRequest
      .get("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/ns/repo/check?source=master&target=master");

    dispatcher.invoke(request, response);
    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getContentAsString()).contains("\"status\":\"SAME_BRANCHES\"");
    assertThat(response.getContentAsString()).contains("\"_links\":{\"self\":{\"href\":\"/v2/pull-requests/ns/repo/check?source=master&target=master\"}}");
  }

  @Test
  @SubjectAware(username = "dent")
  public void shouldReturnBranchesNotDifferResultIfNoChangesetsInDiff() throws URISyntaxException, IOException {
    mockLoggedInUser(new User("dent"));
    mockLogCommandForPullRequestCheck(emptyList());

    MockHttpRequest request = MockHttpRequest
      .get("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/ns/repo/check?source=feature&target=master");

    dispatcher.invoke(request, response);
    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getContentAsString()).contains("\"status\":\"BRANCHES_NOT_DIFFER\"");
    assertThat(response.getContentAsString()).contains("\"_links\":{\"self\":{\"href\":\"/v2/pull-requests/ns/repo/check?source=feature&target=master\"}}");
  }

  @Test
  @SubjectAware(username = "dent")
  public void shouldReturnAlreadyExistsResult() throws URISyntaxException, IOException {
    mockLoggedInUser(new User("dent"));
    mockLogCommandForPullRequestCheck(ImmutableList.of(new Changeset()));
    PullRequest pullRequest = createPullRequest();
    when(store.getAll()).thenReturn(ImmutableList.of(pullRequest));

    MockHttpRequest request = MockHttpRequest
      .get("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/ns/repo/check?source=develop&target=master");

    dispatcher.invoke(request, response);
    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getContentAsString()).contains("\"status\":\"PR_ALREADY_EXISTS\"");
    assertThat(response.getContentAsString()).contains("\"_links\":{\"self\":{\"href\":\"/v2/pull-requests/ns/repo/check?source=develop&target=master\"}}");
  }

  @Test
  @SubjectAware(username = "dent")
  public void shouldReturnIllegalRequestIfSourceBranchIsEmpty() throws URISyntaxException {
    PullRequest pullRequest = createPullRequest();
    pullRequest.setSource("");

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
    config.setDefaultReviewers(singletonList("dent"));
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
    when(logCommandBuilder.getChangesets()).thenReturn(new ChangesetPagingResult(0, singletonList(new Changeset("hashtag", Instant.now().getEpochSecond(), new Person("Test"), "Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet.\nMore Text"))));

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
    when(logCommandBuilder.getChangesets()).thenReturn(new ChangesetPagingResult(0, asList(
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
      .get("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/namespace/name/1/changes");

    dispatcher.invoke(request, response);
    assertThat(response.getStatus()).isEqualTo(403);
  }

  @Test
  @SubjectAware(username = "dent")
  public void shouldGetHistoryOfPullRequest() throws URISyntaxException, IOException {
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

    when(pullRequestChangeService.getAllChangesOfPullRequest(any(NamespaceAndName.class), eq("1"))).thenReturn(changes);

    MockHttpRequest request = MockHttpRequest
      .get("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/namespace/name/1/changes");

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

  private void mockLogCommandForPullRequestCheck(List<Changeset> changesets) throws IOException {
    when(repositoryServiceFactory.create(repository)).thenReturn(repositoryService);
    when(repositoryService.getLogCommand()).thenReturn(logCommandBuilder);
    when(logCommandBuilder.getChangesets()).thenReturn(new ChangesetPagingResult(0, changesets));
  }

  private void mockLoggedInUser(User user1) {
    Subject subject = mock(Subject.class);
    ThreadContext.bind(subject);

    PrincipalCollection principals = mock(PrincipalCollection.class);
    when(subject.getPrincipals()).thenReturn(principals);
    when(subject.isPermitted(any(String.class))).thenReturn(true);
    when(principals.oneByType(User.class)).thenReturn(user1);
    when(principals.getPrimaryPrincipal()).thenReturn(user1.getId());
  }

  private void initPullRequestRootResource() {
    PullRequestRootResource rootResource =
      new PullRequestRootResource(mapper, pullRequestService, commentService, repositoryServiceFactory, Providers.of(new PullRequestResource(mapper, pullRequestService, null, null, channelRegistry, pullRequestChangeService)), configService, userDisplayManager);

    dispatcher = new RestDispatcher();
    dispatcher.addSingletonResource(rootResource);
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

  private void mockChangesets(String sourceBranch, String targetBranch, Changeset... changesets) throws IOException {
    when(repositoryService.getLogCommand()).thenReturn(logCommandBuilder);
    LogCommandBuilder subLogCommandBuilder = mock(LogCommandBuilder.class);
    when(logCommandBuilder.setStartChangeset(sourceBranch)).thenReturn(subLogCommandBuilder);
    when(subLogCommandBuilder.setAncestorChangeset(targetBranch)).thenReturn(subLogCommandBuilder);
    when(subLogCommandBuilder.setPagingLimit(1)).thenReturn(subLogCommandBuilder);
    when(subLogCommandBuilder.getChangesets()).thenReturn(new ChangesetPagingResult(changesets.length, asList(changesets)));
  }
}
