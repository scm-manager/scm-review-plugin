package com.cloudogu.scm.review.pullrequest.service;

import com.cloudogu.scm.review.MergeStrategyNotSupportedException;
import com.cloudogu.scm.review.PermissionCheck;
import com.cloudogu.scm.review.comment.service.CommentService;
import com.cloudogu.scm.review.comment.service.SystemCommentType;
import com.cloudogu.scm.review.pullrequest.dto.DisplayedUserDto;
import com.cloudogu.scm.review.pullrequest.dto.MergeCommitDto;
import sonia.scm.ContextEntry;
import sonia.scm.api.v2.resources.MergeCommandDto;
import sonia.scm.event.ScmEventBus;
import sonia.scm.repository.InternalRepositoryException;
import sonia.scm.repository.NamespaceAndName;
import sonia.scm.repository.Person;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryPermissions;
import sonia.scm.repository.api.Command;
import sonia.scm.repository.api.MergeCommandBuilder;
import sonia.scm.repository.api.MergeCommandResult;
import sonia.scm.repository.api.MergeDryRunCommandResult;
import sonia.scm.repository.api.MergeStrategy;
import sonia.scm.repository.api.RepositoryService;
import sonia.scm.repository.api.RepositoryServiceFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Optional;

public class MergeService {

  private final RepositoryServiceFactory serviceFactory;
  private final PullRequestService pullRequestService;
  private final CommentService commentService;
  private final ScmEventBus scmEventBus;

  @Inject
  public MergeService(RepositoryServiceFactory serviceFactory, PullRequestService pullRequestService, CommentService commentService, ScmEventBus scmEventBus) {
    this.serviceFactory = serviceFactory;
    this.pullRequestService = pullRequestService;
    this.commentService = commentService;
    this.scmEventBus = scmEventBus;
  }

  public MergeCommandResult merge(NamespaceAndName namespaceAndName, MergeCommitDto mergeCommitDto, MergeStrategy strategy) {
    try (RepositoryService repositoryService = serviceFactory.create(namespaceAndName)) {
      MergeCommandBuilder mergeCommand = repositoryService.getMergeCommand();
      isAllowedToMerge(repositoryService.getRepository(), mergeCommand, strategy);
      prepareMergeCommand(mergeCommand, mergeCommitDto, strategy);
      MergeCommandResult mergeCommandResult = mergeCommand.executeMerge();

      if (repositoryService.isSupported(Command.BRANCH) && mergeCommitDto.isShouldDeleteSourceBranch()) {
        repositoryService.getBranchCommand().delete(mergeCommitDto.getSource());
      }
      updatePullRequestStatusIfNecessary(repositoryService, mergeCommitDto, strategy, mergeCommandResult);
      return mergeCommandResult;
    }
  }

  public MergeDryRunCommandResult dryRun(NamespaceAndName namespaceAndName, MergeCommandDto mergeCommandDto) {
    try (RepositoryService repositoryService = serviceFactory.create(namespaceAndName)) {
      if (RepositoryPermissions.push(repositoryService.getRepository()).isPermitted()) {
        MergeCommandBuilder mergeCommandBuilder = prepareDryRun(repositoryService, mergeCommandDto);
        return mergeCommandBuilder.dryRun();
      }
    }
    return new MergeDryRunCommandResult(false);
  }

  public String createSquashCommitMessage(NamespaceAndName namespaceAndName, String sourceRevision, String targetRevision) {
    try (RepositoryService repositoryService = serviceFactory.create(namespaceAndName)) {
      if (RepositoryPermissions.read(repositoryService.getRepository()).isPermitted() && repositoryService.isSupported(Command.LOG)) {
        try {
          StringBuilder builder = new StringBuilder();
          repositoryService.getLogCommand()
            .setBranch(sourceRevision)
            .setAncestorChangeset(targetRevision)
            .getChangesets()
            .getChangesets()
            .forEach(c -> builder.append("-- ").append(c.getDescription()).append("\n\n"));
          return builder.toString();
        } catch (IOException e) {
          throw new InternalRepositoryException(ContextEntry.ContextBuilder.entity(repositoryService.getRepository()),
            "Could not read changesets from repository");
        }
      }
    }
    return "";
  }

  private void updatePullRequestStatusIfNecessary(
    RepositoryService repositoryService,
    MergeCommitDto mergeCommitDto,
    MergeStrategy strategy,
    MergeCommandResult mergeCommandResult
  ) {
    if (shouldUpdatePullRequestStatus(repositoryService, mergeCommitDto, strategy, mergeCommandResult)) {
      Repository repository = repositoryService.getRepository();
      Optional<PullRequest> optionalPullRequest = pullRequestService.get(repository, mergeCommitDto.getSource(), mergeCommitDto.getTarget(), PullRequestStatus.OPEN);
      if (optionalPullRequest.isPresent()) {
        PullRequest pullRequest = optionalPullRequest.get();
        pullRequestService.setStatus(repository, pullRequest, PullRequestStatus.MERGED);
        commentService.addStatusChangedComment(repository, pullRequest.getId(), SystemCommentType.MERGED);
        scmEventBus.post(new PullRequestMergedEvent(repository, pullRequest));
      }
    }
  }

  private boolean shouldUpdatePullRequestStatus(
    RepositoryService repositoryService,
    MergeCommitDto mergeCommitDto,
    MergeStrategy strategy,
    MergeCommandResult mergeCommandResult
  ) {
    return (strategy == MergeStrategy.SQUASH && mergeCommandResult.isSuccess())
      || (mergeCommandResult.isSuccess()
      && mergeCommitDto.isShouldDeleteSourceBranch()
      && repositoryService.isSupported(Command.BRANCHES)
      && !branchExists(repositoryService, mergeCommitDto));
  }

  private boolean branchExists(RepositoryService repositoryService, MergeCommitDto mergeCommitDto) {
    try {
      return repositoryService
        .getBranchesCommand()
        .getBranches()
        .getBranches()
        .stream()
        .anyMatch(b ->b.getName().equals(mergeCommitDto.getSource()));
    } catch (IOException e) {
      throw new InternalRepositoryException(ContextEntry.ContextBuilder.entity(repositoryService.getRepository()), "Could not read branches");
    }
  }

  private void isAllowedToMerge(Repository repository, MergeCommandBuilder mergeCommand, MergeStrategy strategy) {
    PermissionCheck.mayMerge(repository);
    if (!mergeCommand.isSupported(strategy)) {
      throw new MergeStrategyNotSupportedException(repository, strategy);
    }
  }

  private void prepareMergeCommand(MergeCommandBuilder mergeCommand, MergeCommitDto mergeCommitDto, MergeStrategy strategy) {
    mergeCommand.setBranchToMerge(mergeCommitDto.getSource());
    mergeCommand.setTargetBranch(mergeCommitDto.getTarget());
    mergeCommand.setMessageTemplate(mergeCommitDto.getCommitMessage());
    mergeCommand.setMergeStrategy(strategy);
    DisplayedUserDto author = mergeCommitDto.getAuthor();
    mergeCommand.setAuthor(new Person(author.getDisplayName(), author.getMail()));
  }

  private MergeCommandBuilder prepareDryRun(RepositoryService repositoryService, MergeCommandDto mergeCommandDto) {
    MergeCommandBuilder mergeCommand = repositoryService.getMergeCommand();
    mergeCommand.setBranchToMerge(mergeCommandDto.getSourceRevision());
    mergeCommand.setTargetBranch(mergeCommandDto.getTargetRevision());
    return mergeCommand;
  }
}
