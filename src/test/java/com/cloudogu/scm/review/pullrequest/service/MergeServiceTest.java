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
import sonia.scm.user.User;

import java.io.IOException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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
  LogCommandBuilder logCommandBuilder;
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

    MergeCommitDto mergeCommit = createMergeCommit("squash", "master",false);
    MergeCommandResult result = service.merge(REPOSITORY.getNamespaceAndName(), mergeCommit, MergeStrategy.SQUASH);

    assertThat(result.isSuccess()).isTrue();
  }

  @Test
  @SubjectAware(username = "trillian", password = "secret")
  void shouldCloseBranchIfFlagIsSet() {
    when(mergeCommandBuilder.isSupported(MergeStrategy.SQUASH)).thenReturn(true);
    when(mergeCommandBuilder.executeMerge()).thenReturn(MergeCommandResult.success());
    when(repositoryService.getBranchCommand()).thenReturn(branchCommandBuilder);
    when(repositoryService.isSupported(Command.BRANCH)).thenReturn(true);

    MergeCommitDto mergeCommit = createMergeCommit("squash", "master",true);
    MergeCommandResult result = service.merge(REPOSITORY.getNamespaceAndName(), mergeCommit, MergeStrategy.SQUASH);

    verify(branchCommandBuilder).delete(mergeCommit.getSource());
    assertThat(result.isSuccess()).isTrue();
  }

  @Test
  @SubjectAware(username = "trillian", password = "secret")
  void shouldUpdatePullRequestStatus() throws IOException {
    mocksForPullRequestUpdate("master");
    when(pullRequestService.get(any(), any(), any(), any())).thenReturn(Optional.of(new PullRequest()));

    MergeCommitDto mergeCommit = createMergeCommit("squash", "master",true);
    service.merge(REPOSITORY.getNamespaceAndName(), mergeCommit, MergeStrategy.MERGE_COMMIT);

    verify(pullRequestService).setStatus(any(), any(), any());
    verify(commentService).addStatusChangedComment(any(), any(), any());
  }

  @Test
  @SubjectAware(username = "trillian", password = "secret")
  void shouldNotUpdatePullRequestStatusIfDeletionFailed() throws IOException {
    mocksForPullRequestUpdate("squash");

    MergeCommitDto mergeCommit = createMergeCommit("squash", "master",true);
    service.merge(REPOSITORY.getNamespaceAndName(), mergeCommit, MergeStrategy.MERGE_COMMIT);

    verify(pullRequestService, never()).setStatus(any(), any(), any());
    verify(commentService, never()).addStatusChangedComment(any(), any(), any());
  }

  @Test
  @SubjectAware(username = "trillian", password = "secret")
  void shouldThrowExceptionIfStrategyNotSupported() {
    when(mergeCommandBuilder.isSupported(MergeStrategy.SQUASH)).thenReturn(false);

    MergeCommitDto mergeCommit = createMergeCommit("squash", "master", false);

    assertThrows(MergeStrategyNotSupportedException.class, () -> service.merge(REPOSITORY.getNamespaceAndName(), mergeCommit, MergeStrategy.SQUASH));
  }

  @Test
  @SubjectAware(username = "trillian", password = "secret")
  void shouldDoDryRun() {
    when(subject.isPermitted((String) any())).thenReturn(true);
    when(mergeCommandBuilder.dryRun()).thenReturn(new MergeDryRunCommandResult(true));

    MergeCommandDto mergeCommandDto = new MergeCommandDto();
    mergeCommandDto.setSourceRevision("mergeable");
    mergeCommandDto.setTargetRevision("master");
    MergeDryRunCommandResult mergeDryRunCommandResult = service.dryRun(REPOSITORY.getNamespaceAndName(), mergeCommandDto);

    assertThat(mergeDryRunCommandResult.isMergeable()).isTrue();
  }

  @Test
  @SubjectAware(username = "trillian", password = "secret")
  void shouldNotDoDryRunIfMissingPermission() {
    when(subject.isPermitted((String) any())).thenReturn(false);

    MergeCommandDto mergeCommandDto = new MergeCommandDto();
    mergeCommandDto.setSourceRevision("mergeable");
    mergeCommandDto.setTargetRevision("master");
    MergeDryRunCommandResult mergeDryRunCommandResult = service.dryRun(REPOSITORY.getNamespaceAndName(), mergeCommandDto);

    assertThat(mergeDryRunCommandResult.isMergeable()).isFalse();
  }

  @Test
  @SubjectAware(username = "trillian", password = "secret")
  void shouldCreateCommitMessageForSquash() throws IOException {
    when(subject.isPermitted((String) any())).thenReturn(true);
    when(repositoryService.isSupported(Command.LOG)).thenReturn(true);
    when(repositoryService.getLogCommand()).thenReturn(logCommandBuilder);
    when(logCommandBuilder.setBranch(any())).thenReturn(logCommandBuilder);
    when(logCommandBuilder.setAncestorChangeset(any())).thenReturn(logCommandBuilder);

    Person author = new Person("Philip");
    Changeset changeset1 = new Changeset("1", 1L, author, "first commit");
    Changeset changeset2 = new Changeset("2", 2L, author, "second commit");

    ChangesetPagingResult changesets = new ChangesetPagingResult(2, ImmutableList.of(changeset1, changeset2));

    when(logCommandBuilder.getChangesets()).thenReturn(changesets);
    String message = service.createSquashCommitMessage(REPOSITORY.getNamespaceAndName(), "squash", "master");
    assertThat(message).isEqualTo("-- first commit\n-- second commit\n");
  }

  private MergeCommitDto createMergeCommit(String source, String target, boolean deleteBranch) {
    MergeCommitDto mergeCommit = new MergeCommitDto();
    mergeCommit.setSource(source);
    mergeCommit.setTarget(target);
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
    when(repositoryService.getBranchesCommand()).thenReturn(branchesCommandBuilder);
    when(branchesCommandBuilder.getBranches()).thenReturn(branches);
  }
}
