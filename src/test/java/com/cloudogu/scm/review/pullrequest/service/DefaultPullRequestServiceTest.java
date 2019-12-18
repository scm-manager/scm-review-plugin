package com.cloudogu.scm.review.pullrequest.service;

import com.cloudogu.scm.review.RepositoryResolver;
import com.cloudogu.scm.review.StatusChangeNotAllowedException;
import com.github.sdorra.shiro.ShiroRule;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.junit.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import sonia.scm.HandlerEventType;
import sonia.scm.event.ScmEventBus;
import sonia.scm.repository.Changeset;
import sonia.scm.repository.ChangesetPagingResult;
import sonia.scm.repository.Repository;
import sonia.scm.repository.api.LogCommandBuilder;
import sonia.scm.repository.api.RepositoryService;
import sonia.scm.repository.api.RepositoryServiceFactory;
import sonia.scm.user.User;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static com.cloudogu.scm.review.pullrequest.service.PullRequestRejectedEvent.RejectionCause.REJECTED_BY_USER;
import static com.cloudogu.scm.review.pullrequest.service.PullRequestStatus.MERGED;
import static com.cloudogu.scm.review.pullrequest.service.PullRequestStatus.OPEN;
import static com.cloudogu.scm.review.pullrequest.service.PullRequestStatus.REJECTED;
import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultPullRequestServiceTest {

  @Rule
  public ShiroRule shiroRule = new ShiroRule();

  private static final Repository REPOSITORY = new Repository("1", "git", "space", "X");
  private static final User LOGGED_IN_USER = new User("user", "display", "user@example.com");

  @Mock
  RepositoryResolver repositoryResolver;
  @Mock
  PullRequestStoreFactory storeFactory;
  @Mock
  PullRequestStore store;
  @Mock
  ScmEventBus eventBus;
  @Mock
  Subject subject;
  @Mock
  RepositoryServiceFactory repositoryService;

  @Captor
  ArgumentCaptor<Object> eventCaptor;

  @InjectMocks
  DefaultPullRequestService service;

  @BeforeEach
  void initStore() {
    when(storeFactory.create(REPOSITORY)).thenReturn(store);
  }

  @BeforeEach
  void initEventBus() {
    lenient().doNothing().when(eventBus).post(eventCaptor.capture());
  }

  @Nested
  class ForNewPullRequests {

    @BeforeEach
    void setUser() {
      ThreadContext.bind(subject);
      PrincipalCollection principalCollection = mock(PrincipalCollection.class);
      lenient().when(subject.getPrincipals()).thenReturn(principalCollection);
      lenient().when(principalCollection.oneByType(any())).thenReturn(LOGGED_IN_USER);
    }

    @BeforeEach
    void initAddWithNewId() {
      lenient().when(store.add(any())).thenReturn("new_id");
    }

    @BeforeEach
    void mockExistingDiff() throws IOException {
      mockChangesets(new Changeset());
    }

    @Test
    void shouldStoreNewPullRequest() throws NoDifferenceException {
      PullRequest pullRequest = createPullRequest(null, null, null);

      String newId = service.add(REPOSITORY, pullRequest);

      assertThat(newId).isEqualTo("new_id");
      verify(store).add(pullRequest);
    }

    @Test
    void shouldSetCreationDate() throws NoDifferenceException {
      PullRequest pullRequest = createPullRequest(null, null, null);

      service.add(REPOSITORY, pullRequest);

      assertThat(pullRequest.getCreationDate()).isNotNull();
      assertThat(pullRequest.getLastModified()).isNull();
    }

    @Test
    void shouldStoreAuthorAsSubscriber() throws NoDifferenceException {
      PullRequest pullRequest = createPullRequest(null, null, null);

      service.add(REPOSITORY, pullRequest);

      assertThat(pullRequest.getSubscriber())
        .hasSize(1)
        .allMatch(r -> r.equals(LOGGED_IN_USER.getId()));
    }

    @Test
    void shouldStoreReviewerAsSubscriber() throws NoDifferenceException {
      PullRequest pullRequest = createPullRequest(null, null, null);
      pullRequest.setReviewer(singletonMap("reviewer", false));
      service.add(REPOSITORY, pullRequest);

      assertThat(pullRequest.getSubscriber())
        .hasSize(2)
        .anyMatch(r -> r.equals("reviewer"));
    }

    @Test
    void shouldStoreApproverAsReviewer() {
      Subject subject = mock(Subject.class);
      shiroRule.setSubject(subject);

      PrincipalCollection principals = mock(PrincipalCollection.class);
      when(subject.getPrincipals()).thenReturn(principals);
      when(subject.isPermitted(any(String.class))).thenReturn(true);
      User user1 = new User("user", "User", "user@mail.com");
      when(principals.oneByType(User.class)).thenReturn(user1);

      when(repositoryResolver.resolve(any())).thenReturn(REPOSITORY);
      PullRequest pullRequest = createPullRequest("1", null, null);
      when(store.get("1")).thenReturn(pullRequest);

      service.approve(REPOSITORY.getNamespaceAndName(), pullRequest.getId());

      PullRequest storedPullRequest = service.get(REPOSITORY, "1");
      assertThat(storedPullRequest.getReviewer().keySet())
        .hasSize(1)
        .allMatch(r -> r.equals(LOGGED_IN_USER.getId()));
      assertThat(storedPullRequest.getReviewer().values())
        .hasSize(1)
        .allMatch(Boolean::booleanValue);
    }

    @Test
    void shouldSetReviewerFlagFalseOnDisapprove() {
      Subject subject = mock(Subject.class);
      shiroRule.setSubject(subject);

      PrincipalCollection principals = mock(PrincipalCollection.class);
      when(subject.getPrincipals()).thenReturn(principals);
      when(subject.isPermitted(any(String.class))).thenReturn(true);
      User user1 = new User("user", "User", "user@mail.com");
      when(principals.oneByType(User.class)).thenReturn(user1);

      when(repositoryResolver.resolve(any())).thenReturn(REPOSITORY);
      PullRequest pullRequest = createPullRequest("1", null, null);
      Map<String, Boolean> reviewers = new HashMap<>();
      reviewers.put(user1.getId(), Boolean.TRUE);
      pullRequest.setReviewer(reviewers);
      when(store.get("1")).thenReturn(pullRequest);

      service.disapprove(REPOSITORY.getNamespaceAndName(), pullRequest.getId());

      PullRequest storedPullRequest = service.get(REPOSITORY, "1");
      assertThat(storedPullRequest.getReviewer().keySet())
        .hasSize(1)
        .allMatch(r -> r.equals(LOGGED_IN_USER.getId()));
      assertThat(storedPullRequest.getReviewer().values())
        .hasSize(1)
        .noneMatch(Boolean::booleanValue);
    }

    @Test
    void shouldSendEvent() throws NoDifferenceException {
      PullRequest pullRequest = createPullRequest(null, null, null);

      service.add(REPOSITORY, pullRequest);

      assertThat(eventCaptor.getValue())
        .isInstanceOf(PullRequestEvent.class)
        .extracting("item", "oldItem", "eventType")
        .containsExactly(pullRequest, null, HandlerEventType.CREATE);
    }
  }

  @Nested
  @MockitoSettings(strictness = Strictness.LENIENT)
  class ForNewPullRequestsWithoutDiff {

    @BeforeEach
    void mockMissingDiff() throws IOException {
      mockChangesets();
    }

    @Test
    void shouldFail() {
      PullRequest pullRequest = createPullRequest(null, null, null);

      assertThrows(NoDifferenceException.class, () -> service.add(REPOSITORY, pullRequest));

      verify(store, never()).add(pullRequest);
    }
  }

  private void mockChangesets(Changeset... changesets) throws IOException {
    RepositoryService service = mock(RepositoryService.class);
    lenient().when(repositoryService.create(any(Repository.class))).thenReturn(service);
    LogCommandBuilder logCommandBuilder = mock(LogCommandBuilder.class, Mockito.RETURNS_SELF);
    lenient().when(service.getLogCommand()).thenReturn(logCommandBuilder);
    lenient().when(logCommandBuilder.getChangesets()).thenReturn(new ChangesetPagingResult(changesets.length, asList(changesets)));
  }

  @Nested
  class ForChangedPullRequests {

    PullRequest oldPullRequest;

    @BeforeEach
    void mockOldPullRequest() {
      oldPullRequest = createPullRequest("changed", Instant.ofEpochSecond(1_000_000), null);
      oldPullRequest.setSubscriber(singleton("subscriber"));
      oldPullRequest.setReviewer(singletonMap("reviewer", false));
      when(store.get("changed")).thenReturn(oldPullRequest);
    }

    @Test
    void shouldStoreChangedPullRequest() {
      PullRequest pullRequest = createPullRequest("changed", null, null);

      service.update(REPOSITORY, "changed", pullRequest);

      verify(store).update(pullRequest);
    }

    @Test
    void shouldSetLastModifiedDate() {
      PullRequest pullRequest = createPullRequest("changed", Instant.ofEpochSecond(0), Instant.ofEpochMilli(0));

      service.update(REPOSITORY, "changed", pullRequest);

      assertThat(pullRequest.getCreationDate().getEpochSecond()).isEqualTo(1_000_000);
      assertThat(pullRequest.getLastModified()).isNotNull();
      assertThat(pullRequest.getLastModified().getEpochSecond()).isNotEqualTo(0);
    }

    @Test
    void shouldStoreSubscriberFromOldObject() {
      PullRequest pullRequest = createPullRequest("changed", null, null);

      service.update(REPOSITORY, "changed", pullRequest);

      assertThat(pullRequest.getSubscriber())
        .hasSize(1)
        .allMatch(r -> r.equals("subscriber"));

    }

    @Test
    void shouldAddNewReviewerAsSubscriber() {
      PullRequest pullRequest = createPullRequest("changed", null, null);
      pullRequest.setReviewer(singletonMap("new_reviewer", false));

      service.update(REPOSITORY, "changed", pullRequest);

      assertThat(pullRequest.getSubscriber())
        .hasSize(2)
        .anyMatch(r -> r.equals("new_reviewer"));
    }

    @Test
    void shouldSendEvent() {
      PullRequest pullRequest = createPullRequest("changed", null, null);

      service.update(REPOSITORY, "changed", pullRequest);

      assertThat(eventCaptor.getValue())
        .isInstanceOf(PullRequestEvent.class)
        .extracting("item", "oldItem", "eventType")
        .containsExactly(pullRequest, oldPullRequest, HandlerEventType.MODIFY);
    }
  }

  @Nested
  class WithExistingPullRequest {

    PullRequest pullRequest = createPullRequest("id", null, null);

    @BeforeEach
    void addExistingPullRequest() {
      lenient().when(store.get("id")).thenReturn(pullRequest);
    }

    @Test
    void shouldSetPullRequestRejectedAndSendEvent() {
      pullRequest.setStatus(OPEN);

      service.setRejected(REPOSITORY, pullRequest.getId(), REJECTED_BY_USER);

      assertThat(pullRequest.getStatus()).isEqualTo(REJECTED);
      assertThat(eventCaptor.getAllValues()).hasSize(1);
      PullRequestRejectedEvent event = (PullRequestRejectedEvent) eventCaptor.getValue();
      assertThat(event.getCause()).isEqualTo(REJECTED_BY_USER);
      assertThat(event.getRepository()).isSameAs(REPOSITORY);
      assertThat(event.getPullRequest()).isSameAs(pullRequest);
    }

    @Test
    void shouldNotSetPullRequestRejectedMultipleTimes() {
      pullRequest.setStatus(REJECTED);

      service.setRejected(REPOSITORY, pullRequest.getId(), REJECTED_BY_USER);

      assertThat(pullRequest.getStatus()).isEqualTo(REJECTED);
      assertThat(eventCaptor.getAllValues()).hasSize(0);
    }

    @Test
    void shouldNotSetMergedPullRequestRejected() {
      pullRequest.setStatus(MERGED);

      assertThrows(StatusChangeNotAllowedException.class, () -> service.setRejected(REPOSITORY, pullRequest.getId(), REJECTED_BY_USER));

      assertThat(pullRequest.getStatus()).isEqualTo(MERGED);
      assertThat(eventCaptor.getAllValues()).hasSize(0);
    }

    @Test
    void shouldSetPullRequestMergedAndSendEvent() {
      pullRequest.setStatus(OPEN);

      service.setMerged(REPOSITORY, pullRequest.getId());

      assertThat(pullRequest.getStatus()).isEqualTo(MERGED);
      assertThat(eventCaptor.getAllValues()).hasSize(1);
      PullRequestMergedEvent event = (PullRequestMergedEvent) eventCaptor.getValue();
      assertThat(event.getRepository()).isSameAs(REPOSITORY);
      assertThat(event.getPullRequest()).isSameAs(pullRequest);
    }

    @Test
    void shouldNotSetPullRequestMergedMultipleTimes() {
      pullRequest.setStatus(MERGED);

      service.setMerged(REPOSITORY, pullRequest.getId());

      assertThat(pullRequest.getStatus()).isEqualTo(MERGED);
      assertThat(eventCaptor.getAllValues()).hasSize(0);
    }

    @Test
    void shouldNotSetRejectedPullRequestMerged() {
      pullRequest.setStatus(REJECTED);

      assertThrows(StatusChangeNotAllowedException.class, () -> service.setMerged(REPOSITORY, pullRequest.getId()));

      assertThat(pullRequest.getStatus()).isEqualTo(REJECTED);
      assertThat(eventCaptor.getAllValues()).hasSize(0);
    }
  }

  private PullRequest createPullRequest(String id, Instant creationDate, Instant lastModified) {
    return new PullRequest(id, "source", "target", "pr", "description", null, creationDate, lastModified, OPEN, emptySet(), new HashMap<>(), "", "");
  }
}
