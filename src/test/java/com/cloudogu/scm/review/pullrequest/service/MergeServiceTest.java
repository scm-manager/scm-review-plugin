package com.cloudogu.scm.review.pullrequest.service;

import com.cloudogu.scm.review.BranchResolver;
import com.cloudogu.scm.review.pullrequest.dto.DisplayedUserDto;
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

import static com.cloudogu.scm.review.pullrequest.service.PullRequestStatus.OPEN;
import static com.cloudogu.scm.review.pullrequest.service.PullRequestStatus.REJECTED;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
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
  private BranchResolver branchResolver;

  private MergeService service;

  @Before
  public void initService() {
    service = new MergeService(serviceFactory, pullRequestService, branchResolver);
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
    when(mergeCommandBuilder.executeMerge()).thenReturn(new MergeCommandResult(emptyList(), "123"));
    when(branchResolver.resolve(any(), any())).thenReturn(Branch.normalBranch("master", "123"));
    PullRequest pullRequest = mockPullRequest("squash", "master", "1");

    MergeCommitDto mergeCommit = createMergeCommit(false);
    service.merge(REPOSITORY.getNamespaceAndName(), "1", mergeCommit, MergeStrategy.SQUASH);
    assertThat(pullRequest.getTargetRevision()).isEqualTo("123");
    assertThat(pullRequest.getSourceRevision()).isEqualTo("123");
  }

  @Test(expected = UnauthorizedException.class)
  @SubjectAware(username = "trillian")
  public void shouldNotMergeWithoutPermission() {
    when(branchResolver.resolve(any(), any())).thenReturn(Branch.normalBranch("master", "123"));
    mockPullRequest("squash", "master", "1");

    MergeCommitDto mergeCommit = createMergeCommit(false);
    service.merge(REPOSITORY.getNamespaceAndName(), "1", mergeCommit, MergeStrategy.SQUASH);
  }

  @Test
  @SubjectAware(username = "dent", password = "secret")
  public void shouldDeleteBranchIfFlagIsSet() {
    when(mergeCommandBuilder.isSupported(MergeStrategy.SQUASH)).thenReturn(true);
    when(mergeCommandBuilder.executeMerge()).thenReturn(new MergeCommandResult(emptyList(), "123"));
    when(branchResolver.resolve(any(), any())).thenReturn(Branch.normalBranch("master", "123"));
    mockPullRequest("squash", "master", "1");
    when(repositoryService.getBranchCommand()).thenReturn(branchCommandBuilder);
    when(repositoryService.isSupported(Command.BRANCH)).thenReturn(true);

    MergeCommitDto mergeCommit = createMergeCommit(true);
    service.merge(REPOSITORY.getNamespaceAndName(), "1", mergeCommit, MergeStrategy.SQUASH);

    verify(branchCommandBuilder).delete("squash");
  }

  @Test
  @SubjectAware(username = "dent", password = "secret")
  public void shouldUpdatePullRequestStatus() throws IOException {
    mocksForPullRequestUpdate("master");
    PullRequest pullRequest = mockPullRequest("squash", "master", "1");

    MergeCommitDto mergeCommit = createMergeCommit(true);
    service.merge(REPOSITORY.getNamespaceAndName(), "1", mergeCommit, MergeStrategy.MERGE_COMMIT);

    verify(pullRequestService).setMerged(REPOSITORY, "1");
  }

  @Test
  @SubjectAware(username = "dent", password = "secret")
  public void shouldThrowExceptionIfStrategyNotSupported() {
    when(mergeCommandBuilder.isSupported(MergeStrategy.SQUASH)).thenReturn(false);
    when(branchResolver.resolve(any(), any())).thenReturn(Branch.normalBranch("master", "123"));
    mockPullRequest("squash", "master", "1");

    MergeCommitDto mergeCommit = createMergeCommit(false);

    assertThrows(MergeStrategyNotSupportedException.class, () -> service.merge(REPOSITORY.getNamespaceAndName(), "1", mergeCommit, MergeStrategy.SQUASH));
  }

  @Test
  @SubjectAware(username = "dent", password = "secret")
  public void shouldThrowExceptionWhenPullRequestIsNotOpen() {
    lenient().when(mergeCommandBuilder.isSupported(MergeStrategy.SQUASH)).thenReturn(true);
    when(branchResolver.resolve(any(), any())).thenReturn(Branch.normalBranch("master", "123"));
    PullRequest pullRequest = mockPullRequest("squash", "master", "1", REJECTED);

    MergeCommitDto mergeCommit = createMergeCommit(false);

    assertThrows(CannotMergeNotOpenPullRequestException.class, () -> service.merge(REPOSITORY.getNamespaceAndName(), "1", mergeCommit, MergeStrategy.SQUASH));
  }

  @Test
  @SubjectAware(username = "dent", password = "secret")
  public void shouldDoDryRun() {
    mockPullRequest("mergeable", "master", "1");
    when(mergeCommandBuilder.dryRun()).thenReturn(new MergeDryRunCommandResult(true));

    MergeDryRunCommandResult mergeDryRunCommandResult = service.dryRun(REPOSITORY.getNamespaceAndName(), "1");

    assertThat(mergeDryRunCommandResult.isMergeable()).isTrue();
  }

  @Test
  @SubjectAware(username = "trillian", password = "secret")
  public void shouldNotDoDryRunIfMissingPermission() {
    mockPullRequest("mergable", "master", "1");

    MergeDryRunCommandResult mergeDryRunCommandResult = service.dryRun(REPOSITORY.getNamespaceAndName(), "1");

    assertThat(mergeDryRunCommandResult.isMergeable()).isFalse();
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

    Person author = new Person("Philip");

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
      "- second commit\n" +
      "with multiple lines\n" +
      "\n" +
      "- third commit\n"
    );
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
    mergeCommit.setAuthor(new DisplayedUserDto("philip", "Philip J Fry", "philip@fry.com"));
    mergeCommit.setShouldDeleteSourceBranch(deleteBranch);
    return mergeCommit;
  }

  private void mocksForPullRequestUpdate(String branchName) throws IOException {
    lenient().when(repositoryService.isSupported(Command.BRANCH)).thenReturn(true);
    lenient().when(repositoryService.isSupported(Command.BRANCHES)).thenReturn(true);
    when(mergeCommandBuilder.isSupported(MergeStrategy.MERGE_COMMIT)).thenReturn(true);
    when(mergeCommandBuilder.executeMerge()).thenReturn(new MergeCommandResult(emptyList(), "123"));
    when(repositoryService.getBranchCommand()).thenReturn(branchCommandBuilder);
    Branches branches = new Branches();
    branches.setBranches(ImmutableList.of(Branch.normalBranch(branchName, "123")));
    when(branchesCommandBuilder.getBranches()).thenReturn(branches);
  }
}
