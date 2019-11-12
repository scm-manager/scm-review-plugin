package com.cloudogu.scm.review.pullrequest.service;

import com.cloudogu.scm.review.comment.service.CommentService;
import com.cloudogu.scm.review.pullrequest.dto.DisplayedUserDto;
import com.cloudogu.scm.review.pullrequest.dto.MergeCommitDto;
import com.github.sdorra.shiro.ShiroRule;
import com.github.sdorra.shiro.SubjectAware;
import com.google.common.collect.ImmutableList;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.junit.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.api.v2.resources.MergeCommandDto;
import sonia.scm.event.ScmEventBus;
import sonia.scm.repository.Branch;
import sonia.scm.repository.Branches;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryTestData;
import sonia.scm.repository.api.BranchCommandBuilder;
import sonia.scm.repository.api.BranchesCommandBuilder;
import sonia.scm.repository.api.Command;
import sonia.scm.repository.api.MergeCommandBuilder;
import sonia.scm.repository.api.MergeCommandResult;
import sonia.scm.repository.api.MergeDryRunCommandResult;
import sonia.scm.repository.api.MergeStrategy;
import sonia.scm.repository.api.MergeStrategyNotSupportedException;
import sonia.scm.repository.api.RepositoryService;
import sonia.scm.repository.api.RepositoryServiceFactory;
import sonia.scm.user.User;

import java.io.IOException;

import static com.cloudogu.scm.review.pullrequest.service.PullRequestStatus.MERGED;
import static com.cloudogu.scm.review.pullrequest.service.PullRequestStatus.OPEN;
import static com.cloudogu.scm.review.pullrequest.service.PullRequestStatus.REJECTED;
import static java.time.Instant.now;
import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SubjectAware(
  configuration = "classpath:com/cloudogu/scm/review/shiro.ini",
  password = "secret"
)
@ExtendWith(MockitoExtension.class)
class MergeServiceTest {

  @Rule
  public ShiroRule shiroRule = new ShiroRule();
  private final Subject subject = mock(Subject.class);

  private final Repository REPOSITORY = RepositoryTestData.createHeartOfGold();

  @Mock
  BranchCommandBuilder branchCommandBuilder;
  @Mock (answer = Answers.RETURNS_DEEP_STUBS)
  BranchesCommandBuilder branchesCommandBuilder;
  @Mock
  private RepositoryServiceFactory serviceFactory;
  @Mock
  private RepositoryService repositoryService;
  @Mock
  private PullRequestService pullRequestService;
  @Mock
  private CommentService commentService;
  @Mock
  private ScmEventBus scmEventBus;
  @Mock
  private MergeCommandBuilder mergeCommandBuilder;

  private MergeService service;

  @BeforeEach
  void initService() {
    service = new MergeService(serviceFactory, pullRequestService, commentService, scmEventBus);
  }

  @BeforeEach
  void initMocks() {
    mockPrincipal();

    when(serviceFactory.create(REPOSITORY.getNamespaceAndName())).thenReturn(repositoryService);
    when(repositoryService.getRepository()).thenReturn(REPOSITORY);
    lenient().when(repositoryService.getMergeCommand()).thenReturn(mergeCommandBuilder);
  }

  @Test
  @SubjectAware(username = "trillian", password = "secret")
  void shouldMergeSuccessfully() {
    when(mergeCommandBuilder.isSupported(MergeStrategy.SQUASH)).thenReturn(true);
    when(mergeCommandBuilder.executeMerge()).thenReturn(MergeCommandResult.success());
    mockPullRequest("squash", "master", "1");

    MergeCommitDto mergeCommit = createMergeCommit(false);
    service.merge(REPOSITORY.getNamespaceAndName(), "1", mergeCommit, MergeStrategy.SQUASH);
  }

  @Test
  @SubjectAware(username = "trillian", password = "secret")
  void shouldCloseBranchIfFlagIsSet() {
    when(mergeCommandBuilder.isSupported(MergeStrategy.SQUASH)).thenReturn(true);
    when(mergeCommandBuilder.executeMerge()).thenReturn(MergeCommandResult.success());
    mockPullRequest("squash", "master", "1");
    when(repositoryService.getBranchCommand()).thenReturn(branchCommandBuilder);
    when(repositoryService.isSupported(Command.BRANCH)).thenReturn(true);

    MergeCommitDto mergeCommit = createMergeCommit(true);
    service.merge(REPOSITORY.getNamespaceAndName(), "1", mergeCommit, MergeStrategy.SQUASH);

    verify(branchCommandBuilder).delete("squash");
  }

  @Test
  @SubjectAware(username = "trillian", password = "secret")
  void shouldUpdatePullRequestStatus() throws IOException {
    mocksForPullRequestUpdate("master");
    PullRequest pullRequest = mockPullRequest("squash", "master", "1");

    MergeCommitDto mergeCommit = createMergeCommit(true);
    service.merge(REPOSITORY.getNamespaceAndName(), "1", mergeCommit, MergeStrategy.MERGE_COMMIT);

    verify(pullRequestService).setStatus(REPOSITORY, pullRequest, MERGED);
    verify(commentService).addStatusChangedComment(eq(REPOSITORY), eq("1"), any());
  }

