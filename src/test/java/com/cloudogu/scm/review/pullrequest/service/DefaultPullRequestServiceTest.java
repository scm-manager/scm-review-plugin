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

package com.cloudogu.scm.review.pullrequest.service;

import com.cloudogu.scm.review.BranchResolver;
import com.cloudogu.scm.review.RepositoryResolver;
import com.cloudogu.scm.review.StatusChangeNotAllowedException;
import com.cloudogu.scm.review.pullrequest.api.PullRequestSelector;
import com.cloudogu.scm.review.pullrequest.api.PullRequestSortSelector;
import com.cloudogu.scm.review.pullrequest.api.RequestParameters;
import com.cloudogu.scm.review.pullrequest.dto.PullRequestCheckResultDto.PullRequestCheckStatus;
import com.cloudogu.scm.review.workflow.AllReviewerApprovedRule;
import com.github.sdorra.shiro.ShiroRule;
import com.google.common.collect.ImmutableList;
import org.apache.shiro.authz.UnauthorizedException;
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
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import sonia.scm.HandlerEventType;
import sonia.scm.NotFoundException;
import sonia.scm.event.ScmEventBus;
import sonia.scm.repository.Branch;
import sonia.scm.repository.Changeset;
import sonia.scm.repository.ChangesetPagingResult;
import sonia.scm.repository.Repository;
import sonia.scm.repository.api.LogCommandBuilder;
import sonia.scm.repository.api.RepositoryService;
import sonia.scm.repository.api.RepositoryServiceFactory;
import sonia.scm.store.QueryableMutableStore;
import sonia.scm.store.QueryableStore;
import sonia.scm.store.QueryableStoreExtension;
import sonia.scm.user.User;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static com.cloudogu.scm.review.pullrequest.service.PullRequestRejectedEvent.RejectionCause.REJECTED_BY_USER;
import static com.cloudogu.scm.review.pullrequest.service.PullRequestStatus.DRAFT;
import static com.cloudogu.scm.review.pullrequest.service.PullRequestStatus.MERGED;
import static com.cloudogu.scm.review.pullrequest.service.PullRequestStatus.OPEN;
import static com.cloudogu.scm.review.pullrequest.service.PullRequestStatus.REJECTED;
import static com.google.common.collect.ImmutableSet.of;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, QueryableStoreExtension.class})
@QueryableStoreExtension.QueryableTypes(PullRequest.class)
class DefaultPullRequestServiceTest {

  @Rule
  public ShiroRule shiroRule = new ShiroRule();

  private static final Repository REPOSITORY = new Repository("1", "git", "space", "X");
  private static final User LOGGED_IN_USER = new User("user", "display", "user@example.com");

  @Mock
  RepositoryResolver repositoryResolver;
  PullRequestStoreBuilder storeBuilder;
  @Mock
  ScmEventBus eventBus;
  @Mock
  Subject subject;
  @Mock
  RepositoryServiceFactory repositoryService;
  @Mock
  BranchResolver branchResolver;

  @Captor
  ArgumentCaptor<Object> eventCaptor;

  DefaultPullRequestService service;

  @BeforeEach
  void initEventBus() {
    lenient().doNothing().when(eventBus).post(eventCaptor.capture());
  }

