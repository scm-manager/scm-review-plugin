package com.cloudogu.scm.review.pullrequest.service;

import com.cloudogu.scm.review.PermissionCheck;
import com.cloudogu.scm.review.pullrequest.dto.DisplayedUserDto;
import com.cloudogu.scm.review.pullrequest.dto.MergeCommitDto;
import sonia.scm.ContextEntry;
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
import sonia.scm.repository.api.MergeStrategyNotSupportedException;
import sonia.scm.repository.api.RepositoryService;
import sonia.scm.repository.api.RepositoryServiceFactory;

import javax.inject.Inject;

import java.io.IOException;
import java.text.MessageFormat;

import static com.cloudogu.scm.review.pullrequest.service.PullRequestStatus.OPEN;
import static sonia.scm.ContextEntry.ContextBuilder.entity;

public class MergeService {

  private static final String MERGE_COMMIT_MESSAGE_TEMPLATE = String.join("\n",
    "Merge of branch {0} into {1}",
    "",
    "Automatic merge by SCM-Manager.");
  private static final String SQUASH_COMMIT_MESSAGE_TEMPLATE = String.join("\n",
    "Squash commits of branch {0}:",
    "",
    "{2}");

  private final RepositoryServiceFactory serviceFactory;
  private final PullRequestService pullRequestService;

  @Inject
  public MergeService(RepositoryServiceFactory serviceFactory, PullRequestService pullRequestService) {
    this.serviceFactory = serviceFactory;
    this.pullRequestService = pullRequestService;
  }

  public void merge(NamespaceAndName namespaceAndName, String pullRequestId, MergeCommitDto mergeCommitDto, MergeStrategy strategy) {
    try (RepositoryService repositoryService = serviceFactory.create(namespaceAndName)) {
      PullRequest pullRequest = pullRequestService.get(repositoryService.getRepository(), pullRequestId);
      assertPullRequestIsOpen(repositoryService.getRepository(), pullRequest);
      MergeCommandBuilder mergeCommand = repositoryService.getMergeCommand();
      isAllowedToMerge(repositoryService.getRepository(), mergeCommand, strategy);
      prepareMergeCommand(mergeCommand, pullRequest, mergeCommitDto, strategy);
      MergeCommandResult mergeCommandResult = mergeCommand.executeMerge();

      if (!mergeCommandResult.isSuccess()) {
        throw new MergeConflictException(namespaceAndName, pullRequest.getSource(), pullRequest.getTarget(), mergeCommandResult);
      }

      pullRequestService.setMerged(repositoryService.getRepository(), pullRequestId);

      if (repositoryService.isSupported(Command.BRANCH) && mergeCommitDto.isShouldDeleteSourceBranch()) {
        repositoryService.getBranchCommand().delete(pullRequest.getSource());
      }
    }
  }

  public MergeDryRunCommandResult dryRun(NamespaceAndName namespaceAndName, String pullRequestId) {
    try (RepositoryService repositoryService = serviceFactory.create(namespaceAndName)) {
      PullRequest pullRequest = pullRequestService.get(repositoryService.getRepository(), pullRequestId);
      assertPullRequestIsOpen(repositoryService.getRepository(), pullRequest);
      if (RepositoryPermissions.push(repositoryService.getRepository()).isPermitted()) {
        MergeCommandBuilder mergeCommandBuilder = prepareDryRun(repositoryService, pullRequest.getSource(), pullRequest.getTarget());
        return mergeCommandBuilder.dryRun();
      }
    }
    return new MergeDryRunCommandResult(false);
  }

  public String createDefaultCommitMessage(NamespaceAndName namespaceAndName, String pullRequestId, MergeStrategy strategy) {
    PullRequest pullRequest = pullRequestService.get(namespaceAndName.getNamespace(), namespaceAndName.getName(), pullRequestId);
    if (strategy == null) {
      return "";
    }
    switch (strategy) {
      case SQUASH:
        try (RepositoryService repositoryService = serviceFactory.create(namespaceAndName)) {
          if (RepositoryPermissions.read(repositoryService.getRepository()).isPermitted() && repositoryService.isSupported(Command.LOG)) {
            try {
              StringBuilder builder = new StringBuilder();
              repositoryService.getLogCommand()
                .setBranch(pullRequest.getSource())
                .setAncestorChangeset(pullRequest.getTarget())
                .getChangesets()
                .getChangesets()
                .forEach(c -> {
                  builder.append("- ").append(c.getDescription()).append('\n');
                  if (c.getDescription().contains("\n")) {
                    builder.append('\n');
                  }
                });
              return MessageFormat.format(SQUASH_COMMIT_MESSAGE_TEMPLATE, pullRequest.getSource(), pullRequest.getTarget(), builder.toString());
            } catch (IOException e) {
              throw new InternalRepositoryException(entity("Branch", pullRequest.getSource()).in(repositoryService.getRepository()),
                "Could not read changesets from repository");
            }
          } else {
            return MessageFormat.format(MERGE_COMMIT_MESSAGE_TEMPLATE, pullRequest.getSource(), pullRequest.getTarget());
          }
        }
      case FAST_FORWARD_IF_POSSIBLE: // should be same as merge (fallback message only)
      case MERGE_COMMIT: // should be default
      default:
        return MessageFormat.format(MERGE_COMMIT_MESSAGE_TEMPLATE, pullRequest.getSource(), pullRequest.getTarget());
    }
  }

  private void assertPullRequestIsOpen(Repository repository, PullRequest pullRequest) {
    if (pullRequest.getStatus() != OPEN) {
      throw new CannotMergeNotOpenPullRequestException(repository, pullRequest);
    }
  }

  private void isAllowedToMerge(Repository repository, MergeCommandBuilder mergeCommand, MergeStrategy strategy) {
    PermissionCheck.checkMerge(repository);
    if (!mergeCommand.isSupported(strategy)) {
      throw new MergeStrategyNotSupportedException(repository, strategy);
    }
  }

  private void prepareMergeCommand(MergeCommandBuilder mergeCommand, PullRequest pullRequest, MergeCommitDto mergeCommitDto, MergeStrategy strategy) {
    mergeCommand.setBranchToMerge(pullRequest.getSource());
    mergeCommand.setTargetBranch(pullRequest.getTarget());
    mergeCommand.setMessageTemplate(mergeCommitDto.getCommitMessage());
    mergeCommand.setMergeStrategy(strategy);
    DisplayedUserDto author = mergeCommitDto.getAuthor();
    mergeCommand.setAuthor(new Person(author.getDisplayName(), author.getMail()));
  }

  private MergeCommandBuilder prepareDryRun(RepositoryService repositoryService, String sourceBranch, String targetBranch) {
    MergeCommandBuilder mergeCommand = repositoryService.getMergeCommand();
    mergeCommand.setBranchToMerge(sourceBranch);
    mergeCommand.setTargetBranch(targetBranch);
    return mergeCommand;
  }
}
