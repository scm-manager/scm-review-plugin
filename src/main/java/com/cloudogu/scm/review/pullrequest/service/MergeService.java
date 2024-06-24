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

import com.cloudogu.scm.review.InternalMergeSwitch;
import com.cloudogu.scm.review.PermissionCheck;
import com.cloudogu.scm.review.pullrequest.dto.MergeCommitDto;
import sonia.scm.repository.Contributor;
import sonia.scm.repository.NamespaceAndName;
import sonia.scm.repository.Person;
import sonia.scm.repository.Repository;
import sonia.scm.repository.api.Command;
import sonia.scm.repository.api.MergeCommandBuilder;
import sonia.scm.repository.api.MergeCommandResult;
import sonia.scm.repository.api.MergeDryRunCommandResult;
import sonia.scm.repository.api.MergePreventReason;
import sonia.scm.repository.api.MergePreventReasonType;
import sonia.scm.repository.api.MergeStrategy;
import sonia.scm.repository.api.MergeStrategyNotSupportedException;
import sonia.scm.repository.api.RepositoryService;
import sonia.scm.repository.api.RepositoryServiceFactory;
import sonia.scm.repository.spi.MergeConflictResult;
import sonia.scm.user.DisplayUser;
import sonia.scm.user.User;
import sonia.scm.user.UserDisplayManager;

import jakarta.inject.Inject;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.apache.shiro.SecurityUtils.getSubject;

public class MergeService {

  private final RepositoryServiceFactory serviceFactory;
  private final PullRequestService pullRequestService;
  private final Collection<MergeGuard> mergeGuards;
  private final InternalMergeSwitch internalMergeSwitch;
  private final UserDisplayManager userDisplayManager;
  private final MergeCommitMessageService mergeCommitMessageService;

  @Inject
  public MergeService(
    RepositoryServiceFactory serviceFactory,
    PullRequestService pullRequestService,
    Set<MergeGuard> mergeGuards,
    InternalMergeSwitch internalMergeSwitch,
    UserDisplayManager userDisplayManager,
    MergeCommitMessageService mergeCommitMessageService
  ) {
    this.serviceFactory = serviceFactory;
    this.pullRequestService = pullRequestService;
    this.mergeGuards = mergeGuards;
    this.internalMergeSwitch = internalMergeSwitch;
    this.userDisplayManager = userDisplayManager;
    this.mergeCommitMessageService = mergeCommitMessageService;
  }

