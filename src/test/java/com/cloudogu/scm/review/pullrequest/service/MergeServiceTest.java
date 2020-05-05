/*
 * MIT License
 *
 * Copyright (c) 2020-present Cloudogu GmbH and Contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.cloudogu.scm.review.pullrequest.service;

import com.cloudogu.scm.review.BranchProtectionHook;
import com.cloudogu.scm.review.pullrequest.dto.MergeCommitDto;
import com.github.sdorra.shiro.ShiroRule;
import com.github.sdorra.shiro.SubjectAware;
import com.google.common.collect.ImmutableList;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import sonia.scm.repository.Branch;
import sonia.scm.repository.Branches;
import sonia.scm.repository.Changeset;
import sonia.scm.repository.ChangesetPagingResult;
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

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import static com.cloudogu.scm.review.pullrequest.service.PullRequestStatus.OPEN;
import static com.cloudogu.scm.review.pullrequest.service.PullRequestStatus.REJECTED;
import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
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
public class MergeServiceTest {

  @Rule
  public ShiroRule shiroRule = new ShiroRule();

  private final Repository REPOSITORY = RepositoryTestData.createHeartOfGold();

  @Mock
  BranchCommandBuilder branchCommandBuilder;
  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  BranchesCommandBuilder branchesCommandBuilder;
  @Mock
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
  private BranchProtectionHook branchProtectionHook;

  private Set<MergeGuard> mergeGuards = new HashSet<>();

  private MergeService service;

  @Before
  public void initService() {
    service = new MergeService(serviceFactory, pullRequestService, mergeGuards, branchProtectionHook);
    doAnswer(invocation -> {
      invocation.<Runnable>getArgument(0).run();
      return null;
    })
      .when(branchProtectionHook).runPrivileged(any());
  }

  @Before
  public void initMocks() {
    when(serviceFactory.create(REPOSITORY.getNamespaceAndName())).thenReturn(repositoryService);
    when(repositoryService.getRepository()).thenReturn(REPOSITORY);
    lenient().when(repositoryService.getMergeCommand()).thenReturn(mergeCommandBuilder);
  }

  @Test
  @SubjectAware(username = "dent", password = "secret")
  public void shouldMergeSuccessfully() {
    when(mergeCommandBuilder.isSupported(MergeStrategy.SQUASH)).thenReturn(true);
    when(mergeCommandBuilder.executeMerge()).thenReturn(MergeCommandResult.success("1", "2", "123"));
    mockPullRequest("squash", "master", "1");

    MergeCommitDto mergeCommit = createMergeCommit(false);
    service.merge(REPOSITORY.getNamespaceAndName(), "1", mergeCommit, MergeStrategy.SQUASH, false);
    verify(pullRequestService).setRevisions(REPOSITORY, "1", "1", "2");
    verify(pullRequestService, never()).setEmergencyMerged(any(Repository.class), anyString(), anyString(), anyList());
  }

  @Test
  @SubjectAware(username = "dent", password = "secret")
  public void shouldEmergencyMergeSuccessfully() {
    when(mergeCommandBuilder.isSupported(MergeStrategy.SQUASH)).thenReturn(true);
    when(mergeCommandBuilder.executeMerge()).thenReturn(MergeCommandResult.success("1", "2", "123"));
    mockPullRequest("squash", "master", "1");

    MergeCommitDto mergeCommit = createMergeCommit(false);
    service.merge(REPOSITORY.getNamespaceAndName(), "1", mergeCommit, MergeStrategy.SQUASH, true);

    verify(pullRequestService).setEmergencyMerged(REPOSITORY, "1", mergeCommit.getOverrideMessage(), mergeCommit.getIgnoredMergeObstacles());
    verify(pullRequestService, never()).setMerged(REPOSITORY, "1", mergeCommit.getOverrideMessage());
  }

  @Test(expected = UnauthorizedException.class)
  @SubjectAware(username = "trillian")
  public void shouldNotEmergencyMergeWithoutPermission() {
    mockPullRequest("squash", "master", "1");

    MergeCommitDto mergeCommit = createMergeCommit(false);
    service.merge(REPOSITORY.getNamespaceAndName(), "1", mergeCommit, MergeStrategy.SQUASH, true);
  }

  @Test(expected = MergeNotAllowedException.class)
  @SubjectAware(username = "dent")
  public void shouldNotMergeWithObstaclesIfNotEmergency() {
    when(mergeCommandBuilder.isSupported(MergeStrategy.SQUASH)).thenReturn(true);
    when(mergeCommandBuilder.executeMerge()).thenReturn(MergeCommandResult.success("1", "2", "123"));
    PullRequest pullRequest = mockPullRequest("squash", "master", "1");
    mockMergeGuard(pullRequest, true);
    MergeCommitDto mergeCommit = createMergeCommit(false);

    service.merge(REPOSITORY.getNamespaceAndName(), "1", mergeCommit, MergeStrategy.SQUASH, false);
  }

  @Test(expected = UnauthorizedException.class)
  @SubjectAware(username = "trillian")
  public void shouldNotMergeWithoutPermission() {
    mockPullRequest("squash", "master", "1");

    MergeCommitDto mergeCommit = createMergeCommit(false);
    service.merge(REPOSITORY.getNamespaceAndName(), "1", mergeCommit, MergeStrategy.SQUASH, false);
  }

  @Test
  @SubjectAware(username = "dent", password = "secret")
  public void shouldDeleteBranchIfFlagIsSet() {
    when(mergeCommandBuilder.isSupported(MergeStrategy.SQUASH)).thenReturn(true);
    when(mergeCommandBuilder.executeMerge()).thenReturn(MergeCommandResult.success("1", "2", "123"));
    mockPullRequest("squash", "master", "1");
    when(repositoryService.getBranchCommand()).thenReturn(branchCommandBuilder);
    when(repositoryService.isSupported(Command.BRANCH)).thenReturn(true);

    MergeCommitDto mergeCommit = createMergeCommit(true);
    service.merge(REPOSITORY.getNamespaceAndName(), "1", mergeCommit, MergeStrategy.SQUASH, false);

    verify(branchCommandBuilder).delete("squash");
  }

  @Test
  @SubjectAware(username = "dent", password = "secret")
  public void shouldUpdatePullRequestStatus() throws IOException {
    mocksForPullRequestUpdate("master");
    mockPullRequest("squash", "master", "1");

    MergeCommitDto mergeCommit = createMergeCommit(true);
    service.merge(REPOSITORY.getNamespaceAndName(), "1", mergeCommit, MergeStrategy.MERGE_COMMIT, false);

    verify(pullRequestService).setMerged(REPOSITORY, "1", "override");
  }

  @Test
  @SubjectAware(username = "dent", password = "secret")
  public void shouldThrowExceptionIfStrategyNotSupported() {
    when(mergeCommandBuilder.isSupported(MergeStrategy.SQUASH)).thenReturn(false);
    mockPullRequest("squash", "master", "1");

    MergeCommitDto mergeCommit = createMergeCommit(false);

    assertThrows(MergeStrategyNotSupportedException.class,
      () -> service.merge(REPOSITORY.getNamespaceAndName(), "1", mergeCommit, MergeStrategy.SQUASH, false));
  }

  @Test
  @SubjectAware(username = "dent", password = "secret")
  public void shouldThrowExceptionIfObstacleExists() {
    PullRequest pullRequest = mockPullRequest("squash", "master", "1");

    mockMergeGuard(pullRequest, false);

    MergeCommitDto mergeCommit = createMergeCommit(false);

    assertThrows(MergeNotAllowedException.class,
      () -> service.merge(REPOSITORY.getNamespaceAndName(), "1", mergeCommit, MergeStrategy.SQUASH, false));
  }

  @Test
  @SubjectAware(username = "dent", password = "secret")
  public void shouldNotThrowExceptionIfObstacleIsOverrideable() throws IOException {
    mocksForPullRequestUpdate("master");
    PullRequest pullRequest = mockPullRequest("squash", "master", "1");
    mockMergeGuard(pullRequest, true);

    MergeCommitDto mergeCommit = createMergeCommit(true);
    service.merge(REPOSITORY.getNamespaceAndName(), "1", mergeCommit, MergeStrategy.MERGE_COMMIT, false);

    verify(pullRequestService).setMerged(REPOSITORY, "1", "override");
  }

  @Test
  @SubjectAware(username = "dent", password = "secret")
  public void shouldThrowExceptionWhenPullRequestIsNotOpen() {
    lenient().when(mergeCommandBuilder.isSupported(MergeStrategy.SQUASH)).thenReturn(true);
    mockPullRequest("squash", "master", "1", REJECTED);

    MergeCommitDto mergeCommit = createMergeCommit(false);

    assertThrows(CannotMergeNotOpenPullRequestException.class,
      () -> service.merge(REPOSITORY.getNamespaceAndName(), "1", mergeCommit, MergeStrategy.SQUASH, false));
  }

  @Test
  @SubjectAware(username = "dent", password = "secret")
  public void shouldDoDryRun() {
    mockPullRequest("mergeable", "master", "1");
    when(mergeCommandBuilder.dryRun()).thenReturn(new MergeDryRunCommandResult(true));

    MergeCheckResult mergeCheckResult = service.checkMerge(REPOSITORY.getNamespaceAndName(), "1");

    assertThat(mergeCheckResult.hasConflicts()).isFalse();
  }

  @Test
  @SubjectAware(username = "trillian", password = "secret")
  public void shouldNotDoDryRunIfMissingPermission() {
    mockPullRequest("mergable", "master", "1");

    MergeCheckResult mergeCheckResult = service.checkMerge(REPOSITORY.getNamespaceAndName(), "1");

    assertThat(mergeCheckResult.hasConflicts()).isTrue();
  }

  @Test
  @SubjectAware(username = "trillian", password = "secret")
  public void shouldCreateCommitMessageForSquash() throws IOException {
    when(repositoryService.isSupported(Command.LOG)).thenReturn(true);
    when(repositoryService.getLogCommand()).thenReturn(logCommandBuilder);
    when(logCommandBuilder.setBranch(any())).thenReturn(logCommandBuilder);
    when(logCommandBuilder.setAncestorChangeset(any())).thenReturn(logCommandBuilder);
    PullRequest pullRequest = createPullRequest();
    when(pullRequestService.get(REPOSITORY.getNamespace(), REPOSITORY.getName(), "1")).thenReturn(pullRequest);

    Person author = new Person("Philip", "phil@groundhog.com");

    ChangesetPagingResult changesets = new ChangesetPagingResult(3, asList(
      new Changeset("1", 1L, author, "first commit"),
      new Changeset("2", 2L, author, "second commit\nwith multiple lines"),
      new Changeset("3", 3L, author, "third commit")
    ));

    when(logCommandBuilder.getChangesets()).thenReturn(changesets);
    String message = service.createDefaultCommitMessage(REPOSITORY.getNamespaceAndName(), "1", MergeStrategy.SQUASH);
    assertThat(message).isEqualTo("Squash commits of branch squash:\n" +
      "\n" +
      "- first commit\n" +
      "  Author: Philip <phil@groundhog.com>\n" +
      "- second commit\n" +
      "with multiple lines\n" +
      "\n" +
      "Author: Philip <phil@groundhog.com>\n" +
      "\n" +
      "- third commit\n" +
      "  Author: Philip <phil@groundhog.com>\n"
    );
  }

  @Test
  @SubjectAware(username = "trillian", password = "secret")
  public void shouldForwardObstaclesFromGuards() {
    PullRequest pullRequest = mockPullRequest("mergeable", "master", "1");

    TestMergeObstacle obstacle1 = mockMergeGuard(pullRequest, false);
    TestMergeObstacle obstacle2 = mockMergeGuard(pullRequest, false);

    MergeCheckResult mergeCheckResult = service.checkMerge(REPOSITORY.getNamespaceAndName(), "1");

    assertThat(mergeCheckResult.getMergeObstacles()).contains(obstacle1, obstacle2);
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
    when(pullRequestService.get(REPOSITORY, "1")).thenReturn(pr);
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
