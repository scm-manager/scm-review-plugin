package com.cloudogu.scm.review.pullrequest.api;

import com.cloudogu.scm.review.BranchResolver;
import com.cloudogu.scm.review.ExceptionMessageMapper;
import com.cloudogu.scm.review.RepositoryResolver;
import com.cloudogu.scm.review.comment.service.CommentService;
import com.cloudogu.scm.review.pullrequest.dto.BranchRevisionResolver;
import com.cloudogu.scm.review.pullrequest.dto.PullRequestMapperImpl;
import com.cloudogu.scm.review.pullrequest.dto.PullRequestStatusDto;
import com.cloudogu.scm.review.pullrequest.service.DefaultPullRequestService;
import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import com.cloudogu.scm.review.pullrequest.service.PullRequestStatus;
import com.cloudogu.scm.review.pullrequest.service.PullRequestStore;
import com.cloudogu.scm.review.pullrequest.service.PullRequestStoreFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.sdorra.shiro.ShiroRule;
import com.github.sdorra.shiro.SubjectAware;
import com.google.common.io.Resources;
import com.google.inject.util.Providers;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.assertj.core.util.Lists;
import org.jboss.resteasy.core.Dispatcher;
import org.jboss.resteasy.mock.MockDispatcherFactory;
import org.jboss.resteasy.mock.MockHttpRequest;
import org.jboss.resteasy.mock.MockHttpResponse;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import sonia.scm.NotFoundException;
import sonia.scm.event.ScmEventBus;
import sonia.scm.repository.Changeset;
import sonia.scm.repository.ChangesetPagingResult;
import sonia.scm.repository.NamespaceAndName;
import sonia.scm.repository.Repository;
import sonia.scm.repository.api.LogCommandBuilder;
import sonia.scm.repository.api.RepositoryService;
import sonia.scm.repository.api.RepositoryServiceFactory;
import sonia.scm.user.User;
import sonia.scm.user.UserDisplayManager;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static com.cloudogu.scm.review.ExceptionMessageMapper.assertExceptionFrom;
import static com.cloudogu.scm.review.TestData.createPullRequest;
import static com.cloudogu.scm.review.pullrequest.service.PullRequestStatus.REJECTED;
import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
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
  private final ArgumentCaptor<PullRequest> pullRequestStoreCaptor = ArgumentCaptor.forClass(PullRequest.class);

  private Dispatcher dispatcher;

  private final MockHttpResponse response = new MockHttpResponse();
  private final Subject subject = mock(Subject.class);

  private PullRequestRootResource pullRequestRootResource;
  private static final String REPOSITORY_NAME = "repo";
  private static final String REPOSITORY_ID = "repo_ID";
  private static final String REPOSITORY_NAMESPACE = "ns";

  @Mock
  private UserDisplayManager userDisplayManager;

  @Mock
  private RepositoryServiceFactory repositoryServiceFactory;
  @Mock
  private RepositoryService repositoryService;
  @Mock
  private BranchRevisionResolver branchRevisionResolver;
  @Mock
  private LogCommandBuilder logCommandBuilder;

  @InjectMocks
  private PullRequestMapperImpl mapper;

  private ScmEventBus eventBus = mock(ScmEventBus.class) ;

  private CommentService commentService = mock(CommentService.class);

  @Before
  public void init() {
    when(repository.getId()).thenReturn(REPOSITORY_ID);
    when(repository.getName()).thenReturn(REPOSITORY_NAME);
    when(repository.getNamespace()).thenReturn(REPOSITORY_NAMESPACE);
    when(repository.getNamespaceAndName()).thenReturn(new NamespaceAndName(REPOSITORY_NAMESPACE, REPOSITORY_NAME));
    when(repositoryResolver.resolve(any())).thenReturn(repository);
    DefaultPullRequestService service = new DefaultPullRequestService(repositoryResolver, branchResolver, storeFactory, eventBus, repositoryServiceFactory);
    pullRequestRootResource = new PullRequestRootResource(mapper, service, Providers.of(new PullRequestResource(mapper, service, null, commentService)));
    when(storeFactory.create(null)).thenReturn(store);
    when(storeFactory.create(any())).thenReturn(store);
    when(store.add(pullRequestStoreCaptor.capture())).thenReturn("1");
    dispatcher = MockDispatcherFactory.createDispatcher();
    dispatcher.getProviderFactory().register(new ExceptionMessageMapper());
    dispatcher.getRegistry().addSingletonResource(pullRequestRootResource);
    lenient().when(repositoryServiceFactory.create(any(Repository.class))).thenReturn(repositoryService);
  }

  @Test
  @SubjectAware(username = "slarti", password = "secret")
  public void shouldCreateNewValidPullRequest() throws URISyntaxException, IOException {
    mockPrincipal();
    mockChangesets("sourceBranch", "targetBranch", new Changeset());

    byte[] pullRequestJson = loadJson("com/cloudogu/scm/review/pullRequest.json");
    MockHttpRequest request =
      MockHttpRequest
        .post("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/space/name")
        .content(pullRequestJson)
        .contentType(MediaType.APPLICATION_JSON);

    dispatcher.invoke(request, response);

    assertEquals(HttpServletResponse.SC_CREATED, response.getStatus());
    assertThat(response.getOutputHeaders().getFirst("Location").toString()).isEqualTo("/v2/pull-requests/space/name/1");
    PullRequest pullRequest = pullRequestStoreCaptor.getValue();
    assertThat(pullRequest.getAuthor()).isEqualTo("user1");
  }

  @Test
  @SubjectAware(username = "trillian", password = "secret")
  public void shouldGetUnauthorizedExceptionWhenMissingPermissionOnGetPR() throws URISyntaxException, UnsupportedEncodingException {
    MockHttpRequest request = MockHttpRequest.get("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/" + REPOSITORY_NAMESPACE + "/" + REPOSITORY_NAME + "/123");
    dispatcher.invoke(request, response);

    assertEquals(HttpServletResponse.SC_FORBIDDEN, response.getStatus());
  }

  @Test
  @SubjectAware(username = "trillian", password = "secret")
  public void shouldGetUnauthorizedExceptionWhenMissingPermissionOnGetAllPR() throws URISyntaxException, UnsupportedEncodingException {
    MockHttpRequest request = MockHttpRequest.get("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/" + REPOSITORY_NAMESPACE + "/" + REPOSITORY_NAME + "");
    dispatcher.invoke(request, response);

    assertEquals(HttpServletResponse.SC_FORBIDDEN, response.getStatus());
  }

  @Test
  @SubjectAware(username = "trillian", password = "secret")
  public void shouldGetUnauthorizedExceptionWhenMissingPermissionOnCreatePR() throws URISyntaxException, IOException {
    byte[] pullRequestJson = loadJson("com/cloudogu/scm/review/pullRequest.json");
    MockHttpRequest request = MockHttpRequest.post("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/space/name")
      .content(pullRequestJson)
      .contentType(MediaType.APPLICATION_JSON);
    dispatcher.invoke(request, response);

    assertEquals(HttpServletResponse.SC_FORBIDDEN, response.getStatus());
  }

  @Test
  @SubjectAware(username = "slarti", password = "secret")
  public void shouldGetAlreadyExistsExceptionOnCreatePR() throws URISyntaxException, IOException {
    PullRequest pr = new PullRequest();
    pr.setStatus(PullRequestStatus.OPEN);
    pr.setSource("source");
    pr.setTarget("target");
    pr.setTitle("title");
    pr.setId("id");
    when(store.getAll()).thenReturn(Lists.newArrayList(pr));

    byte[] pullRequestJson = "{\"source\": \"source\", \"target\": \"target\", \"title\": \"pull request\"}".getBytes();
    when(repositoryResolver.resolve(new NamespaceAndName(REPOSITORY_NAMESPACE, REPOSITORY_NAME))).thenReturn(repository);
    MockHttpRequest request = MockHttpRequest
      .post("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/" + REPOSITORY_NAMESPACE + "/" + REPOSITORY_NAME)
      .content(pullRequestJson)
      .contentType(MediaType.APPLICATION_JSON);
    dispatcher.invoke(request, response);
    assertExceptionFrom(response).hasMessageMatching("pull request with id id in repository with id .* already exists");
  }

  @Test
  @SubjectAware(username = "trillian", password = "secret")
  public void shouldRejectInvalidPullRequest() throws URISyntaxException, IOException {
    byte[] pullRequestJson = loadJson("com/cloudogu/scm/review/pullRequest_invalid.json");
    MockHttpRequest request =
      MockHttpRequest
        .post("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/space/name")
        .content(pullRequestJson)
        .contentType(MediaType.APPLICATION_JSON);

    dispatcher.invoke(request, response);

    assertEquals(HttpServletResponse.SC_BAD_REQUEST, response.getStatus());
    verify(store, never()).add(any());
  }

  @Test
  @SubjectAware(username = "slarti", password = "secret")
  public void shouldRejectSameBranches() throws URISyntaxException, IOException {
    byte[] pullRequestJson = loadJson("com/cloudogu/scm/review/pullRequest_sameBranches.json");
    MockHttpRequest request =
      MockHttpRequest
        .post("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/space/name")
        .content(pullRequestJson)
        .contentType(MediaType.APPLICATION_JSON);

    dispatcher.invoke(request, response);

    assertEquals(HttpServletResponse.SC_BAD_REQUEST, response.getStatus());
    verify(store, never()).add(any());
  }

  @Test
  @SubjectAware(username = "slarti", password = "secret")
  public void shouldRejectWithoutDiff() throws URISyntaxException, IOException {
    mockPrincipal();
    mockChangesets("sourceBranch", "targetBranch");

    byte[] pullRequestJson = loadJson("com/cloudogu/scm/review/pullRequest.json");
    MockHttpRequest request =
      MockHttpRequest
        .post("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/space/name")
        .content(pullRequestJson)
        .contentType(MediaType.APPLICATION_JSON);

    dispatcher.invoke(request, response);

    assertEquals(HttpServletResponse.SC_BAD_REQUEST, response.getStatus());
    verify(store, never()).add(any());
  }

  @Test
  @SubjectAware(username = "trillian", password = "secret")
  public void shouldHandleMissingRepository() throws URISyntaxException, IOException {
    when(repositoryResolver.resolve(new NamespaceAndName("space", "X")))
      .thenThrow(new NotFoundException("x", "y"));
    byte[] pullRequestJson = loadJson("com/cloudogu/scm/review/pullRequest.json");
    MockHttpRequest request =
      MockHttpRequest
        .post("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/space/X")
        .content(pullRequestJson)
        .contentType(MediaType.APPLICATION_JSON);

    dispatcher.invoke(request, response);

    assertExceptionFrom(response)
      .isOffClass(NotFoundException.class)
      .hasMessageMatching("could not find.*");
  }

  @Test
  @SubjectAware(username = "slarti", password = "secret")
  public void shouldHandleMissingBranch() throws URISyntaxException, IOException {
    when(branchResolver.resolve(repository, "sourceBranch")).thenThrow(new NotFoundException("x", "y"));
    byte[] pullRequestJson = loadJson("com/cloudogu/scm/review/pullRequest.json");
    MockHttpRequest request =
      MockHttpRequest
        .post("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/" + REPOSITORY_NAMESPACE + "/" + REPOSITORY_NAME)
        .content(pullRequestJson)
        .contentType(MediaType.APPLICATION_JSON);

    dispatcher.invoke(request, response);

    assertExceptionFrom(response)
      .isOffClass(NotFoundException.class)
      .hasMessageMatching("could not find.*");
  }

  @Test
  @SubjectAware(username = "rr", password = "secret")
  public void shouldGetPullRequest() throws URISyntaxException, UnsupportedEncodingException {
    when(repositoryResolver.resolve(new NamespaceAndName(REPOSITORY_NAMESPACE, REPOSITORY_NAME))).thenReturn(repository);
    PullRequest pullRequest = createPullRequest();
    when(store.get("123")).thenReturn(pullRequest);
    MockHttpRequest request = MockHttpRequest.get("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/" + REPOSITORY_NAMESPACE + "/" + REPOSITORY_NAME + "/123");
    dispatcher.invoke(request, response);
    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getContentAsString()).contains("_links");
  }


  @Test
  @SubjectAware(username = "rr", password = "secret")
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
  }

  @Test
  @SubjectAware(username = "rr", password = "secret")
  public void shouldGetAllPullRequests() throws URISyntaxException, UnsupportedEncodingException {
    when(repositoryResolver.resolve(new NamespaceAndName(REPOSITORY_NAMESPACE, REPOSITORY_NAME))).thenReturn(repository);
    String id_1 = "id_1";
    String id_2 = "ABC ID 2";
    List<PullRequest> pullRequests = Lists.newArrayList(createPullRequest(id_1), createPullRequest(id_2));
    when(store.getAll()).thenReturn(pullRequests);

    // request all PRs without filter
    MockHttpRequest request = MockHttpRequest.get("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/" + REPOSITORY_NAMESPACE + "/" + REPOSITORY_NAME + "");
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
  @SubjectAware(username = "rr", password = "secret")
  public void shouldGetOpenedPullRequests() throws URISyntaxException, IOException {
    verifyFilteredPullRequests(PullRequestStatusDto.OPEN.name());
  }

  @Test
  @SubjectAware(username = "rr", password = "secret")
  public void shouldGetRejectedPullRequests() throws URISyntaxException, IOException {
    verifyFilteredPullRequests(PullRequestStatusDto.REJECTED.name());
  }

  @Test
  @SubjectAware(username = "rr", password = "secret")
  public void shouldGetMergedPullRequests() throws URISyntaxException, IOException {
    verifyFilteredPullRequests(PullRequestStatusDto.MERGED.name());
  }

  @Test
  @SubjectAware(username = "rr", password = "secret")
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
  @SubjectAware(username = "rr", password = "secret")
  public void shouldNotGetUpdateLinkForUserWithoutPushPermission() throws URISyntaxException, IOException {
    initRepoWithPRs("ns", "repo");
    MockHttpRequest request = MockHttpRequest.get("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/ns/repo");
    dispatcher.invoke(request, response);
    assertThat(response.getStatus()).isEqualTo(200);
    ObjectMapper mapper = new ObjectMapper();
    JsonNode jsonNode = mapper.readValue(response.getContentAsString(), JsonNode.class);
    JsonNode prNode = jsonNode.get("_embedded").get("pullRequests");
    prNode.elements().forEachRemaining(node -> {
      assertThat(node.path("_links").get("update")).isNull();
    });
  }

  @Test
  @SubjectAware(username = "slarti", password = "secret")
  public void shouldGetCommentLink() throws URISyntaxException, IOException {
    when(branchRevisionResolver.getRevisions(any(), any())).thenReturn(new BranchRevisionResolver.RevisionResult("developId", "masterId"));
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
  @SubjectAware(username = "slarti", password = "secret")
  public void shouldGetUpdateLink() throws URISyntaxException, IOException {
    when(branchRevisionResolver.getRevisions(any(), any())).thenReturn(new BranchRevisionResolver.RevisionResult("developId", "masterId"));
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
  @SubjectAware(username = "slarti", password = "secret")
  public void shouldUpdatePullRequestSuccessfully() throws URISyntaxException {
    PullRequest existingPullRequest = new PullRequest();
    existingPullRequest.setAuthor("somebody");
    when(store.get("1")).thenReturn(existingPullRequest);

    MockHttpRequest request = MockHttpRequest
      .put("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/ns/repo/1")
      .content("{\"title\": \"new Title\", \"description\": \"new description\"}".getBytes())
      .contentType(MediaType.APPLICATION_JSON);
    dispatcher.invoke(request, response);

    assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_NO_CONTENT);
    verify(store).update(argThat(pullRequest -> {
      assertThat(pullRequest.getTitle()).isEqualTo("new Title");
      assertThat(pullRequest.getDescription()).isEqualTo("new description");
      return true;
    }));
  }

  @Test
  @SubjectAware(username = "slarti", password = "secret")
  public void shouldFailOnUpdatingNonExistingPullRequest() throws URISyntaxException, IOException {
    initRepoWithPRs("ns", "repo");
    when(store.get("opened_1")).thenThrow(new NotFoundException("x", "y"));
    MockHttpRequest request = MockHttpRequest
      .put("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/ns/repo/opened_1")
      .content("{\"title\": \"new Title\", \"description\": \"new description\"}".getBytes())
      .contentType(MediaType.APPLICATION_JSON);

    dispatcher.invoke(request, response);

    assertExceptionFrom(response)
      .isOffClass(NotFoundException.class)
      .hasMessageMatching("could not find.*");
    verify(store, never()).update(any());
  }

  @Test
  @SubjectAware(username = "rr", password = "secret")
  public void shouldFailUpdatingOnMissingModifyPushPermission() throws URISyntaxException {
    MockHttpRequest request = MockHttpRequest
      .put("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/ns/repo/1")
      .content("{\"title\": \"new Title\", \"description\": \"new description\"}".getBytes())
      .contentType(MediaType.APPLICATION_JSON);
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
//    Subject subject = mock(Subject.class);
    ThreadContext.bind(subject);

    PrincipalCollection principals = mock(PrincipalCollection.class);
    when(subject.getPrincipals()).thenReturn(principals);
    when(subject.isPermitted(any(String.class))).thenReturn(true);
    String currentUser = "username";
    User user1 = new User();
    user1.setName("user1");
    user1.setMail("user1@mail.de");
    user1.setDisplayName("User 1");
    when(principals.oneByType(User.class)).thenReturn(user1);

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
  public void shouldNotReturnAnySubscriptionLinkOnMissingUserMail() throws URISyntaxException, IOException {
    // the PR has no subscriber
    PullRequest pullRequest = createPullRequest();

    Subject subject = mock(Subject.class);
    ThreadContext.bind(subject);

    PrincipalCollection principals = mock(PrincipalCollection.class);
    when(subject.getPrincipals()).thenReturn(principals);
    when(subject.isPermitted(any(String.class))).thenReturn(true);
    String currentUser = "username";
    User user1 = new User();
    user1.setName("user1");
    user1.setDisplayName("User 1");
    when(principals.oneByType(User.class)).thenReturn(user1);

    MockHttpRequest request = MockHttpRequest
      .get("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/ns/repo/1/subscription");
    dispatcher.invoke(request, response);
    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getContentAsString()).isNullOrEmpty();
  }

  @Test
  public void shouldGetTheUnsubscribeLink() throws URISyntaxException, IOException {
    // the PR has no subscriber
    PullRequest pullRequest = createPullRequest();
    pullRequest.setSubscriber(singleton("user1"));

    when(store.get("1")).thenReturn(pullRequest);
    Subject subject = mock(Subject.class);
    ThreadContext.bind(subject);

    PrincipalCollection principals = mock(PrincipalCollection.class);
    when(subject.getPrincipals()).thenReturn(principals);
    when(subject.isPermitted(any(String.class))).thenReturn(true);
    User user1 = new User();
    user1.setName("user1");
    user1.setMail("email@d.de");
    user1.setDisplayName("User 1");
    when(principals.oneByType(User.class)).thenReturn(user1);

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

  private void verifyFilteredPullRequests(String status) throws URISyntaxException, IOException {
    initRepoWithPRs("ns", "repo");
    MockHttpRequest request = MockHttpRequest.get("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/ns/repo?status=" + status);
    dispatcher.invoke(request, response);
    assertThat(response.getStatus()).isEqualTo(200);
    ObjectMapper mapper = new ObjectMapper();
    JsonNode jsonNode = mapper.readValue(response.getContentAsString(), JsonNode.class);
    JsonNode prNode = jsonNode.get("_embedded").get("pullRequests");
    prNode.elements().forEachRemaining(node -> assertThat(node.get("status").asText()).isEqualTo(status));
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
    when(store.getAll()).thenReturn(Lists.newArrayList(openedPR1, openedPR2, rejectedPR1, rejectedPR2, mergedPR1, mergedPR2));
  }

  private byte[] loadJson(String s) throws IOException {
    URL url = Resources.getResource(s);
    return Resources.toByteArray(url);
  }

  @Test
  @SubjectAware(username = "rr", password = "secret")
  public void shouldReturnCollectionWithOnlySelfLink() throws URISyntaxException, IOException {
    JsonNode links = invokeAndReturnLinks();

    assertThat(links.has("self")).isTrue();
    assertThat(links.has("create")).isFalse();
  }

  @Test
  @SubjectAware(username = "slarti", password = "secret")
  public void shouldReturnCollectionWithCreateLink() throws URISyntaxException, IOException {
    when(branchRevisionResolver.getRevisions(any(), any())).thenReturn(new BranchRevisionResolver.RevisionResult("developId", "masterId"));

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
  @SubjectAware(username = "slarti", password = "secret")
  public void shouldSetPullRequestToStatusRejected() throws URISyntaxException {
    when(store.get("1")).thenReturn(createPullRequest("opened_1", PullRequestStatus.OPEN));
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
  @SubjectAware(username = "slarti", password = "secret")
  public void shouldRejectChangingStatusOfMergedPullRequest() throws URISyntaxException {
    when(store.get("1")).thenReturn(createPullRequest("opened_1", PullRequestStatus.MERGED));
    MockHttpRequest request = MockHttpRequest
      .post("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/ns/repo/1/reject");
    dispatcher.invoke(request, response);
    assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_BAD_REQUEST);
    verify(store, never()).update(any());
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
    when(subLogCommandBuilder.setPagingStart(0)).thenReturn(subLogCommandBuilder);
    when(subLogCommandBuilder.setPagingLimit(1)).thenReturn(subLogCommandBuilder);
    when(subLogCommandBuilder.getChangesets()).thenReturn(new ChangesetPagingResult(changesets.length, asList(changesets)));
  }
}
