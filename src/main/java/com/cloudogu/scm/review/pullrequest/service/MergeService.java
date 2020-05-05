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
import com.cloudogu.scm.review.PermissionCheck;
import com.cloudogu.scm.review.pullrequest.dto.MergeCommitDto;
import sonia.scm.repository.InternalRepositoryException;
import sonia.scm.repository.NamespaceAndName;
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
import sonia.scm.repository.spi.MergeConflictResult;

import javax.inject.Inject;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.cloudogu.scm.review.pullrequest.service.PullRequestStatus.OPEN;
import static java.util.Optional.empty;
import static java.util.Optional.of;
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
  private final Collection<MergeGuard> mergeGuards;
  private final BranchProtectionHook branchProtectionHook;

  @Inject
  public MergeService(RepositoryServiceFactory serviceFactory, PullRequestService pullRequestService, Set<MergeGuard> mergeGuards, BranchProtectionHook branchProtectionHook) {
    this.serviceFactory = serviceFactory;
    this.pullRequestService = pullRequestService;
    this.mergeGuards = mergeGuards;
    this.branchProtectionHook = branchProtectionHook;
  }

  public void merge(NamespaceAndName namespaceAndName, String pullRequestId, MergeCommitDto mergeCommitDto, MergeStrategy strategy, boolean emergency) {
    try (RepositoryService repositoryService = serviceFactory.create(namespaceAndName)) {
      PullRequest pullRequest = pullRequestService.get(repositoryService.getRepository(), pullRequestId);
      Collection<MergeObstacle> obstacles = getObstacles(repositoryService.getRepository(), pullRequest);
      checkIfMergeIsPreventedByObstacles(repositoryService, pullRequest, obstacles, emergency);
      assertPullRequestIsOpen(repositoryService.getRepository(), pullRequest);

      branchProtectionHook.runPrivileged(
        () -> {
          MergeCommandBuilder mergeCommand = repositoryService.getMergeCommand();
          isAllowedToMerge(repositoryService.getRepository(), mergeCommand, strategy, emergency);
          prepareMergeCommand(mergeCommand, pullRequest, mergeCommitDto, strategy);
          MergeCommandResult mergeCommandResult = mergeCommand.executeMerge();

          if (!mergeCommandResult.isSuccess()) {
            throw new MergeConflictException(namespaceAndName, pullRequest.getSource(), pullRequest.getTarget(), mergeCommandResult);
          }

          pullRequestService.setRevisions(repositoryService.getRepository(), pullRequest.getId(), mergeCommandResult.getTargetRevision(), mergeCommandResult.getRevisionToMerge());
          if (emergency) {
            pullRequestService.setEmergencyMerged(repositoryService.getRepository(), pullRequest.getId(), mergeCommitDto.getOverrideMessage(), mergeCommitDto.getIgnoredMergeObstacles());
          } else {
            pullRequestService.setMerged(repositoryService.getRepository(), pullRequest.getId(), mergeCommitDto.getOverrideMessage());
          }

          if (repositoryService.isSupported(Command.BRANCH) && mergeCommitDto.isShouldDeleteSourceBranch()) {
            repositoryService.getBranchCommand().delete(pullRequest.getSource());
          }
        }
      );
    }
  }

  private void checkIfMergeIsPreventedByObstacles(RepositoryService repositoryService, PullRequest pullRequest, Collection<MergeObstacle> obstacles, boolean emergency) {
    if (!obstacles.stream().allMatch(MergeObstacle::isOverrideable)) {
      throw new MergeNotAllowedException(repositoryService.getRepository(), pullRequest, obstacles);
    }
    if (!emergency && !obstacles.isEmpty()) {
      throw new MergeNotAllowedException(repositoryService.getRepository(), pullRequest, obstacles);
    }
  }

  public MergeCheckResult checkMerge(NamespaceAndName namespaceAndName, String pullRequestId) {
    try (RepositoryService repositoryService = serviceFactory.create(namespaceAndName)) {
      PullRequest pullRequest = pullRequestService.get(repositoryService.getRepository(), pullRequestId);
      boolean isMergeable = dryRun(repositoryService, pullRequest)
        .map(MergeDryRunCommandResult::isMergeable)
        .orElse(false);
      Collection<MergeObstacle> obstacles = getObstacles(repositoryService.getRepository(), pullRequest);
      return new MergeCheckResult(!isMergeable, obstacles);
    }
  }

  private Optional<MergeDryRunCommandResult> dryRun(RepositoryService repositoryService, PullRequest pullRequest) {
    assertPullRequestIsOpen(repositoryService.getRepository(), pullRequest);
    if (RepositoryPermissions.push(repositoryService.getRepository()).isPermitted()) {
      MergeCommandBuilder mergeCommandBuilder = prepareDryRun(repositoryService, pullRequest.getSource(), pullRequest.getTarget());
      return of(mergeCommandBuilder.dryRun());
    }
    return empty();
  }

  public MergeConflictResult conflicts(NamespaceAndName namespaceAndName, String pullRequestId) {
    try (RepositoryService repositoryService = serviceFactory.create(namespaceAndName)) {
      PullRequest pullRequest = pullRequestService.get(repositoryService.getRepository(), pullRequestId);
      RepositoryPermissions.push(repositoryService.getRepository()).check();
      MergeCommandBuilder mergeCommandBuilder = prepareDryRun(repositoryService, pullRequest.getSource(), pullRequest.getTarget());
      return mergeCommandBuilder.conflicts();
    }
  }

  private Collection<MergeObstacle> getObstacles(Repository repository, PullRequest pullRequest) {
    return mergeGuards.stream()
      .map(guard -> guard.getObstacles(repository, pullRequest))
      .flatMap(Collection::stream)
      .collect(Collectors.toList());
  }

  public String createDefaultCommitMessage(NamespaceAndName namespaceAndName, String pullRequestId, MergeStrategy strategy) {
    PullRequest pullRequest = pullRequestService.get(namespaceAndName.getNamespace(), namespaceAndName.getName(), pullRequestId);
    if (strategy == null) {
      return "";
    }
    switch (strategy) {
      case SQUASH:
        return createDefaultSquashCommitMessage(namespaceAndName, pullRequest);
      case FAST_FORWARD_IF_POSSIBLE: // should be same as merge (fallback message only)
      case MERGE_COMMIT: // should be default
      default:
        return createDefaultMergeCommitMessage(pullRequest);
    }
  }

  private String createDefaultMergeCommitMessage(PullRequest pullRequest) {
    return MessageFormat.format(MERGE_COMMIT_MESSAGE_TEMPLATE, pullRequest.getSource(), pullRequest.getTarget());
  }

  private String createDefaultSquashCommitMessage(NamespaceAndName namespaceAndName, PullRequest pullRequest) {
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
              } else {
                builder.append("  ");
              }
              builder.append("Author: ").append(c.getAuthor().getName()).append(" <").append(c.getAuthor().getMail()).append(">\n");
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
        return createDefaultMergeCommitMessage(pullRequest);
      }
    }
  }

  private void assertPullRequestIsOpen(Repository repository, PullRequest pullRequest) {
    if (pullRequest.getStatus() != OPEN) {
      throw new CannotMergeNotOpenPullRequestException(repository, pullRequest);
    }
  }

  private void isAllowedToMerge(Repository repository, MergeCommandBuilder mergeCommand, MergeStrategy strategy, boolean emergency) {
    if (emergency) {
      PermissionCheck.checkEmergencyMerge(repository);
    } else {
      PermissionCheck.checkMerge(repository);
    }
    if (!mergeCommand.isSupported(strategy)) {
      throw new MergeStrategyNotSupportedException(repository, strategy);
    }
  }

  private void prepareMergeCommand(MergeCommandBuilder mergeCommand, PullRequest pullRequest, MergeCommitDto mergeCommitDto, MergeStrategy strategy) {
    mergeCommand.setBranchToMerge(pullRequest.getSource());
    mergeCommand.setTargetBranch(pullRequest.getTarget());
    mergeCommand.setMessageTemplate(mergeCommitDto.getCommitMessage());
    mergeCommand.setMergeStrategy(strategy);
  }

  private MergeCommandBuilder prepareDryRun(RepositoryService repositoryService, String sourceBranch, String targetBranch) {
    MergeCommandBuilder mergeCommand = repositoryService.getMergeCommand();
    mergeCommand.setBranchToMerge(sourceBranch);
    mergeCommand.setTargetBranch(targetBranch);
    return mergeCommand;
  }
}
