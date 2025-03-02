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
import org.mockito.InjectMocks;
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
import sonia.scm.user.User;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static com.cloudogu.scm.review.pullrequest.service.PullRequestRejectedEvent.RejectionCause.REJECTED_BY_USER;
import static com.cloudogu.scm.review.pullrequest.service.PullRequestStatus.*;
import static com.google.common.collect.ImmutableSet.of;
import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

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
  @Mock
  BranchResolver branchResolver;

  @Captor
  ArgumentCaptor<Object> eventCaptor;

  @InjectMocks
  DefaultPullRequestService service;

  @BeforeEach
  void initStore() {
    lenient().when(storeFactory.create(REPOSITORY)).thenReturn(store);
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
      oldPullRequest.setReviewer(Map.of("reviewer", false, "otherReviewer", true));
      when(store.get("changed")).thenReturn(oldPullRequest);
    }

    @Test
    void shouldStoreChangedPullRequest() throws IOException {
      PullRequest pullRequest = createPullRequest("changed", null, null);
      pullRequest.setDescription("new description");
      pullRequest.setTitle("new title");
      pullRequest.setShouldDeleteSourceBranch(true);
      pullRequest.setTarget("new-target");
      mockChangesets(new Changeset());

      service.update(REPOSITORY, "changed", pullRequest);

      verify(store).update(argThat(pr -> {
        assertThat(pr.getDescription()).isEqualTo("new description");
        assertThat(pr.getTitle()).isEqualTo("new title");
        assertThat(pr.getCreationDate()).isEqualTo(oldPullRequest.getCreationDate());
        assertThat(pr.getLastModified()).isNotNull();
        assertThat(pr.isShouldDeleteSourceBranch()).isTrue();
        assertThat(pr.getTarget()).isEqualTo("new-target");
        return true;
      }));
    }

    @Test
    void shouldResetApprovalsWhenTargetBranchHasChanged() throws IOException {
      PullRequest pullRequest = createPullRequest("changed", null, null);
      pullRequest.setTarget("new-target");
      pullRequest.setReviewer(new HashMap<>(oldPullRequest.getReviewer()));
      mockChangesets(new Changeset());

      service.update(REPOSITORY, "changed", pullRequest);

      verify(store).update(argThat(pr -> {
        assertThat(pr.getReviewer()).containsValues(false, false);
        return true;
      }));
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
    void shouldNotChangeCreationDate() {
      PullRequest pullRequest = createPullRequest("changed", null, null);

      service.update(REPOSITORY, "changed", pullRequest);

      verify(store).update(argThat(pr -> {
        assertThat(pr.getCreationDate()).isEqualTo(oldPullRequest.getCreationDate());
        return true;
      }));
    }

    @Test
    void shouldNotChangeApprovalOfExistingReviewers() {
      PullRequest pullRequest = createPullRequest("changed", null, null);
      pullRequest.setReviewer(Map.of("reviewer", true, "otherReviewer", false));

      service.update(REPOSITORY, "changed", pullRequest);

      verify(store).update(argThat(pr -> {
        assertThat(pr.getReviewer().get("reviewer")).isFalse();
        assertThat(pr.getReviewer().get("otherReviewer")).isTrue();
        return true;
      }));
    }

    @Test
    void shouldThrowUnauthorizedForConvertingPr() {
      Subject subject = mock(Subject.class);
      when(subject.isPermitted("repository:mergePullRequest:1")).thenReturn(false);
      when(subject.getPrincipal()).thenReturn("Author");
      shiroRule.setSubject(subject);

      PullRequest pullRequest = createPullRequest("changed", null, null);
      pullRequest.setStatus(DRAFT);
      pullRequest.setAuthor("someone else");
      when(service.get(REPOSITORY, "changed")).thenReturn(pullRequest);

      assertThatThrownBy(() -> service.convertToPR(REPOSITORY, "changed"))
        .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void shouldSendEventWhenStatusConvertedToOpen() {
      Subject subject = mock(Subject.class);
      when(subject.isPermitted("repository:mergePullRequest:1")).thenReturn(true);
      shiroRule.setSubject(subject);

      PullRequest pullRequest = createPullRequest("changed", null, null);
      pullRequest.setStatus(DRAFT);
      when(service.get(REPOSITORY, "changed")).thenReturn(pullRequest);

      service.convertToPR(REPOSITORY, "changed");

      PullRequestStatusChangedEvent event = (PullRequestStatusChangedEvent) eventCaptor.getAllValues().get(0);
      assertThat(event.getRepository()).isSameAs(REPOSITORY);
      assertThat(event.getPullRequest()).isSameAs(pullRequest);
    }

    @Test
    void shouldSendEventWhenStatusConvertedToOpenByAuthor() {
      Subject subject = mock(Subject.class);
      when(subject.isPermitted(any(String.class))).thenReturn(false);
      when(subject.getPrincipal()).thenReturn("Author");
      shiroRule.setSubject(subject);

      PullRequest pullRequest = createPullRequest("changed", null, null);
      pullRequest.setStatus(DRAFT);
      pullRequest.setAuthor("Author");
      when(service.get(REPOSITORY, "changed")).thenReturn(pullRequest);

      service.convertToPR(REPOSITORY, "changed");

      PullRequestStatusChangedEvent event = (PullRequestStatusChangedEvent) eventCaptor.getAllValues().get(0);
      assertThat(event.getRepository()).isSameAs(REPOSITORY);
      assertThat(event.getPullRequest()).isSameAs(pullRequest);
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
    void shouldResetApprovalsWhenConvertedToDraft() {
      PullRequest pullRequest = createPullRequest("changed", null, null);
      pullRequest.setReviewer(Map.of("reviewer", false, "otherReviewer", true));
      pullRequest.setStatus(DRAFT);

      service.update(REPOSITORY, "changed", pullRequest);

      verify(store).update(argThat(pr -> {
        assertThat(pr.getReviewer().get("reviewer")).isFalse();
        assertThat(pr.getReviewer().get("otherReviewer")).isFalse();
        return true;
      }));
    }

    @Test
    void shouldSetLastModifiedDate() {
      PullRequest pullRequest = createPullRequest("changed", Instant.ofEpochSecond(0), Instant.ofEpochMilli(0));

      service.update(REPOSITORY, "changed", pullRequest);

      verify(store).update(argThat(pr -> {
        assertThat(pr.getLastModified()).isNotNull();
        return true;
      }));
    }

    @Test
    void shouldStoreSubscriberFromOldObject() {
      PullRequest pullRequest = createPullRequest("changed", null, null);

      service.update(REPOSITORY, "changed", pullRequest);

      verify(store).update(argThat(pr -> {
        assertThat(pr.getSubscriber()).contains("subscriber");
        return true;
      }));
    }

    @Test
    void shouldAddReviewers() {
      PullRequest pullRequest = createPullRequest("changed", null, null);
      pullRequest.setReviewer(singletonMap("new_reviewer", false));

      service.update(REPOSITORY, "changed", pullRequest);

      verify(store).update(argThat(pr -> {
        assertThat(pr.getReviewer())
          .containsKeys("new_reviewer");
        return true;
      }));
    }

    @Test
    void shouldRemoveReviewers() {
      PullRequest pullRequest = createPullRequest("changed", null, null);
      pullRequest.setReviewer(emptyMap());

      service.update(REPOSITORY, "changed", pullRequest);

      verify(store).update(argThat(pr -> {
        assertThat(pr.getReviewer())
          .isEmpty();
        return true;
      }));
    }

    @Test
    void shouldAddNewReviewerAsSubscriber() {
      PullRequest pullRequest = createPullRequest("changed", null, null);
      pullRequest.setReviewer(singletonMap("new_reviewer", false));

      service.update(REPOSITORY, "changed", pullRequest);

      verify(store).update(argThat(pr -> {
        assertThat(pr.getSubscriber())
          .contains("subscriber", "new_reviewer");
        return true;
      }));
    }

    @Test
    void shouldSendEvent() {
      PullRequest pullRequest = createPullRequest("changed", null, null);

      service.update(REPOSITORY, "changed", pullRequest);

      assertThat(eventCaptor.getValue())
        .isInstanceOf(PullRequestEvent.class);
      PullRequestEvent event = (PullRequestEvent) eventCaptor.getValue();

      assertThat(event.getItem()).matches(pr -> pr.getId().equals("changed"));
      assertThat(event.getOldItem()).isEqualTo(oldPullRequest);
      assertThat(event.getEventType()).isEqualTo(HandlerEventType.MODIFY);
    }

    @Test
    void shouldNotDeleteStatus() {
      PullRequest pullRequest = createPullRequest("changed", null, null);
      pullRequest.setStatus(null);

      service.update(REPOSITORY, "changed", pullRequest);

      verify(store).update(argThat(pr -> {
        assertThat(pr.getStatus()).isEqualTo(OPEN);
        return true;
      }));
    }

    @Test
    void shouldNotResetClosedStatus() {
      oldPullRequest.setStatus(MERGED);
      PullRequest pullRequest = createPullRequest("changed", null, null);
      pullRequest.setStatus(OPEN);

      service.update(REPOSITORY, "changed", pullRequest);

      verify(store).update(argThat(pr -> {
        assertThat(pr.getStatus()).isEqualTo(MERGED);
        return true;
      }));
    }

    @Test
    void shouldNotChangeInProgressStatus() {
      oldPullRequest.setStatus(OPEN);
      PullRequest pullRequest = createPullRequest("changed", null, null);
      pullRequest.setStatus(DRAFT);

      service.update(REPOSITORY, "changed", pullRequest);

      verify(store).update(argThat(pr -> {
        assertThat(pr.getStatus()).isEqualTo(DRAFT);
        return true;
      }));
    }
  }

  @Nested
  class WithExistingPullRequest {

    PullRequest pullRequest = createPullRequest("id", null, null);

    @BeforeEach
    void addExistingPullRequest() {
      lenient().when(store.getAll()).thenReturn(asList(pullRequest));
      lenient().when(store.get("id")).thenReturn(pullRequest);
    }

    @Test
    void shouldSetPullRequestRejectedAndSendEvent() {
      Subject subject = mock(Subject.class, RETURNS_DEEP_STUBS);
      shiroRule.setSubject(subject);
      User currentUser = new User("currentUser");
      when(subject.getPrincipals().oneByType(User.class)).thenReturn(currentUser);

      doReturn(Branch.normalBranch("source", "1"))
        .when(branchResolver).resolve(REPOSITORY, pullRequest.getSource());
      doReturn(Branch.normalBranch("target", "2"))
        .when(branchResolver).resolve(REPOSITORY, pullRequest.getTarget());
      pullRequest.setStatus(OPEN);

      service.setRejected(REPOSITORY, pullRequest.getId(), REJECTED_BY_USER);

      assertThat(pullRequest.getReviser()).isEqualTo(currentUser.getName());
      assertThat(pullRequest.getStatus()).isEqualTo(REJECTED);
      assertThat(pullRequest.getSourceRevision()).isEqualTo("1");
      assertThat(pullRequest.getTargetRevision()).isEqualTo("2");
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
      assertThat(eventCaptor.getAllValues()).isEmpty();
    }

    @Test
    void shouldNotSetMergedPullRequestRejected() {
      pullRequest.setStatus(MERGED);

      assertThrows(StatusChangeNotAllowedException.class, () -> service.setRejected(REPOSITORY, pullRequest.getId(), REJECTED_BY_USER));

      assertThat(pullRequest.getStatus()).isEqualTo(MERGED);
      assertThat(eventCaptor.getAllValues()).isEmpty();
    }

    @Test
    void shouldReopenPullRequest() throws IOException {
      Subject subject = mock(Subject.class, RETURNS_DEEP_STUBS);
      shiroRule.setSubject(subject);

      pullRequest.setStatus(REJECTED);
      mockChangesets(new Changeset());

      service.reopen(REPOSITORY, pullRequest.getId());

      assertThat(pullRequest.getStatus()).isEqualTo(OPEN);
      assertThat(pullRequest.getReviser()).isNull();
      assertThat(pullRequest.getCloseDate()).isNull();
      assertThat(pullRequest.getSourceRevision()).isNull();
      assertThat(pullRequest.getTargetRevision()).isNull();

      PullRequestReopenedEvent pullRequestStatusChangedEvent = (PullRequestReopenedEvent) eventCaptor.getAllValues().get(0);
      assertThat(pullRequestStatusChangedEvent).isInstanceOf(PullRequestReopenedEvent.class);
      assertThat(pullRequestStatusChangedEvent.getRepository()).isSameAs(REPOSITORY);
      assertThat(pullRequestStatusChangedEvent.getPullRequest()).isSameAs(pullRequest);
    }

    @Test
    void shouldNotReopenMergedPullRequest() throws IOException {
      Subject subject = mock(Subject.class, RETURNS_DEEP_STUBS);
      shiroRule.setSubject(subject);

      pullRequest.setStatus(MERGED);
      mockChangesets(new Changeset());

      String pullRequestId = pullRequest.getId();
      assertThrows(StatusChangeNotAllowedException.class, () -> service.reopen(REPOSITORY, pullRequestId));
    }

    @Test
    void shouldNotReopenPullRequestWithoutCreatePermission() {
      Subject subject = mock(Subject.class, RETURNS_DEEP_STUBS);
      shiroRule.setSubject(subject);
      doThrow(UnauthorizedException.class).when(subject).checkPermission("repository:createPullRequest:1");
      pullRequest.setStatus(REJECTED);

      String pullRequestId = pullRequest.getId();
      assertThrows(UnauthorizedException.class, () -> service.reopen(REPOSITORY, pullRequestId));
    }

    @Test
    void shouldNotReopenPullRequestWithDeletedSourceBranch() {
      Subject subject = mock(Subject.class, RETURNS_DEEP_STUBS);
      shiroRule.setSubject(subject);

      doThrow(NotFoundException.class)
        .when(branchResolver)
        .resolve(REPOSITORY, pullRequest.getSource());

      pullRequest.setStatus(REJECTED);
      String pullRequestId = pullRequest.getId();
      assertThrows(NotFoundException.class, () -> service.reopen(REPOSITORY, pullRequestId));
    }

    @Test
    void shouldNotReopenPullRequestWithDeletedTargetBranch() throws IOException {
      Subject subject = mock(Subject.class, RETURNS_DEEP_STUBS);
      shiroRule.setSubject(subject);
      mockChangesets(new Changeset());

      lenient().doThrow(NotFoundException.class)
        .when(branchResolver)
        .resolve(REPOSITORY, pullRequest.getTarget());

      pullRequest.setStatus(REJECTED);
      String pullRequestId = pullRequest.getId();
      assertThrows(NotFoundException.class, () -> service.reopen(REPOSITORY, pullRequestId));
    }

    @Test
    void shouldNotReopenPullRequestWithoutChanges() {
      Subject subject = mock(Subject.class, RETURNS_DEEP_STUBS);
      shiroRule.setSubject(subject);

      pullRequest.setStatus(REJECTED);
      pullRequest.setSource("develop");
      pullRequest.setTarget("develop");

      String pullRequestId = pullRequest.getId();
      assertThrows(StatusChangeNotAllowedException.class, () -> service.reopen(REPOSITORY, pullRequestId));
    }

    @Test
    void shouldSetPullRequestMergedAndSendEvent() {
      Subject subject = mock(Subject.class, RETURNS_DEEP_STUBS);
      shiroRule.setSubject(subject);
      User currentUser = new User("currentUser");
      when(subject.getPrincipals().oneByType(User.class)).thenReturn(currentUser);

      pullRequest.setStatus(OPEN);

      service.setMerged(REPOSITORY, pullRequest.getId());

      assertThat(pullRequest.getStatus()).isEqualTo(MERGED);
      assertThat(pullRequest.getReviser()).isEqualTo(currentUser.getName());
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
      assertThat(eventCaptor.getAllValues()).isEmpty();
    }

    @Test
    void shouldNotSetRejectedPullRequestMerged() {
      pullRequest.setStatus(REJECTED);

      assertThrows(StatusChangeNotAllowedException.class, () -> service.setMerged(REPOSITORY, pullRequest.getId()));

      assertThat(pullRequest.getStatus()).isEqualTo(REJECTED);
      assertThat(eventCaptor.getAllValues()).isEmpty();
    }

    @Test
    void shouldSendPullRequestUpdatedEvent() {
      pullRequest.setStatus(OPEN);

      service.targetRevisionChanged(REPOSITORY, pullRequest.getId());

      assertThat(eventCaptor.getValue()).isInstanceOfSatisfying(PullRequestUpdatedEvent.class, (event) -> {
        assertThat(event.getRepository()).isSameAs(REPOSITORY);
        assertThat(event.getPullRequest()).isSameAs(pullRequest);
      });
    }

    @Test
    void shouldNotSendPullRequestUpdatedEventForNonOpenPRs() {
      pullRequest.setStatus(MERGED);

      service.targetRevisionChanged(REPOSITORY, pullRequest.getId());

      assertThat(eventCaptor.getAllValues()).isEmpty();
    }

    @Test
    void shouldResetApprovalOnCodeChanges() {
      PullRequest pullRequest = createPullRequest("pr1", Instant.ofEpochSecond(1_000_000), null);

      Map<String, Boolean> reviewers = new HashMap<>();
      reviewers.put("reviewer1", true);
      reviewers.put("reviewer2", false);
      pullRequest.setReviewer(reviewers);
      when(store.get("pr1")).thenReturn(pullRequest);

      service.sourceRevisionChanged(REPOSITORY, pullRequest.getId());

      PullRequest storedPullRequest = service.get(REPOSITORY, "pr1");
      assertThat(storedPullRequest.getReviewer()).containsOnly(entry("reviewer1", false), entry("reviewer2", false));
    }

    @Test
    void shouldMarkAsReviewed() {
      service.markAsReviewed(REPOSITORY, pullRequest.getId(), "some/file", new User("user"));

      verify(store).update(argThat(pr -> {
        assertThat(pr.getReviewMarks())
          .contains(new ReviewMark("some/file", "user"));
        return true;
      }));
    }

    @Test
    void shouldMarkAsNotReviewed() {
      pullRequest.setReviewMarks(of(new ReviewMark("some/file", "user")));

      service.markAsNotReviewed(REPOSITORY, pullRequest.getId(), "some/file", new User("user"));

      verify(store).update(argThat(pr -> {
        assertThat(pr.getReviewMarks()).isEmpty();
        return true;
      }));
    }

    @Test
    void shouldRemoveReviewMarks() {
      pullRequest.setReviewMarks(of(
        new ReviewMark("some/path", "dent"),
        new ReviewMark("some/other/path", "dent")
      ));

      service.removeReviewMarks(REPOSITORY, pullRequest.getId(), of(new ReviewMark("some/path", "dent")));

      verify(store).update(argThat(pr -> {
        assertThat(pr.getReviewMarks())
          .contains(new ReviewMark("some/other/path", "dent"));
        return true;
      }));
    }

    @Test
    void shouldDoNothingWhenReviewMarksToBeRemovedAreEmpty() {
      pullRequest.setReviewMarks(of(
        new ReviewMark("some/path", "dent"),
        new ReviewMark("some/other/path", "dent")
      ));

      service.removeReviewMarks(REPOSITORY, pullRequest.getId(), of());

      verify(store, never()).update(any());
    }

    @Test
    void shouldSetPullRequestEmergencyMerged() {
      Subject subject = mock(Subject.class, RETURNS_DEEP_STUBS);
      shiroRule.setSubject(subject);
      User currentUser = new User("currentUser");
      when(subject.getPrincipals().oneByType(User.class)).thenReturn(currentUser);

      PullRequest pullRequest = createPullRequest("emergency", null, null);
      pullRequest.setStatus(OPEN);
      String overrideMessage = "really urgent";
      String ignoredRule = AllReviewerApprovedRule.class.getSimpleName();
      when(store.get("emergency")).thenReturn(pullRequest);

      service.setEmergencyMerged(REPOSITORY, pullRequest.getId(), overrideMessage, ImmutableList.of(ignoredRule));

      assertThat(pullRequest.getStatus()).isEqualTo(MERGED);
      assertThat(pullRequest.isEmergencyMerged()).isTrue();
      assertThat(pullRequest.getOverrideMessage()).isEqualTo(overrideMessage);
      assertThat(pullRequest.getIgnoredMergeObstacles()).hasSize(1);
      assertThat(pullRequest.getIgnoredMergeObstacles().get(0)).isEqualTo(ignoredRule);

      verify(eventBus).post(any(PullRequestEmergencyMergedEvent.class));
    }

    @Test
    void shouldReturnInvalidPullRequestCheckResultForEqualBranches() {
      pullRequest.setSource("feature/1");
      pullRequest.setTarget("develop");

      PullRequestCheckStatus pullRequestCheckStatus = service.checkIfPullRequestIsValid(REPOSITORY, "feature/1", "develop");

      assertThat(pullRequestCheckStatus).isEqualTo(PullRequestCheckStatus.PR_ALREADY_EXISTS);
    }

    @Test
    void shouldIgnoreSameRepositoryInvalidPullRequestCheckResultForEqualBranches() {
      pullRequest.setSource("feature/1");
      pullRequest.setTarget("develop");

      PullRequestCheckStatus pullRequestCheckStatus = service.checkIfPullRequestIsValid(REPOSITORY, "feature/1", "develop");

      assertThat(pullRequestCheckStatus).isEqualTo(PullRequestCheckStatus.PR_ALREADY_EXISTS);
    }
  }

  @Test
  void shouldReturnInvalidPullRequestCheckResultForEqualBranches() {
    PullRequestCheckStatus pullRequestCheckStatus = service.checkIfPullRequestIsValid(REPOSITORY, "develop", "develop");

    assertThat(pullRequestCheckStatus).isEqualTo(PullRequestCheckStatus.SAME_BRANCHES);
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
}
