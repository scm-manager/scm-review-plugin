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

import com.cloudogu.scm.review.InternalMergeSwitch;
import com.cloudogu.scm.review.pullrequest.dto.MergeCommitDto;
import com.cloudogu.scm.review.pullrequest.service.MergeService.CommitDefaults;
import com.google.common.collect.ImmutableList;
import org.apache.shiro.authz.UnauthorizedException;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.repository.Branch;
import sonia.scm.repository.Branches;
import sonia.scm.repository.Changeset;
import sonia.scm.repository.ChangesetPagingResult;
import sonia.scm.repository.Contributor;
import sonia.scm.repository.NamespaceAndName;
import sonia.scm.repository.Person;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryTestData;
import sonia.scm.repository.api.BranchCommandBuilder;
import sonia.scm.repository.api.BranchesCommandBuilder;
import sonia.scm.repository.api.Command;
import sonia.scm.repository.api.LogCommandBuilder;
import sonia.scm.repository.api.MergeCommandBuilder;
import sonia.scm.repository.api.MergeCommandResult;
import sonia.scm.repository.api.MergeDryRunCommandResult;
import sonia.scm.repository.api.MergeStrategy;
import sonia.scm.repository.api.MergeStrategyNotSupportedException;
import sonia.scm.repository.api.RepositoryService;
import sonia.scm.repository.api.RepositoryServiceFactory;
import sonia.scm.user.DisplayUser;
import sonia.scm.user.EMail;
import sonia.scm.user.User;
import sonia.scm.user.UserDisplayManager;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.Set;