  @BeforeEach
  void initService(PullRequestStoreFactory storeFactory) {
    storeBuilder = new PullRequestStoreBuilder(storeFactory);
    service = new DefaultPullRequestService(
      repositoryResolver,
      branchResolver,
      storeBuilder,
      eventBus,
      repositoryService
    );
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
    void mockExistingDiff() throws IOException {
      mockChangesets(new Changeset());
    }

    @Test
    void shouldStoreNewPullRequest(PullRequestStoreFactory storeFactory) throws NoDifferenceException {
      PullRequest pullRequest = createPullRequest(null, null, null);

      String newId = service.add(REPOSITORY, pullRequest);

      assertThat(newId).isEqualTo("1");
      try (QueryableMutableStore<PullRequest> store = storeFactory.getMutable(REPOSITORY.getId())) {
        assertThat(store.getAll())
          .values()
          .extracting("id")
          .containsExactly("1");
      }
    }

    @Test
    void shouldSetCreationDate() throws NoDifferenceException {
      PullRequest pullRequest = createPullRequest(null, null, null);

      Instant beforeAdd = Instant.now();
      service.add(REPOSITORY, pullRequest);
      Instant afterAdd = Instant.now();

      assertThat(pullRequest.getCreationDate()).isBetween(beforeAdd, afterAdd);
      assertThat(pullRequest.getLastModified()).isBetween(beforeAdd, afterAdd);
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
    void shouldStoreApproverAsReviewer(PullRequestStoreFactory storeFactory) {
      Subject subject = mock(Subject.class);
      shiroRule.setSubject(subject);

      PrincipalCollection principals = mock(PrincipalCollection.class);
      when(subject.getPrincipals()).thenReturn(principals);
      User user1 = new User("user", "User", "user@mail.com");
      when(principals.oneByType(User.class)).thenReturn(user1);

      when(repositoryResolver.resolve(any())).thenReturn(REPOSITORY);
      PullRequest pullRequest = createPullRequest("1", null, null);
      storePullRequest(storeFactory, "1", pullRequest);

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
    void shouldSetReviewerFlagFalseOnDisapprove(PullRequestStoreFactory storeFactory) {
      Subject subject = mock(Subject.class);
      shiroRule.setSubject(subject);

      PrincipalCollection principals = mock(PrincipalCollection.class);
      when(subject.getPrincipals()).thenReturn(principals);
      User user1 = new User("user", "User", "user@mail.com");
      when(principals.oneByType(User.class)).thenReturn(user1);

      when(repositoryResolver.resolve(any())).thenReturn(REPOSITORY);
      PullRequest pullRequest = createPullRequest("1", null, null);
      Map<String, Boolean> reviewers = new HashMap<>();
      reviewers.put(user1.getId(), Boolean.TRUE);
      pullRequest.setReviewer(reviewers);
      storePullRequest(storeFactory, "1", pullRequest);

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
    void shouldFail(PullRequestStoreFactory storeFactory) {
      PullRequest pullRequest = createPullRequest(null, null, null);

      assertThrows(NoDifferenceException.class, () -> service.add(REPOSITORY, pullRequest));

      try (QueryableStore<PullRequest> store = storeFactory.getOverall()) {
        assertThat(store.query().findAll()).isEmpty();
      }
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
    void mockOldPullRequest(PullRequestStoreFactory storeFactory) {
      oldPullRequest = createPullRequest("changed", Instant.ofEpochSecond(1_000_000), null);
      oldPullRequest.setSubscriber(singleton("subscriber"));
      oldPullRequest.setReviewer(Map.of("reviewer", false, "otherReviewer", true));
      storePullRequest(storeFactory, "changed", oldPullRequest);
    }

    @Test
    void shouldStoreChangedPullRequest(PullRequestStoreFactory storeFactory) throws IOException {
      PullRequest pullRequest = createPullRequest("changed", null, null);
      pullRequest.setDescription("new description");
      pullRequest.setTitle("new title");
      pullRequest.setShouldDeleteSourceBranch(true);
      pullRequest.setTarget("new-target");
      mockChangesets(new Changeset());

      service.update(REPOSITORY, "changed", pullRequest);

      PullRequest pr = readPullRequestFromStore(storeFactory, "changed");
      assertThat(pr.getDescription()).isEqualTo("new description");
      assertThat(pr.getTitle()).isEqualTo("new title");
      assertThat(pr.getCreationDate()).isEqualTo(oldPullRequest.getCreationDate());
      assertThat(pr.getLastModified()).isNotNull();
      assertThat(pr.isShouldDeleteSourceBranch()).isTrue();
      assertThat(pr.getTarget()).isEqualTo("new-target");
    }

    @Test
    void shouldResetApprovalsWhenTargetBranchHasChanged(PullRequestStoreFactory storeFactory) throws IOException {
      PullRequest pullRequest = createPullRequest("changed", null, null);
      pullRequest.setTarget("new-target");
      pullRequest.setReviewer(new HashMap<>(oldPullRequest.getReviewer()));
      mockChangesets(new Changeset());

      service.update(REPOSITORY, "changed", pullRequest);

      PullRequest pr = readPullRequestFromStore(storeFactory, "changed");
      assertThat(pr.getReviewer()).containsValues(false, false);
    }

    @Test
    void shouldRejectChangedPullRequestWithInvalidBranches() throws IOException {
      PullRequest pullRequest = createPullRequest("changed", null, null);
      pullRequest.setTarget("source");

      assertThrows(
        SameBranchesNotAllowedException.class,
        () -> service.update(REPOSITORY, "changed", pullRequest)
      );
    }

    @Test
    void shouldNotChangeCreationDate(PullRequestStoreFactory storeFactory) {
      PullRequest pullRequest = createPullRequest("changed", null, null);

      service.update(REPOSITORY, "changed", pullRequest);

      PullRequest pr = readPullRequestFromStore(storeFactory, "changed");
      assertThat(pr.getCreationDate()).isEqualTo(oldPullRequest.getCreationDate());
    }

    @Test
    void shouldNotChangeApprovalOfExistingReviewers(PullRequestStoreFactory storeFactory) {
      PullRequest pullRequest = createPullRequest("changed", null, null);
      pullRequest.setReviewer(Map.of("reviewer", true, "otherReviewer", false));

      service.update(REPOSITORY, "changed", pullRequest);

      PullRequest pr = readPullRequestFromStore(storeFactory, "changed");
      assertThat(pr.getReviewer().get("reviewer")).isFalse();
      assertThat(pr.getReviewer().get("otherReviewer")).isTrue();
    }

    @Test
    void shouldThrowUnauthorizedForConvertingPr(PullRequestStoreFactory storeFactory) {
      Subject subject = mock(Subject.class);
      when(subject.isPermitted("repository:mergePullRequest:1")).thenReturn(false);
      when(subject.getPrincipal()).thenReturn("Author");
      shiroRule.setSubject(subject);

      PullRequest pullRequest = createPullRequest("changed", null, null);
      pullRequest.setStatus(DRAFT);
      pullRequest.setAuthor("someone else");
      storePullRequest(storeFactory, "changed", pullRequest);

      assertThatThrownBy(() -> service.convertToPR(REPOSITORY, "changed"))
        .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void shouldSendEventWhenStatusConvertedToOpen(PullRequestStoreFactory storeFactory) {
      Subject subject = mock(Subject.class);
      when(subject.isPermitted("repository:mergePullRequest:1")).thenReturn(true);
      shiroRule.setSubject(subject);

      PullRequest pullRequest = createPullRequest("changed", null, null);
      pullRequest.setStatus(DRAFT);
      storePullRequest(storeFactory, "changed", pullRequest);

      service.convertToPR(REPOSITORY, "changed");

      PullRequestStatusChangedEvent event = (PullRequestStatusChangedEvent) eventCaptor.getAllValues().get(0);
      assertThat(event.getRepository()).isSameAs(REPOSITORY);
      assertThat(event.getPullRequest().getId()).isEqualTo(pullRequest.getId());
    }

    @Test
    void shouldSendEventWhenStatusConvertedToOpenByAuthor(PullRequestStoreFactory storeFactory) {
      Subject subject = mock(Subject.class);
      when(subject.isPermitted(any(String.class))).thenReturn(false);
      when(subject.getPrincipal()).thenReturn("Author");
      shiroRule.setSubject(subject);

      PullRequest pullRequest = createPullRequest("changed", null, null);
      pullRequest.setStatus(DRAFT);
      pullRequest.setAuthor("Author");
      storePullRequest(storeFactory, "changed", pullRequest);

      service.convertToPR(REPOSITORY, "changed");

      PullRequestStatusChangedEvent event = (PullRequestStatusChangedEvent) eventCaptor.getAllValues().get(0);
      assertThat(event.getRepository()).isSameAs(REPOSITORY);
      assertThat(event.getPullRequest().getId()).isEqualTo(pullRequest.getId());
    }

    @Test
    void shouldSendEventWhenStatusConvertedToDraft() {
      PullRequest pullRequest = createPullRequest("changed", null, null);
      pullRequest.setStatus(DRAFT);

      service.update(REPOSITORY, "changed", pullRequest);

      PullRequestStatusChangedEvent event = (PullRequestStatusChangedEvent) eventCaptor.getAllValues().get(0);
      assertThat(event.getRepository()).isSameAs(REPOSITORY);
      assertThat(event.getPullRequest()).isSameAs(pullRequest);
    }

    @Test
    void shouldResetApprovalsWhenConvertedToDraft(PullRequestStoreFactory storeFactory) {
      PullRequest pullRequest = createPullRequest("changed", null, null);
      pullRequest.setReviewer(Map.of("reviewer", false, "otherReviewer", true));
      pullRequest.setStatus(DRAFT);

      service.update(REPOSITORY, "changed", pullRequest);

      PullRequest pr = readPullRequestFromStore(storeFactory, "changed");
      assertThat(pr.getReviewer().get("reviewer")).isFalse();
      assertThat(pr.getReviewer().get("otherReviewer")).isFalse();
    }

    @Test
    void shouldSetLastModifiedDate(PullRequestStoreFactory storeFactory) {
      PullRequest pullRequest = createPullRequest("changed", Instant.ofEpochSecond(0), Instant.ofEpochMilli(0));

      service.update(REPOSITORY, "changed", pullRequest);

      PullRequest pr = readPullRequestFromStore(storeFactory, "changed");
      assertThat(pr.getLastModified()).isNotNull();
    }

    @Test
    void shouldStoreSubscriberFromOldObject(PullRequestStoreFactory storeFactory) {
      PullRequest pullRequest = createPullRequest("changed", null, null);

      service.update(REPOSITORY, "changed", pullRequest);

      PullRequest pr = readPullRequestFromStore(storeFactory, "changed");
      assertThat(pr.getSubscriber()).contains("subscriber");
    }

    @Test
    void shouldAddReviewers(PullRequestStoreFactory storeFactory) {
      PullRequest pullRequest = createPullRequest("changed", null, null);
      pullRequest.setReviewer(singletonMap("new_reviewer", false));

      service.update(REPOSITORY, "changed", pullRequest);

      PullRequest pr = readPullRequestFromStore(storeFactory, "changed");
      assertThat(pr.getReviewer())
        .containsKeys("new_reviewer");
    }

    @Test
    void shouldRemoveReviewers(PullRequestStoreFactory storeFactory) {
      PullRequest pullRequest = createPullRequest("changed", null, null);
      pullRequest.setReviewer(emptyMap());

      service.update(REPOSITORY, "changed", pullRequest);

      PullRequest pr = readPullRequestFromStore(storeFactory, "changed");
      assertThat(pr.getReviewer())
        .isEmpty();
    }

    @Test
    void shouldAddNewReviewerAsSubscriber(PullRequestStoreFactory storeFactory) {
      PullRequest pullRequest = createPullRequest("changed", null, null);
      pullRequest.setReviewer(singletonMap("new_reviewer", false));

      service.update(REPOSITORY, "changed", pullRequest);

      PullRequest pr = readPullRequestFromStore(storeFactory, "changed");
      assertThat(pr.getSubscriber())
        .contains("subscriber", "new_reviewer");
    }

    @Test
    void shouldSendEvent() {
      PullRequest pullRequest = createPullRequest("changed", null, null);

      service.update(REPOSITORY, "changed", pullRequest);

      assertThat(eventCaptor.getValue())
        .isInstanceOf(PullRequestEvent.class);
      PullRequestEvent event = (PullRequestEvent) eventCaptor.getValue();

      assertThat(event.getItem()).matches(pr -> pr.getId().equals("changed"));
      assertThat(event.getOldItem().getId()).isEqualTo(oldPullRequest.getId());
      assertThat(event.getEventType()).isEqualTo(HandlerEventType.MODIFY);
    }

    @Test
    void shouldNotDeleteStatus(PullRequestStoreFactory storeFactory) {
      PullRequest pullRequest = createPullRequest("changed", null, null);
      pullRequest.setStatus(null);

      service.update(REPOSITORY, "changed", pullRequest);

      PullRequest pr = readPullRequestFromStore(storeFactory, "changed");
      assertThat(pr.getStatus()).isEqualTo(OPEN);
    }

    @Test
    void shouldNotResetClosedStatus(PullRequestStoreFactory storeFactory) {
      oldPullRequest.setStatus(MERGED);
      storePullRequest(storeFactory, "changed", oldPullRequest);
      PullRequest pullRequest = createPullRequest("changed", null, null);
      pullRequest.setStatus(OPEN);

      service.update(REPOSITORY, "changed", pullRequest);

      PullRequest pr = readPullRequestFromStore(storeFactory, "changed");
      assertThat(pr.getStatus()).isEqualTo(MERGED);
    }

    @Test
    void shouldNotChangeInProgressStatus(PullRequestStoreFactory storeFactory) {
      oldPullRequest.setStatus(OPEN);
      PullRequest pullRequest = createPullRequest("changed", null, null);
      pullRequest.setStatus(DRAFT);

      service.update(REPOSITORY, "changed", pullRequest);

      PullRequest pr = readPullRequestFromStore(storeFactory, "changed");
      assertThat(pr.getStatus()).isEqualTo(DRAFT);
    }
  }

  @Nested
  class WithExistingPullRequest {

    PullRequest pullRequest = createPullRequest("id", null, null);

    @BeforeEach
    void addExistingPullRequest(PullRequestStoreFactory storeFactory) {
      storePullRequest(storeFactory, "id", pullRequest);
    }

    @Test
    void shouldSetPullRequestRejectedAndSendEvent(PullRequestStoreFactory storeFactory) {
      Subject subject = mock(Subject.class, RETURNS_DEEP_STUBS);
      shiroRule.setSubject(subject);
      User currentUser = new User("currentUser");
      when(subject.getPrincipals().oneByType(User.class)).thenReturn(currentUser);

      doReturn(Branch.normalBranch("source", "1"))
        .when(branchResolver).resolve(REPOSITORY, pullRequest.getSource());
      doReturn(Branch.normalBranch("target", "2"))
        .when(branchResolver).resolve(REPOSITORY, pullRequest.getTarget());
      pullRequest.setStatus(OPEN);
      storePullRequest(storeFactory, "id", pullRequest);

      service.setRejected(REPOSITORY, pullRequest.getId(), REJECTED_BY_USER);

      PullRequest pr = readPullRequestFromStore(storeFactory);
      assertThat(pr.getReviser()).isEqualTo(currentUser.getName());
      assertThat(pr.getStatus()).isEqualTo(REJECTED);
      assertThat(pr.getSourceRevision()).isEqualTo("1");
      assertThat(pr.getTargetRevision()).isEqualTo("2");
      assertThat(eventCaptor.getAllValues()).hasSize(1);
      PullRequestRejectedEvent event = (PullRequestRejectedEvent) eventCaptor.getValue();
      assertThat(event.getCause()).isEqualTo(REJECTED_BY_USER);
      assertThat(event.getRepository()).isSameAs(REPOSITORY);
      assertPullRequestsAreEqual(event.getPullRequest(), pr);
    }

    @Test
    void shouldNotSetPullRequestRejectedMultipleTimes(PullRequestStoreFactory storeFactory) {
      pullRequest.setStatus(REJECTED);
      storePullRequest(storeFactory, "id", pullRequest);

      service.setRejected(REPOSITORY, pullRequest.getId(), REJECTED_BY_USER);

      PullRequest pr = readPullRequestFromStore(storeFactory);
      assertThat(pr.getStatus()).isEqualTo(REJECTED);
      assertThat(eventCaptor.getAllValues()).isEmpty();
    }

    @Test
    void shouldNotSetMergedPullRequestRejected(PullRequestStoreFactory storeFactory) {
      pullRequest.setStatus(MERGED);
      storePullRequest(storeFactory, "id", pullRequest);

      assertThrows(StatusChangeNotAllowedException.class, () -> service.setRejected(REPOSITORY, pullRequest.getId(), REJECTED_BY_USER));

      PullRequest pr = readPullRequestFromStore(storeFactory);
      assertThat(pr.getStatus()).isEqualTo(MERGED);
      assertThat(eventCaptor.getAllValues()).isEmpty();
    }

    @Test
    void shouldReopenPullRequest(PullRequestStoreFactory storeFactory) throws IOException {
      Subject subject = mock(Subject.class, RETURNS_DEEP_STUBS);
      shiroRule.setSubject(subject);

      pullRequest.setStatus(REJECTED);
      storePullRequest(storeFactory, "id", pullRequest);
      mockChangesets(new Changeset());

      service.reopen(REPOSITORY, pullRequest.getId());

      PullRequest pr = readPullRequestFromStore(storeFactory);
      assertThat(pr.getStatus()).isEqualTo(OPEN);
      assertThat(pr.getReviser()).isNull();
      assertThat(pr.getCloseDate()).isNull();
      assertThat(pr.getSourceRevision()).isNull();
      assertThat(pr.getTargetRevision()).isNull();

      PullRequestReopenedEvent pullRequestStatusChangedEvent = (PullRequestReopenedEvent) eventCaptor.getAllValues().get(0);
      assertThat(pullRequestStatusChangedEvent).isInstanceOf(PullRequestReopenedEvent.class);
      assertThat(pullRequestStatusChangedEvent.getRepository()).isSameAs(REPOSITORY);
      assertPullRequestsAreEqual(pullRequestStatusChangedEvent.getPullRequest(), pr);
    }

    @Test
    void shouldNotReopenMergedPullRequest(PullRequestStoreFactory storeFactory) throws IOException {
      Subject subject = mock(Subject.class, RETURNS_DEEP_STUBS);
      shiroRule.setSubject(subject);

      pullRequest.setStatus(MERGED);
      storePullRequest(storeFactory, "id", pullRequest);
      mockChangesets(new Changeset());

      String pullRequestId = pullRequest.getId();
      assertThrows(StatusChangeNotAllowedException.class, () -> service.reopen(REPOSITORY, pullRequestId));
    }

    @Test
    void shouldNotReopenPullRequestWithoutCreatePermission(PullRequestStoreFactory storeFactory) {
      Subject subject = mock(Subject.class, RETURNS_DEEP_STUBS);
      shiroRule.setSubject(subject);
      doThrow(UnauthorizedException.class).when(subject).checkPermission("repository:createPullRequest:1");
      pullRequest.setStatus(REJECTED);
      storePullRequest(storeFactory, "id", pullRequest);

      String pullRequestId = pullRequest.getId();
      assertThrows(UnauthorizedException.class, () -> service.reopen(REPOSITORY, pullRequestId));
    }

    @Test
    void shouldNotReopenPullRequestWithDeletedSourceBranch(PullRequestStoreFactory storeFactory) {
      Subject subject = mock(Subject.class, RETURNS_DEEP_STUBS);
      shiroRule.setSubject(subject);

      doThrow(NotFoundException.class)
        .when(branchResolver)
        .resolve(REPOSITORY, pullRequest.getSource());

      pullRequest.setStatus(REJECTED);
      storePullRequest(storeFactory, "id", pullRequest);
      String pullRequestId = pullRequest.getId();

      assertThrows(NotFoundException.class, () -> service.reopen(REPOSITORY, pullRequestId));
    }

    @Test
    void shouldNotReopenPullRequestWithDeletedTargetBranch(PullRequestStoreFactory storeFactory) throws IOException {
      Subject subject = mock(Subject.class, RETURNS_DEEP_STUBS);
      shiroRule.setSubject(subject);
      mockChangesets(new Changeset());

      lenient().doThrow(NotFoundException.class)
        .when(branchResolver)
        .resolve(REPOSITORY, pullRequest.getTarget());

      pullRequest.setStatus(REJECTED);
      storePullRequest(storeFactory, "id", pullRequest);
      String pullRequestId = pullRequest.getId();

      assertThrows(NotFoundException.class, () -> service.reopen(REPOSITORY, pullRequestId));
    }

    @Test
    void shouldNotReopenPullRequestWithoutChanges(PullRequestStoreFactory storeFactory) {
      Subject subject = mock(Subject.class, RETURNS_DEEP_STUBS);
      shiroRule.setSubject(subject);

      pullRequest.setStatus(REJECTED);
      pullRequest.setSource("develop");
      pullRequest.setTarget("develop");
      storePullRequest(storeFactory, "id", pullRequest);

      String pullRequestId = pullRequest.getId();
      assertThrows(StatusChangeNotAllowedException.class, () -> service.reopen(REPOSITORY, pullRequestId));
    }

    @Test
    void shouldSetPullRequestMergedAndSendEvent(PullRequestStoreFactory storeFactory) {
      Subject subject = mock(Subject.class, RETURNS_DEEP_STUBS);
      shiroRule.setSubject(subject);
      User currentUser = new User("currentUser");
      when(subject.getPrincipals().oneByType(User.class)).thenReturn(currentUser);

      pullRequest.setStatus(OPEN);
      storePullRequest(storeFactory, "id", pullRequest);

      service.setMerged(REPOSITORY, pullRequest.getId());

      PullRequest pr = readPullRequestFromStore(storeFactory);
      assertThat(pr.getStatus()).isEqualTo(MERGED);
      assertThat(pr.getReviser()).isEqualTo(currentUser.getName());
      assertThat(eventCaptor.getAllValues()).hasSize(1);
      PullRequestMergedEvent event = (PullRequestMergedEvent) eventCaptor.getValue();
      assertThat(event.getRepository()).isSameAs(REPOSITORY);
      assertPullRequestsAreEqual(event.getPullRequest(), pr);
    }

    @Test
    void shouldNotSetPullRequestMergedMultipleTimes(PullRequestStoreFactory storeFactory) {
      pullRequest.setStatus(MERGED);
      storePullRequest(storeFactory, "id", pullRequest);

      service.setMerged(REPOSITORY, pullRequest.getId());

      PullRequest pr = readPullRequestFromStore(storeFactory);
      assertThat(pr.getStatus()).isEqualTo(MERGED);
      assertThat(eventCaptor.getAllValues()).isEmpty();
    }

    @Test
    void shouldNotSetRejectedPullRequestMerged(PullRequestStoreFactory storeFactory) {
      pullRequest.setStatus(REJECTED);
      storePullRequest(storeFactory, "id", pullRequest);

      assertThrows(StatusChangeNotAllowedException.class, () -> service.setMerged(REPOSITORY, pullRequest.getId()));

      PullRequest pr = readPullRequestFromStore(storeFactory);
      assertThat(pr.getStatus()).isEqualTo(REJECTED);
      assertThat(eventCaptor.getAllValues()).isEmpty();
    }

    @Test
    void shouldSendPullRequestUpdatedEvent(PullRequestStoreFactory storeFactory) {
      pullRequest.setStatus(OPEN);
      storePullRequest(storeFactory, "id", pullRequest);

      service.targetRevisionChanged(REPOSITORY, pullRequest.getId());

      assertThat(eventCaptor.getValue()).isInstanceOfSatisfying(PullRequestUpdatedEvent.class, (event) -> {
        assertThat(event.getRepository()).isSameAs(REPOSITORY);
        assertPullRequestsAreEqual(event.getPullRequest(), pullRequest);
      });
    }

    @Test
    void shouldNotSendPullRequestUpdatedEventForNonOpenPRs(PullRequestStoreFactory storeFactory) {
      pullRequest.setStatus(MERGED);
      storePullRequest(storeFactory, "id", pullRequest);

      service.targetRevisionChanged(REPOSITORY, pullRequest.getId());

      assertThat(eventCaptor.getAllValues()).isEmpty();
    }

    @Test
    void shouldResetApprovalOnCodeChanges(PullRequestStoreFactory storeFactory) {
      PullRequest pullRequest = createPullRequest("pr1", Instant.ofEpochSecond(1_000_000), null);

      Map<String, Boolean> reviewers = new HashMap<>();
      reviewers.put("reviewer1", true);
      reviewers.put("reviewer2", false);
      pullRequest.setReviewer(reviewers);
      storePullRequest(storeFactory, "pr1", pullRequest);

      service.sourceRevisionChanged(REPOSITORY, pullRequest.getId());

      PullRequest storedPullRequest = service.get(REPOSITORY, "pr1");
      assertThat(storedPullRequest.getReviewer()).containsOnly(entry("reviewer1", false), entry("reviewer2", false));
    }

    @Test
    void shouldMarkAsReviewed(PullRequestStoreFactory storeFactory) {
      service.markAsReviewed(REPOSITORY, pullRequest.getId(), "some/file", new User("user"));

      PullRequest pr = readPullRequestFromStore(storeFactory);
      assertThat(pr.getReviewMarks())
        .contains(new ReviewMark("some/file", "user"));
    }

    @Test
    void shouldMarkAsNotReviewed(PullRequestStoreFactory storeFactory) {
      pullRequest.setReviewMarks(of(new ReviewMark("some/file", "user")));

      service.markAsNotReviewed(REPOSITORY, pullRequest.getId(), "some/file", new User("user"));

      PullRequest pr = readPullRequestFromStore(storeFactory);
      assertThat(pr.getReviewMarks()).isEmpty();
    }

    @Test
    void shouldRemoveReviewMarks(PullRequestStoreFactory storeFactory) {
      pullRequest.setReviewMarks(of(
        new ReviewMark("some/path", "dent"),
        new ReviewMark("some/other/path", "dent")
      ));
      storePullRequest(storeFactory, "id", pullRequest);

      service.removeReviewMarks(REPOSITORY, pullRequest.getId(), of(new ReviewMark("some/path", "dent")));

      PullRequest pr = readPullRequestFromStore(storeFactory);
      assertThat(pr.getReviewMarks())
        .contains(new ReviewMark("some/other/path", "dent"));
    }

    @Test
    void shouldDoNothingWhenReviewMarksToBeRemovedAreEmpty(PullRequestStoreFactory storeFactory) {
      pullRequest.setReviewMarks(of(
        new ReviewMark("some/path", "dent"),
        new ReviewMark("some/other/path", "dent")
      ));
      storePullRequest(storeFactory, "id", pullRequest);

      service.removeReviewMarks(REPOSITORY, pullRequest.getId(), of());

      PullRequest pr = readPullRequestFromStore(storeFactory);
      assertThat(pr.getReviewMarks()).hasSize(2);
    }

    @Test
    void shouldSetPullRequestEmergencyMerged(PullRequestStoreFactory storeFactory) {
      Subject subject = mock(Subject.class, RETURNS_DEEP_STUBS);
      shiroRule.setSubject(subject);
      User currentUser = new User("currentUser");
      when(subject.getPrincipals().oneByType(User.class)).thenReturn(currentUser);

      PullRequest pullRequest = createPullRequest("emergency", null, null);
      pullRequest.setStatus(OPEN);
      String overrideMessage = "really urgent";
      String ignoredRule = AllReviewerApprovedRule.class.getSimpleName();
      storePullRequest(storeFactory, "emergency", pullRequest);

      service.setEmergencyMerged(REPOSITORY, pullRequest.getId(), overrideMessage, ImmutableList.of(ignoredRule));

      PullRequest pr = readPullRequestFromStore(storeFactory, "emergency");
      assertThat(pr.getStatus()).isEqualTo(MERGED);
      assertThat(pr.isEmergencyMerged()).isTrue();
      assertThat(pr.getOverrideMessage()).isEqualTo(overrideMessage);
      assertThat(pr.getIgnoredMergeObstacles()).hasSize(1);
      assertThat(pr.getIgnoredMergeObstacles().get(0)).isEqualTo(ignoredRule);

      verify(eventBus).post(any(PullRequestEmergencyMergedEvent.class));
    }

    @Test
    void shouldIgnoreSameRepositoryInvalidPullRequestCheckResultForEqualBranches(PullRequestStoreFactory storeFactory) {
      pullRequest.setSource("feature/1");
      pullRequest.setTarget("develop");
      storePullRequest(storeFactory, "id", pullRequest);

      PullRequestCheckStatus pullRequestCheckStatus = service.checkIfPullRequestIsValid(REPOSITORY, "feature/1", "develop");

      assertThat(pullRequestCheckStatus).isEqualTo(PullRequestCheckStatus.PR_ALREADY_EXISTS);
    }
  }

  @Test
  void shouldReturnInvalidPullRequestCheckResultForEqualBranches() {
    PullRequestCheckStatus pullRequestCheckStatus = service.checkIfPullRequestIsValid(REPOSITORY, "develop", "develop");

    assertThat(pullRequestCheckStatus).isEqualTo(PullRequestCheckStatus.SAME_BRANCHES);
  }

  @Test
  void shouldSortById(PullRequestStoreFactory storeFactory) {
    when(repositoryResolver.resolve(REPOSITORY.getNamespaceAndName()))
      .thenReturn(REPOSITORY);
    for (int i = 1; i < 11; i++) {
      storePullRequest(storeFactory, createPullRequest(String.valueOf(i), null, null));
    }

    List<PullRequest> pullRequests =
      service.getAll(
        REPOSITORY.getNamespace(),
        REPOSITORY.getName(),
        new RequestParameters(
          PullRequestSelector.ALL,
          PullRequestSortSelector.ID_ASC,
          0,
          3
        )
      );

    assertThat(pullRequests)
      .extracting("id")
      .containsExactly("1", "2", "3");
  }

  private PullRequest createPullRequest(String id, Instant creationDate, Instant lastModified) {
    PullRequest pullRequest = new PullRequest();
    pullRequest.setId(id);
    pullRequest.setSource("source");
    pullRequest.setTarget("target");
    pullRequest.setTitle("pr");
    pullRequest.setDescription("description");
    pullRequest.setCreationDate(creationDate);
    pullRequest.setLastModified(lastModified);
    pullRequest.setStatus(OPEN);
    pullRequest.setSubscriber(emptySet());
    pullRequest.setReviewer(new HashMap<>());
    pullRequest.setSourceRevision("");
    pullRequest.setTargetRevision("");
    pullRequest.setReviewMarks(emptySet());
    pullRequest.setIgnoredMergeObstacles(emptyList());
    return pullRequest;
  }

  private void assertPullRequestsAreEqual(PullRequest pr1, PullRequest pr2) {
    truncateToMillis(pr1::getCreationDate, pr1::setCreationDate);
    truncateToMillis(pr1::getLastModified, pr1::setLastModified);
    truncateToMillis(pr2::getCreationDate, pr2::setCreationDate);
    truncateToMillis(pr2::getLastModified, pr2::setLastModified);
    truncateToMillis(pr1::getCloseDate, pr1::setCloseDate);
    truncateToMillis(pr2::getCloseDate, pr2::setCloseDate);
    assertThat(pr1).isEqualTo(pr2);
  }

  private void truncateToMillis(Supplier<Instant> getter, Consumer<Instant> setter) {
    Instant instant = getter.get();
    if (instant != null) {
      // nanoseconds are not supported by the json mapper
      setter.accept(instant.truncatedTo(ChronoUnit.MILLIS));
    }
  }

  private void storePullRequest(PullRequestStoreFactory storeFactory, PullRequest pullRequest) {
    try (QueryableMutableStore<PullRequest> store = storeFactory.getMutable(REPOSITORY)) {
      store.put(pullRequest);
    }
  }

  private void storePullRequest(PullRequestStoreFactory storeFactory, String id, PullRequest pullRequest) {
    try (QueryableMutableStore<PullRequest> store = storeFactory.getMutable(REPOSITORY)) {
      store.put(id, pullRequest);
    }
  }

  private PullRequest readPullRequestFromStore(PullRequestStoreFactory storeFactory) {
    return readPullRequestFromStore(storeFactory, "id");
  }

  private PullRequest readPullRequestFromStore(PullRequestStoreFactory storeFactory, String pullRequestId) {
    try (QueryableMutableStore<PullRequest> store = storeFactory.getMutable(REPOSITORY)) {
      return store.get(pullRequestId);
    }
  }
}