  public void merge(NamespaceAndName namespaceAndName, String pullRequestId, MergeCommitDto mergeCommitDto, MergeStrategy strategy, boolean emergency) {
    try (RepositoryService repositoryService = serviceFactory.create(namespaceAndName)) {
      PullRequest pullRequest = pullRequestService.get(repositoryService.getRepository(), pullRequestId);
      Collection<MergeObstacle> obstacles = verifyNoObstacles(emergency, repositoryService.getRepository(), pullRequest);
      assertPullRequestIsOpen(repositoryService.getRepository(), pullRequest);

      internalMergeSwitch.runInternalMerge(
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
            pullRequestService.setEmergencyMerged(repositoryService.getRepository(), pullRequest.getId(), mergeCommitDto.getOverrideMessage(), getIgnoredMergeObstacles(obstacles));
          } else {
            pullRequestService.setMerged(repositoryService.getRepository(), pullRequest.getId());
          }

          if (repositoryService.isSupported(Command.BRANCH) && mergeCommitDto.isShouldDeleteSourceBranch()) {
            String deletedSourceBranch = pullRequest.getSource();
            Repository repository = repositoryService.getRepository();
            repositoryService.getBranchCommand().delete(deletedSourceBranch);
            rejectPullRequestsForDeletedBranch(repository, pullRequest, deletedSourceBranch);
          }
        }
      );
    }
  }

  private void rejectPullRequestsForDeletedBranch(Repository repository, PullRequest pullRequest, String deletedSourceBranch) {
    List<PullRequest> pullRequests = pullRequestService.getAll(repository.getNamespace(), repository.getName());
    pullRequests.forEach(pr -> {
      if (shouldRejectPullRequestForDeletedBranch(pullRequest, deletedSourceBranch, pr)) {
        pullRequestService.setRejected(repository, pr.getId(), PullRequestRejectedEvent.RejectionCause.SOURCE_BRANCH_DELETED);
      }
    });
  }

  private boolean shouldRejectPullRequestForDeletedBranch(PullRequest pullRequest, String deletedSourceBranch, PullRequest pr) {
    return pr.isInProgress()
      && !pr.getId().equals(pullRequest.getId())
      && (pr.getSource().equals(deletedSourceBranch) || pr.getTarget().equals(deletedSourceBranch));
  }

  public Collection<MergeObstacle> verifyNoObstacles(boolean emergency, Repository repository, PullRequest pullRequest) {
    if (emergency) {
      PermissionCheck.checkEmergencyMerge(repository);
    }
    Collection<MergeObstacle> obstacles = getObstacles(repository, pullRequest);
    checkIfMergeIsPreventedByObstacles(repository, pullRequest, obstacles, emergency);
    return obstacles;
  }

  private List<String> getIgnoredMergeObstacles(Collection<MergeObstacle> obstacles) {
    return obstacles.stream().map(MergeObstacle::getKey).collect(Collectors.toList());
  }

  private void checkIfMergeIsPreventedByObstacles(Repository repository, PullRequest pullRequest, Collection<MergeObstacle> obstacles, boolean emergency) {
    if (!obstacles.stream().allMatch(MergeObstacle::isOverrideable)) {
      throw new MergeNotAllowedException(repository, pullRequest, obstacles);
    }
    if (!emergency && !obstacles.isEmpty()) {
      throw new MergeNotAllowedException(repository, pullRequest, obstacles);
    }
  }

  public MergeCheckResult checkMerge(NamespaceAndName namespaceAndName, String pullRequestId) {
    try (RepositoryService repositoryService = serviceFactory.create(namespaceAndName)) {
      PullRequest pullRequest = pullRequestService.get(repositoryService.getRepository(), pullRequestId);
      MergeDryRunCommandResult mergeDryRunCommandResult = dryRun(repositoryService, pullRequest)
        .orElse(new MergeDryRunCommandResult(false, null));
      Collection<MergeObstacle> obstacles = getObstacles(repositoryService.getRepository(), pullRequest);
      return new MergeCheckResult(!mergeDryRunCommandResult.isMergeable(), obstacles, mergeDryRunCommandResult.getReasons());
    }
  }

  private Optional<MergeDryRunCommandResult> dryRun(RepositoryService repositoryService, PullRequest pullRequest) {
    assertPullRequestNotClosed(repositoryService.getRepository(), pullRequest);
    if (PermissionCheck.mayRead(repositoryService.getRepository())) {
      MergeCommandBuilder mergeCommandBuilder = prepareDryRun(repositoryService, pullRequest.getSource(), pullRequest.getTarget());
      return of(mergeCommandBuilder.dryRun());
    }
    return empty();
  }

  public MergeConflictResult conflicts(NamespaceAndName namespaceAndName, String pullRequestId) {
    try (RepositoryService repositoryService = serviceFactory.create(namespaceAndName)) {
      PullRequest pullRequest = pullRequestService.get(repositoryService.getRepository(), pullRequestId);
      PermissionCheck.checkRead(repositoryService.getRepository());
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

  public CommitDefaults createCommitDefaults(NamespaceAndName namespaceAndName, String pullRequestId, MergeStrategy strategy) {
    PullRequest pullRequest = pullRequestService.get(namespaceAndName.getNamespace(), namespaceAndName.getName(), pullRequestId);
    String message = mergeCommitMessageService.determineDefaultMessage(namespaceAndName, pullRequest, strategy);
    Optional<DisplayUser> author = determineDefaultAuthorIfNotCurrentUser(pullRequest, strategy);
    return new CommitDefaults(message, author.orElse(null));
  }

  public Optional<DisplayUser> determineDefaultAuthorIfNotCurrentUser(PullRequest pullRequest, MergeStrategy strategy) {
    if (strategy == null) {
      return empty();
    }
    switch (strategy) {
      case SQUASH:
        return of(determineSquashAuthor(pullRequest));
      case FAST_FORWARD_IF_POSSIBLE:
      case MERGE_COMMIT:
      default:
        return empty();
    }
  }

  private DisplayUser determineSquashAuthor(PullRequest pullRequest) {
    return userDisplayManager.get(pullRequest.getAuthor())
      .orElseGet(MergeService::currentDisplayUser);
  }

  public boolean isCommitMessageDisabled(MergeStrategy strategy) {
    return !strategy.isCommitMessageAllowed();
  }

  public String createMergeCommitMessageHint(MergeStrategy strategy) {
    if (strategy == MergeStrategy.FAST_FORWARD_IF_POSSIBLE) {
      return strategy.name();
    }
    return null;
  }

  private void assertPullRequestIsOpen(Repository repository, PullRequest pullRequest) {
    if (!pullRequest.isOpen()) {
      throw new CannotMergeNotOpenPullRequestException(repository, pullRequest);
    }
  }

  private void assertPullRequestNotClosed(Repository repository, PullRequest pullRequest) {
    if (pullRequest.isClosed()) {
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
    String enrichedCommitMessage = enrichCommitMessageWithTrailers(pullRequest, mergeCommitDto);
    mergeCommand.setMessage(enrichedCommitMessage);
    determineDefaultAuthorIfNotCurrentUser(pullRequest, strategy)
      .ifPresent(mergeCommand::setAuthor);
    mergeCommand.setMergeStrategy(strategy);
  }

  private String enrichCommitMessageWithTrailers(PullRequest pullRequest, MergeCommitDto mergeCommitDto) {
    StringBuilder builder = new StringBuilder();
    builder.append(mergeCommitDto.getCommitMessage());

    List<String> approvers = getPullRequestApprovers(pullRequest);
    if (shouldAppendTrailers(approvers)) {
      builder.append("\n\n");

      String merger = currentDisplayUser().getDisplayName();
      appendApproversToCommitMessage(builder, approvers, merger);
    }
    return builder.toString();
  }

  private boolean shouldAppendTrailers(List<String> approvers) {
    return !approvers.isEmpty();
  }

  private void appendApproversToCommitMessage(StringBuilder builder, List<String> approvers, String merger) {
    for (String approver : approvers) {
      userDisplayManager.get(approver)
        .ifPresent(reviewer -> {
          if (!reviewer.getDisplayName().equals(merger)) {
            Contributor reviewerAsContributor = new Contributor(Contributor.REVIEWED_BY, new Person(reviewer.getDisplayName(), reviewer.getMail()));
            builder.append(reviewerAsContributor.toCommitLine()).append("\n");
          }
        });
    }
  }

  private List<String> getPullRequestApprovers(PullRequest pullRequest) {
    return pullRequest.getReviewer()
      .entrySet()
      .stream()
      .filter(Map.Entry::getValue)
      .map(Map.Entry::getKey)
      .collect(Collectors.toList());
  }

  private MergeCommandBuilder prepareDryRun(RepositoryService repositoryService, String sourceBranch, String targetBranch) {
    MergeCommandBuilder mergeCommand = repositoryService.getMergeCommand();
    mergeCommand.setBranchToMerge(sourceBranch);
    mergeCommand.setTargetBranch(targetBranch);
    return mergeCommand;
  }

  private static DisplayUser currentDisplayUser() {
    return DisplayUser.from(currentUser());
  }

  private static User currentUser() {
    return getSubject().getPrincipals().oneByType(User.class);
  }

  public static class CommitDefaults {
    private final String commitMessage;
    private final DisplayUser commitAuthor;

    public CommitDefaults(String commitMessage, DisplayUser commitAuthor) {
      this.commitMessage = commitMessage;
      this.commitAuthor = commitAuthor;
    }

    public String getCommitMessage() {
      return commitMessage;
    }

    public DisplayUser getCommitAuthor() {
      return commitAuthor;
    }
  }
}