import static com.cloudogu.scm.review.pullrequest.service.PullRequestStatus.OPEN;
import static com.cloudogu.scm.review.pullrequest.service.PullRequestStatus.REJECTED;
import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MergeServiceTest {

  private final Repository REPOSITORY = RepositoryTestData.createHeartOfGold();

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private Subject subject;

  @Mock
  BranchCommandBuilder branchCommandBuilder;
  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  BranchesCommandBuilder branchesCommandBuilder;
  @Mock(answer = Answers.RETURNS_SELF)
  LogCommandBuilder logCommandBuilder;
  @Mock
  private RepositoryServiceFactory serviceFactory;
  @Mock
  private RepositoryService repositoryService;
  @Mock
  private PullRequestService pullRequestService;
  @Mock
  private MergeCommandBuilder mergeCommandBuilder;
  @Mock
  private InternalMergeSwitch internalMergeSwitch;
  @Mock
  private UserDisplayManager userDisplayManager;
  @Mock
  private MergeCommitMessageService mergeCommitMessageService;

  @Mock
  private EMail email;

  private final Set<MergeGuard> mergeGuards = new HashSet<>();

  private MergeService service;

  @BeforeEach
  void initService() {
    service = new MergeService(serviceFactory, pullRequestService, mergeGuards, internalMergeSwitch, userDisplayManager, mergeCommitMessageService);
    lenient().doAnswer(invocation -> {
        invocation.<Runnable>getArgument(0).run();
        return null;
      })
      .when(internalMergeSwitch).runInternalMerge(any());
  }

  @BeforeEach
  void initMocks() {
    lenient().when(serviceFactory.create(REPOSITORY.getNamespaceAndName())).thenReturn(repositoryService);
    lenient().when(repositoryService.getRepository()).thenReturn(REPOSITORY);
    lenient().when(repositoryService.getMergeCommand()).thenReturn(mergeCommandBuilder);
    lenient().when(repositoryService.getLogCommand()).thenReturn(logCommandBuilder);
    lenient().when(logCommandBuilder.setAncestorChangeset(any())).thenReturn(logCommandBuilder);
    lenient().when(email.getMailOrFallback(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0, User.class).getMail());
  }

  @BeforeEach
  void bindSubject() {
    ThreadContext.bind(subject);
  }

  @AfterEach
  void shouldNeverSetAuthorToNull() {
    verify(mergeCommandBuilder, never()).setAuthor((DisplayUser) null);
  }

  @AfterEach
  void tearDownSubject() {
    ThreadContext.unbindSubject();
  }

  @Test
  void shouldMergeSuccessfully() {
    when(mergeCommandBuilder.isSupported(MergeStrategy.MERGE_COMMIT)).thenReturn(true);
    when(mergeCommandBuilder.executeMerge()).thenReturn(MergeCommandResult.success("1", "2", "123"));
    mockPullRequest("squash", "master", "1");

    MergeCommitDto mergeCommit = createMergeCommit(false);
    service.merge(REPOSITORY.getNamespaceAndName(), "1", mergeCommit, MergeStrategy.MERGE_COMMIT, false);
    verify(pullRequestService).setRevisions(REPOSITORY, "1", "1", "2");
    verify(pullRequestService, never()).setEmergencyMerged(any(Repository.class), anyString(), anyString(), anyList());
  }

  @Test
  void shouldEnrichCommitMessageWithReviewedBy() {
    mockUser("zaphod", "Zaphod Beeblebrox", "zaphod@hitchhiker.org");
    when(mergeCommandBuilder.isSupported(MergeStrategy.MERGE_COMMIT)).thenReturn(true);
    when(mergeCommandBuilder.executeMerge()).thenReturn(MergeCommandResult.success("1", "2", "123"));
    when(userDisplayManager.get("trillian")).thenReturn(of(DisplayUser.from(new User("trillian", "Tricia McMillan", "trillian@hitchhiker.org"))));
    when(userDisplayManager.get("dent")).thenReturn(of(DisplayUser.from(new User("dent", "Arthur Dent", "dent@hitchhiker.org"))));
    PullRequest pullRequest = mockPullRequest("squash", "master", "1");
    LinkedHashMap<String, Boolean> reviewers = new LinkedHashMap<>();
    reviewers.put("dent", true);
    reviewers.put("trillian", true);
    reviewers.put("zaphod", false);
    pullRequest.setReviewer(reviewers);

    MergeCommitDto mergeCommit = createMergeCommit(false);
    mergeCommit.setCommitMessage("42");
    service.merge(REPOSITORY.getNamespaceAndName(), "1", mergeCommit, MergeStrategy.MERGE_COMMIT, false);
    verify(mergeCommandBuilder).setMessage(
      "42\n\n" +
        "Reviewed-by: Arthur Dent <dent@hitchhiker.org>\n" +
        "Reviewed-by: Tricia McMillan <trillian@hitchhiker.org>\n"
    );
  }

  @Test
  void shouldEmergencyMergeSuccessfully() {
    when(mergeCommandBuilder.isSupported(MergeStrategy.MERGE_COMMIT)).thenReturn(true);
    when(mergeCommandBuilder.executeMerge()).thenReturn(MergeCommandResult.success("1", "2", "123"));
    mockPullRequest("squash", "master", "1");

    MergeCommitDto mergeCommit = createMergeCommit(false);
    service.merge(REPOSITORY.getNamespaceAndName(), "1", mergeCommit, MergeStrategy.MERGE_COMMIT, true);

    verify(pullRequestService).setEmergencyMerged(REPOSITORY, "1", mergeCommit.getOverrideMessage(), Collections.emptyList());
    verify(pullRequestService, never()).setMerged(REPOSITORY, "1");
  }

  @Test
  void shouldNotEmergencyMergeWithoutPermission() {
    doThrow(UnauthorizedException.class).when(subject).checkPermission("repository:performEmergencyMerge:" + REPOSITORY.getId());
    mockPullRequest("squash", "master", "1");

    MergeCommitDto mergeCommit = createMergeCommit(false);

    NamespaceAndName namespaceAndName = REPOSITORY.getNamespaceAndName();
    assertThrows(UnauthorizedException.class,
      () -> service.merge(namespaceAndName, "1", mergeCommit, MergeStrategy.SQUASH, true));
  }

  @Test
  void shouldNotMergeWithObstaclesIfNotEmergency() {
    PullRequest pullRequest = mockPullRequest("squash", "master", "1");
    mockMergeGuard(pullRequest, true);
    MergeCommitDto mergeCommit = createMergeCommit(false);

    NamespaceAndName namespaceAndName = REPOSITORY.getNamespaceAndName();
    assertThrows(MergeNotAllowedException.class,
      () -> service.merge(namespaceAndName, "1", mergeCommit, MergeStrategy.SQUASH, false));
  }

  @Test
  void shouldNotMergeWithoutPermission() {
    doThrow(UnauthorizedException.class).when(subject).checkPermission("repository:mergePullRequest:" + REPOSITORY.getId());
    mockPullRequest("squash", "master", "1");

    MergeCommitDto mergeCommit = createMergeCommit(false);
    NamespaceAndName namespaceAndName = REPOSITORY.getNamespaceAndName();
    assertThrows(UnauthorizedException.class,
      () -> service.merge(namespaceAndName, "1", mergeCommit, MergeStrategy.SQUASH, false));
  }

  @Test
  void shouldDeleteBranchIfFlagIsSet() {
    when(mergeCommandBuilder.isSupported(MergeStrategy.MERGE_COMMIT)).thenReturn(true);
    when(mergeCommandBuilder.executeMerge()).thenReturn(MergeCommandResult.success("1", "2", "123"));
    mockPullRequest("squash", "master", "1");
    when(repositoryService.getBranchCommand()).thenReturn(branchCommandBuilder);
    when(repositoryService.isSupported(Command.BRANCH)).thenReturn(true);

    MergeCommitDto mergeCommit = createMergeCommit(true);
    service.merge(REPOSITORY.getNamespaceAndName(), "1", mergeCommit, MergeStrategy.MERGE_COMMIT, false);

    verify(branchCommandBuilder).delete("squash");
  }

  @Test
  void shouldUpdatePullRequestStatus() throws IOException {
    mocksForPullRequestUpdate("master");
    mockPullRequest("squash", "master", "1");

    MergeCommitDto mergeCommit = createMergeCommit(true);
    service.merge(REPOSITORY.getNamespaceAndName(), "1", mergeCommit, MergeStrategy.MERGE_COMMIT, false);

    verify(pullRequestService).setMerged(REPOSITORY, "1");
  }

  @Test
  void shouldThrowExceptionIfStrategyNotSupported() {
    when(mergeCommandBuilder.isSupported(MergeStrategy.SQUASH)).thenReturn(false);
    mockPullRequest("squash", "master", "1");

    MergeCommitDto mergeCommit = createMergeCommit(false);

    NamespaceAndName namespaceAndName = REPOSITORY.getNamespaceAndName();
    assertThrows(MergeStrategyNotSupportedException.class,
      () -> service.merge(namespaceAndName, "1", mergeCommit, MergeStrategy.SQUASH, false));
  }

  @Test
  void shouldThrowExceptionIfObstacleExists() {
    PullRequest pullRequest = mockPullRequest("squash", "master", "1");

    mockMergeGuard(pullRequest, false);

    MergeCommitDto mergeCommit = createMergeCommit(false);

    NamespaceAndName namespaceAndName = REPOSITORY.getNamespaceAndName();
    assertThrows(MergeNotAllowedException.class,
      () -> service.merge(namespaceAndName, "1", mergeCommit, MergeStrategy.SQUASH, false));
  }

  @Test
  void shouldNotThrowExceptionIfObstacleIsOverrideable() throws IOException {
    mocksForPullRequestUpdate("master");
    PullRequest pullRequest = mockPullRequest("squash", "master", "1");
    mockMergeGuard(pullRequest, true);

    MergeCommitDto mergeCommit = createMergeCommit(true);
    service.merge(REPOSITORY.getNamespaceAndName(), "1", mergeCommit, MergeStrategy.MERGE_COMMIT, true);

    verify(pullRequestService).setEmergencyMerged(any(Repository.class), anyString(), anyString(), anyList());
  }

  @Test
  void shouldThrowExceptionWhenPullRequestIsNotOpen() {
    lenient().when(mergeCommandBuilder.isSupported(MergeStrategy.SQUASH)).thenReturn(true);
    mockPullRequest("squash", "master", "1", REJECTED);

    MergeCommitDto mergeCommit = createMergeCommit(false);

    NamespaceAndName namespaceAndName = REPOSITORY.getNamespaceAndName();
    assertThrows(CannotMergeNotOpenPullRequestException.class,
      () -> service.merge(namespaceAndName, "1", mergeCommit, MergeStrategy.SQUASH, false));
  }

  @Test
  void shouldDoDryRun() {
    when(subject.isPermitted("repository:readPullRequest:" + REPOSITORY.getId())).thenReturn(true);
    mockPullRequest("mergeable", "master", "1");
    when(mergeCommandBuilder.dryRun()).thenReturn(new MergeDryRunCommandResult(true));

    MergeCheckResult mergeCheckResult = service.checkMerge(REPOSITORY.getNamespaceAndName(), "1");

    verify(mergeCommandBuilder).dryRun();
    assertThat(mergeCheckResult.hasConflicts()).isFalse();
  }

  @Test
  void shouldNotDoDryRunIfMissingPermission() {
    mockPullRequest("mergable", "master", "1");

    MergeCheckResult mergeCheckResult = service.checkMerge(REPOSITORY.getNamespaceAndName(), "1");

    assertThat(mergeCheckResult.hasConflicts()).isTrue();
  }

  @Nested
  class WithSquashMerge {

    private final DisplayUser pullRequestAuthor = DisplayUser.from(new User("zaphod", "Zaphod Beeblebrox", "zaphod@hitchhiker.com"));
    private final String pullRequestTitle = "Replace variable X with Y";
    private PullRequest pullRequest;

    @BeforeEach
    void preparePullRequest() throws IOException {
      pullRequest = createPullRequest();
      pullRequest.setAuthor("zaphod");
      pullRequest.setTitle(pullRequestTitle);

      when(pullRequestService.get(REPOSITORY.getNamespace(), REPOSITORY.getName(), "1")).thenReturn(pullRequest);

      when(userDisplayManager.get("zaphod")).thenReturn(Optional.of(pullRequestAuthor));
      Person pullRequestAuthor = new Person("Zaphod Beeblebrox", "zaphod@hitchhiker.com");

      Changeset changesetWithContributor = new Changeset("2", 2L, pullRequestAuthor, "second commit\nwith multiple lines");
      changesetWithContributor.addContributor(new Contributor(Contributor.CO_AUTHORED_BY, new Person("Ford", "prefect@hitchhiker.org")));
      changesetWithContributor.addContributor(new Contributor(Contributor.COMMITTED_BY, new Person("Marvin", "marvin@example.org")));
      ChangesetPagingResult changesets = new ChangesetPagingResult(3, asList(
        new Changeset("3", 3L, new Person("Arthur", "dent@hitchhiker.com"), "third commit"),
        changesetWithContributor,
        new Changeset("1", 1L, pullRequestAuthor, "first commit")
      ));
    }

    @Nested
    class WithCurrentUserNotPullRequestAuthor {

      private User user;

      @BeforeEach
      void setCurrentUser() {
        user = mockUser("Phil", "Phil Groundhog", "phil@groundhog.com");
      }

      @Test
      void shouldUseCurrentUserAsAuthorWhenPullRequestAuthorIsUnknown() {
        when(userDisplayManager.get("zaphod")).thenReturn(empty());

        CommitDefaults commitDefaults = service.createCommitDefaults(REPOSITORY.getNamespaceAndName(), "1", MergeStrategy.SQUASH);

        assertThat(commitDefaults.getCommitAuthor())
          .usingRecursiveComparison()
          .isEqualTo(DisplayUser.from(user));
      }
    }

    @Test
    void shouldHaveAuthorFromPullRequest() {
      CommitDefaults commitDefaults = service.createCommitDefaults(REPOSITORY.getNamespaceAndName(), "1", MergeStrategy.SQUASH);

      assertThat(commitDefaults.getCommitAuthor())
        .usingRecursiveComparison()
        .isEqualTo(pullRequestAuthor);
    }

    @Test
    void shouldTakeCommitMessageFromService() {
      when(mergeCommitMessageService.determineDefaultMessage(REPOSITORY.getNamespaceAndName(), pullRequest, MergeStrategy.SQUASH))
        .thenReturn("Great message");

      CommitDefaults commitDefaults = service.createCommitDefaults(REPOSITORY.getNamespaceAndName(), "1", MergeStrategy.SQUASH);

      assertThat(commitDefaults.getCommitMessage()).isEqualTo("Great message");
    }
  }

  @Test
  void shouldOmitAuthorForNonSquash() {
    PullRequest pullRequest = createPullRequest();
    when(pullRequestService.get(REPOSITORY.getNamespace(), REPOSITORY.getName(), "1")).thenReturn(pullRequest);

    CommitDefaults commitDefaults = service.createCommitDefaults(REPOSITORY.getNamespaceAndName(), "1", MergeStrategy.MERGE_COMMIT);

    assertThat(commitDefaults.getCommitAuthor()).isNull();
  }

  @Test
  void shouldForwardObstaclesFromGuards() {
    PullRequest pullRequest = mockPullRequest("mergeable", "master", "1");

    TestMergeObstacle obstacle1 = mockMergeGuard(pullRequest, false);
    TestMergeObstacle obstacle2 = mockMergeGuard(pullRequest, false);

    MergeCheckResult mergeCheckResult = service.checkMerge(REPOSITORY.getNamespaceAndName(), "1");

    assertThat(mergeCheckResult.getMergeObstacles()).contains(obstacle1, obstacle2);
  }

  @Test
  void shouldReturnCorrectDisabledCommitMessageStatus() {
    assertThat(service.isCommitMessageDisabled(MergeStrategy.REBASE)).isTrue();
    assertThat(service.isCommitMessageDisabled(MergeStrategy.SQUASH)).isFalse();
    assertThat(service.isCommitMessageDisabled(MergeStrategy.MERGE_COMMIT)).isFalse();
    assertThat(service.isCommitMessageDisabled(MergeStrategy.FAST_FORWARD_IF_POSSIBLE)).isFalse();
  }

  @Test
  void shouldReturnCorrectCommitMessageHint() {
    assertThat(service.createMergeCommitMessageHint(MergeStrategy.REBASE)).isNullOrEmpty();
    assertThat(service.createMergeCommitMessageHint(MergeStrategy.SQUASH)).isNullOrEmpty();
    assertThat(service.createMergeCommitMessageHint(MergeStrategy.MERGE_COMMIT)).isNullOrEmpty();
    assertThat(service.createMergeCommitMessageHint(MergeStrategy.FAST_FORWARD_IF_POSSIBLE)).isEqualTo(MergeStrategy.FAST_FORWARD_IF_POSSIBLE.name());
  }

  @Test
  void shouldClosePullRequestsWithDeletedBranchesOnMerge() {
    PullRequest pullRequest = mockPullRequest("commit", "master", "1");
    PullRequest pullRequest2 = mockPullRequest("master", "commit", "2");
    when(mergeCommandBuilder.isSupported(MergeStrategy.MERGE_COMMIT)).thenReturn(true);
    when(mergeCommandBuilder.executeMerge()).thenReturn(MergeCommandResult.success("1", "2", "123"));
    when(repositoryService.isSupported(Command.BRANCH)).thenReturn(true);
    BranchCommandBuilder branchCommand = mock(BranchCommandBuilder.class);
    when(repositoryService.getBranchCommand()).thenReturn(branchCommand);


    when(pullRequestService.getAll(REPOSITORY.getNamespace(), REPOSITORY.getName())).thenReturn(ImmutableList.of(pullRequest, pullRequest2));

    MergeCommitDto mergeCommit = createMergeCommit(true);
    service.merge(REPOSITORY.getNamespaceAndName(), pullRequest.getId(), mergeCommit, MergeStrategy.MERGE_COMMIT, false);

    verify(branchCommand).delete(pullRequest.getSource());
    verify(pullRequestService, times(1)).setRejected(REPOSITORY, "2", PullRequestRejectedEvent.RejectionCause.SOURCE_BRANCH_DELETED);
    verify(pullRequestService, never()).setRejected(REPOSITORY, "1", PullRequestRejectedEvent.RejectionCause.SOURCE_BRANCH_DELETED);
  }

  private TestMergeObstacle mockMergeGuard(PullRequest pullRequest, boolean overrideable) {
    MergeGuard mergeGuard = mock(MergeGuard.class);
    mergeGuards.add(mergeGuard);
    TestMergeObstacle obstacle = new TestMergeObstacle(overrideable);
    when(mergeGuard.getObstacles(REPOSITORY, pullRequest)).thenReturn(singleton(obstacle));
    return obstacle;
  }

  private PullRequest createPullRequest() {
    PullRequest pullRequest = new PullRequest();
    pullRequest.setId("1");
    pullRequest.setSource("squash");
    pullRequest.setTarget("master");
    pullRequest.setStatus(OPEN);
    return pullRequest;
  }

  private PullRequest mockPullRequest(String source, String target, String pullRequestId) {
    return mockPullRequest(source, target, pullRequestId, OPEN);
  }

  private PullRequest mockPullRequest(String source, String target, String pullRequestId, PullRequestStatus status) {
    PullRequest pr = new PullRequest();
    pr.setId(pullRequestId);
    pr.setSource(source);
    pr.setTarget(target);
    pr.setStatus(status);
    lenient().when(pullRequestService.get(REPOSITORY, pullRequestId)).thenReturn(pr);
    return pr;
  }

  private MergeCommitDto createMergeCommit(boolean deleteBranch) {
    MergeCommitDto mergeCommit = new MergeCommitDto();
    mergeCommit.setShouldDeleteSourceBranch(deleteBranch);
    mergeCommit.setOverrideMessage("override");
    return mergeCommit;
  }

  private void mocksForPullRequestUpdate(String branchName) throws IOException {
    lenient().when(repositoryService.isSupported(Command.BRANCH)).thenReturn(true);
    lenient().when(repositoryService.isSupported(Command.BRANCHES)).thenReturn(true);
    when(mergeCommandBuilder.isSupported(MergeStrategy.MERGE_COMMIT)).thenReturn(true);
    when(mergeCommandBuilder.executeMerge()).thenReturn(MergeCommandResult.success("1", "2", "123"));
    when(repositoryService.getBranchCommand()).thenReturn(branchCommandBuilder);
    Branches branches = new Branches();
    branches.setBranches(ImmutableList.of(Branch.normalBranch(branchName, "123")));
    when(branchesCommandBuilder.getBranches()).thenReturn(branches);
  }

  private User mockUser(String name, String displayName, String mail) {
    User user = new User(name, displayName, mail);
    when(subject.getPrincipals().oneByType(User.class)).thenReturn(user);
    return user;
  }

  private static class TestMergeObstacle implements MergeObstacle {

    private final boolean overridable;

    private TestMergeObstacle(boolean overridable) {
      this.overridable = overridable;
    }

    @Override

    public String getMessage() {
      return "not permitted";
    }

    @Override
    public String getKey() {
      return "key";
    }

    @Override
    public boolean isOverrideable() {
      return overridable;
    }
  }
}