  @Test
  @SubjectAware(username = "trillian", password = "secret")
  void shouldNotUpdatePullRequestStatusIfDeletionFailed() throws IOException {
    mocksForPullRequestUpdate("squash");
    PullRequest pullRequest = mockPullRequest("squash", "master", "1");
    when(pullRequest.getStatus()).thenReturn(OPEN, MERGED);

    MergeCommitDto mergeCommit = createMergeCommit(true);
    service.merge(REPOSITORY.getNamespaceAndName(), "1", mergeCommit, MergeStrategy.MERGE_COMMIT);

    verify(pullRequestService, never()).setStatus(any(), any(), any());
    verify(commentService, never()).addStatusChangedComment(any(), any(), any());
  }

  @Test
  @SubjectAware(username = "trillian", password = "secret")
  void shouldThrowExceptionIfStrategyNotSupported() {
    when(mergeCommandBuilder.isSupported(MergeStrategy.SQUASH)).thenReturn(false);
    mockPullRequest("squash", "master", "1");

    MergeCommitDto mergeCommit = createMergeCommit(false);

    assertThrows(MergeStrategyNotSupportedException.class, () -> service.merge(REPOSITORY.getNamespaceAndName(), "1", mergeCommit, MergeStrategy.SQUASH));
  }

  @Test
  @SubjectAware(username = "trillian", password = "secret")
  void shouldThrowExceptionWhenPullRequestIsNotOpen() {
    lenient().when(mergeCommandBuilder.isSupported(MergeStrategy.SQUASH)).thenReturn(true);
    PullRequest pullRequest = mockPullRequest("squash", "master", "1");
    when(pullRequest.getStatus()).thenReturn(REJECTED);

    MergeCommitDto mergeCommit = createMergeCommit(false);

    assertThrows(CannotMergeNotOpenPullRequestException.class, () -> service.merge(REPOSITORY.getNamespaceAndName(), "1", mergeCommit, MergeStrategy.SQUASH));
  }

  @Test
  @SubjectAware(username = "trillian", password = "secret")
  void shouldDoDryRun() {
    when(subject.isPermitted((String) any())).thenReturn(true);
    mockPullRequest("mergeable", "master", "1");
    when(mergeCommandBuilder.dryRun()).thenReturn(new MergeDryRunCommandResult(true));

    MergeDryRunCommandResult mergeDryRunCommandResult = service.dryRun(REPOSITORY.getNamespaceAndName(), "1");

    assertThat(mergeDryRunCommandResult.isMergeable()).isTrue();
  }

  @Test
  @SubjectAware(username = "trillian", password = "secret")
  void shouldNotDoDryRunIfMissingPermission() {
    when(subject.isPermitted((String) any())).thenReturn(false);
    mockPullRequest("mergable", "master", "1");

    MergeDryRunCommandResult mergeDryRunCommandResult = service.dryRun(REPOSITORY.getNamespaceAndName(), "1");

    assertThat(mergeDryRunCommandResult.isMergeable()).isFalse();
  }

  private PullRequest mockPullRequest(String source, String target, String pullRequestId) {
    PullRequest pullRequest = mock(PullRequest.class);
    lenient().when(pullRequest.getId()).thenReturn("1");
    lenient().when(pullRequest.getSource()).thenReturn(source);
    lenient().when(pullRequest.getTarget()).thenReturn(target);
    lenient().when(pullRequest.getStatus()).thenReturn(OPEN);
    when(pullRequestService.get(REPOSITORY, pullRequestId)).thenReturn(pullRequest);
    return pullRequest;
  }

  private MergeCommitDto createMergeCommit(boolean deleteBranch) {
    MergeCommitDto mergeCommit = new MergeCommitDto();
    mergeCommit.setAuthor(new DisplayedUserDto("philip", "Philip J Fry", "philip@fry.com"));
    mergeCommit.setShouldDeleteSourceBranch(deleteBranch);
    return mergeCommit;
  }

  private void mockPrincipal() {
    ThreadContext.bind(subject);
    PrincipalCollection principals = mock(PrincipalCollection.class);
    when(subject.getPrincipals()).thenReturn(principals);
    User user1 = new User();
    user1.setName("Philip J Fry");
    user1.setDisplayName("Philip");
  }

  private void mocksForPullRequestUpdate(String branchName) throws IOException {
    lenient().when(repositoryService.isSupported(Command.BRANCH)).thenReturn(true);
    lenient().when(repositoryService.isSupported(Command.BRANCHES)).thenReturn(true);
    when(mergeCommandBuilder.isSupported(MergeStrategy.MERGE_COMMIT)).thenReturn(true);
    when(mergeCommandBuilder.executeMerge()).thenReturn(MergeCommandResult.success());
    when(repositoryService.getBranchCommand()).thenReturn(branchCommandBuilder);
    Branches branches = new Branches();
    branches.setBranches(ImmutableList.of(Branch.normalBranch(branchName, "123")));
    when(branchesCommandBuilder.getBranches()).thenReturn(branches);
  }
}
