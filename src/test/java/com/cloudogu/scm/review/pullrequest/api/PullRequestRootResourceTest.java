package com.cloudogu.scm.review.pullrequest.api;

import com.cloudogu.scm.review.BranchResolver;
import com.cloudogu.scm.review.ExceptionMessageMapper;
import com.cloudogu.scm.review.RepositoryResolver;
import com.cloudogu.scm.review.comment.service.CommentService;
import com.cloudogu.scm.review.pullrequest.dto.PullRequestMapper;
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
import org.assertj.core.util.Lists;
import org.jboss.resteasy.core.Dispatcher;
import org.jboss.resteasy.mock.MockDispatcherFactory;
import org.jboss.resteasy.mock.MockHttpRequest;
import org.jboss.resteasy.mock.MockHttpResponse;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import sonia.scm.NotFoundException;
import sonia.scm.repository.NamespaceAndName;
import sonia.scm.repository.Repository;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
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
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SubjectAware(configuration = "classpath:com/cloudogu/scm/review/shiro.ini")
public class PullRequestRootResourceTest {

  @Rule
  public final ShiroRule shiroRule = new ShiroRule();

  private final RepositoryResolver repositoryResolver = mock(RepositoryResolver.class);
  private final BranchResolver branchResolver = mock(BranchResolver.class);
  private final PullRequestStoreFactory storeFactory = mock(PullRequestStoreFactory.class);
  private final PullRequestStore store = mock(PullRequestStore.class);
  private final Repository repository = mock(Repository.class);
  private final UriInfo uriInfo = mock(UriInfo.class);
  private final ArgumentCaptor<PullRequest> pullRequestStoreCaptor = ArgumentCaptor.forClass(PullRequest.class);

  private Dispatcher dispatcher;

  private final MockHttpResponse response = new MockHttpResponse();

  private PullRequestRootResource pullRequestRootResource;
  private static final String REPOSITORY_NAME = "repo";
  private static final String REPOSITORY_ID = "repo_ID";
  private static final String REPOSITORY_NAMESPACE = "ns";
  private PullRequestMapper mapper = new PullRequestMapperImpl();

  private CommentService commentService = mock(CommentService.class);

  @Before
  public void init() {
    when(repository.getId()).thenReturn(REPOSITORY_ID);
    when(repository.getName()).thenReturn(REPOSITORY_NAME);
    when(repository.getNamespace()).thenReturn(REPOSITORY_NAMESPACE);
    when(repository.getNamespaceAndName()).thenReturn(new NamespaceAndName(REPOSITORY_NAMESPACE, REPOSITORY_NAME));
    when(repositoryResolver.resolve(any())).thenReturn(repository);
    DefaultPullRequestService service = new DefaultPullRequestService(repositoryResolver, branchResolver, storeFactory);
    pullRequestRootResource = new PullRequestRootResource(mapper, service, Providers.of(new PullRequestResource(mapper, service, null, commentService)));
    when(uriInfo.getAbsolutePathBuilder()).thenReturn(UriBuilder.fromPath("/scm"));
    when(storeFactory.create(null)).thenReturn(store);
    when(storeFactory.create(any())).thenReturn(store);
    when(store.add(pullRequestStoreCaptor.capture())).thenReturn("1");
    dispatcher = MockDispatcherFactory.createDispatcher();
    dispatcher.getProviderFactory().register(new ExceptionMessageMapper());
    dispatcher.getRegistry().addSingletonResource(pullRequestRootResource);
  }

  @Test
  @SubjectAware(username = "slarti", password = "secret")
  public void shouldCreateNewValidPullRequest() throws URISyntaxException, IOException {
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
    assertThat(pullRequest.getAuthor()).isEqualTo("slarti");
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
    initRepoWithPRs("ns", "repo");
    MockHttpRequest request = MockHttpRequest.get("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/ns/repo");
    dispatcher.invoke(request, response);
    assertThat(response.getStatus()).isEqualTo(200);
    ObjectMapper mapper = new ObjectMapper();
    JsonNode jsonNode = mapper.readValue(response.getContentAsString(), JsonNode.class);
    JsonNode prNode = jsonNode.get("_embedded").get("pullRequests");
    prNode.elements().forEachRemaining(node -> {
      String actual = node.path("_links").path("createComment").path("href").asText();
      assertThat(actual).isEqualTo("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/ns/repo/" + node.get("id").asText() + "/comments/");
      actual = node.path("_links").path("comments").path("href").asText();
      assertThat(actual).isEqualTo("/" + PullRequestRootResource.PULL_REQUESTS_PATH_V2 + "/ns/repo/" + node.get("id").asText() + "/comments/");
    });
  }

  @Test
  @SubjectAware(username = "slarti", password = "secret")
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
  public void shouldFailOnUpdatingNonExistingPullRequest() throws URISyntaxException, UnsupportedEncodingException {
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
  public void shouldFailUpdatingOnMissingModifyPushPermission() throws URISyntaxException, UnsupportedEncodingException {
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

  private void initRepoWithPRs(String namespace, String name) {
    when(repository.getNamespace()).thenReturn(namespace);
    when(repository.getName()).thenReturn(name);
    when(repositoryResolver.resolve(new NamespaceAndName("foo", "bar"))).thenReturn(repository);
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
}
