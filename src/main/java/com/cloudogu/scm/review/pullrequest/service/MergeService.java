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
import sonia.scm.repository.Changeset;
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
import sonia.scm.repository.spi.MergeConflictResult;
import sonia.scm.user.DisplayUser;
import sonia.scm.user.EMail;
import sonia.scm.user.User;
import sonia.scm.user.UserDisplayManager;

import javax.inject.Inject;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.cloudogu.scm.review.pullrequest.service.PullRequestStatus.OPEN;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.apache.shiro.SecurityUtils.getSubject;
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
  private final InternalMergeSwitch internalMergeSwitch;
  private final UserDisplayManager userDisplayManager;
  private final EMail email;

  @Inject
  public MergeService(RepositoryServiceFactory serviceFactory, PullRequestService pullRequestService, Set<MergeGuard> mergeGuards, InternalMergeSwitch internalMergeSwitch, UserDisplayManager userDisplayManager, EMail email) {
    this.serviceFactory = serviceFactory;
    this.pullRequestService = pullRequestService;
    this.mergeGuards = mergeGuards;
    this.internalMergeSwitch = internalMergeSwitch;
    this.userDisplayManager = userDisplayManager;
    this.email = email;
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
          prepareMergeCommand(repositoryService, mergeCommand, pullRequest, mergeCommitDto, strategy);
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
            repositoryService.getBranchCommand().delete(pullRequest.getSource());
          }
        }
      );
    }
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

  public CommitDefaults createCommitDefaults(NamespaceAndName namespaceAndName, String pullRequestId, MergeStrategy strategy) {
    PullRequest pullRequest = pullRequestService.get(namespaceAndName.getNamespace(), namespaceAndName.getName(), pullRequestId);
    String message = determineDefaultMessage(namespaceAndName, pullRequest, strategy);
    DisplayUser author = determineDefaultAuthor(pullRequest, strategy);
    return new CommitDefaults(message, author);
  }

  public String determineDefaultMessage(NamespaceAndName namespaceAndName, PullRequest pullRequest, MergeStrategy strategy) {
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

  public DisplayUser determineDefaultAuthor(PullRequest pullRequest, MergeStrategy strategy) {
    if (strategy == null) {
      return currentDisplayUser();
    }
    switch (strategy) {
      case SQUASH:
        return determineSquashAuthor(pullRequest);
      case FAST_FORWARD_IF_POSSIBLE:
      case MERGE_COMMIT:
      default:
        return currentDisplayUser();
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

  private String createDefaultMergeCommitMessage(PullRequest pullRequest) {
    return MessageFormat.format(MERGE_COMMIT_MESSAGE_TEMPLATE, pullRequest.getSource(), pullRequest.getTarget());
  }

  private String createDefaultSquashCommitMessage(NamespaceAndName namespaceAndName, PullRequest pullRequest) {
    try (RepositoryService repositoryService = serviceFactory.create(namespaceAndName)) {
      if (RepositoryPermissions.read(repositoryService.getRepository()).isPermitted() && repositoryService.isSupported(Command.LOG)) {
        try {

          StringBuilder builder = new StringBuilder();
          Set<Person> contributors = new HashSet<>();
          getChangesetsFromLogCommand(pullRequest, repositoryService)
            .forEach(c -> {
              contributors.add(c.getAuthor());
              builder.append("- ").append(c.getDescription()).append("\n\n");
            });
          appendSquashContributors(builder, pullRequest, contributors);

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

  private void appendSquashContributors(StringBuilder builder, PullRequest pullRequest, Set<Person> contributors) {
    userDisplayManager.get(pullRequest.getAuthor()).ifPresent(prAuthor -> {
        User currentUser = currentUser();
        String committerMail = email.getMailOrFallback(currentUser);
        builder.append("\nAuthor: ").append(prAuthor.getDisplayName()).append(" <").append(prAuthor.getMail()).append(">");
        if (!prAuthor.getDisplayName().equals(currentUser.getDisplayName())) {
          builder.append("\nCommitted-by: ").append(currentUser.getDisplayName());
          builder.append(" <").append(committerMail).append(">");
        }
        appendCoAuthors(builder, contributors, prAuthor);
      }
    );
  }

  private void appendCoAuthors(StringBuilder builder, Set<Person> contributors, DisplayUser prAuthor) {
    for (Person contributor : contributors) {
      Optional<DisplayUser> prAuthorDisplayUser = userDisplayManager.get(prAuthor.getId());
      if (prAuthorDisplayUser.isPresent() && !prAuthorDisplayUser.get().getDisplayName().equals(contributor.getName())) {
        Optional<DisplayUser> contributorDisplayUser = userDisplayManager.get(contributor.getName());
        if (contributorDisplayUser.isPresent()) {
          appendCoAuthor(builder, contributorDisplayUser.get().getDisplayName(), contributor.getMail());
        } else {
          appendCoAuthor(builder, contributor.getName(), contributor.getMail());
        }
      }
    }
  }

  private void appendCoAuthor(StringBuilder builder, String name, String mail) {
    builder.append("\nCo-authored-by: ").append(name).append(" <").append(mail).append(">");
  }

  private List<Changeset> getChangesetsFromLogCommand(PullRequest pullRequest, RepositoryService repositoryService) throws IOException {
    return repositoryService.getLogCommand()
      .setBranch(pullRequest.getSource())
      .setAncestorChangeset(pullRequest.getTarget())
      .getChangesets()
      .getChangesets();
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

  private void prepareMergeCommand(RepositoryService repositoryService, MergeCommandBuilder mergeCommand, PullRequest pullRequest, MergeCommitDto mergeCommitDto, MergeStrategy strategy) {
    mergeCommand.setBranchToMerge(pullRequest.getSource());
    mergeCommand.setTargetBranch(pullRequest.getTarget());
    String enrichedCommitMessage = enrichCommitMessageWithTrailers(repositoryService, pullRequest, mergeCommitDto, strategy);
    mergeCommand.setMessage(enrichedCommitMessage);
    DisplayUser author = determineDefaultAuthor(pullRequest, strategy);
    mergeCommand.setAuthor(new Person(author.getDisplayName(), author.getMail()));
    mergeCommand.setMergeStrategy(strategy);
  }

  private String enrichCommitMessageWithTrailers(RepositoryService repositoryService, PullRequest pullRequest, MergeCommitDto mergeCommitDto, MergeStrategy strategy) {
    StringBuilder builder = new StringBuilder();
    builder.append(mergeCommitDto.getCommitMessage());

    List<String> approvers = getPullRequestApprovers(pullRequest);
    if (shouldAppendTrailers(strategy, approvers)) {
      builder.append("\n\n");

      String merger = currentDisplayUser().getDisplayName();
      appendSquashCoAuthorsToCommitMessage(repositoryService, pullRequest, strategy, merger, builder);
      appendApproversToCommitMessage(builder, approvers, merger);
    }
    return builder.toString();
  }

  private void appendSquashCoAuthorsToCommitMessage(RepositoryService repositoryService, PullRequest pullRequest, MergeStrategy strategy, String merger, StringBuilder builder) {
    if (strategy == MergeStrategy.SQUASH) {
      List<Changeset> changesets = getAllChangesetsForBranch(repositoryService, pullRequest);
      Set<String> appendedCoAuthorMails = new HashSet<>();
      appendCommitAuthorsAsCoAuthors(changesets, merger, appendedCoAuthorMails, builder);
      extractCoAuthorsFromSquashedCommitMessages(changesets, merger, appendedCoAuthorMails, builder);
    }
  }

  private void appendCommitAuthorsAsCoAuthors(List<Changeset> changesets, String merger, Set<String> appendedCoAuthorMails, StringBuilder builder) {
    changesets
      .stream()
      .map(Changeset::getAuthor)
      .distinct()
      .filter(author -> !author.getName().equals(merger))
      .forEach(author -> {
        appendedCoAuthorMails.add(author.getMail());
        builder.append("Co-authored-by: ").append(author.getName()).append(" <").append(author.getMail()).append(">\n");
      });
  }

  private void extractCoAuthorsFromSquashedCommitMessages(List<Changeset> changesets, String author, Set<String> appendedCoAuthors, StringBuilder builder) {
    Pattern delimiter = Pattern.compile("[\\n]");
    for (Changeset changeset : changesets) {
      if (changeset.getDescription() != null) {
        try (Scanner scanner = new Scanner(changeset.getDescription())) {
          scanner.useDelimiter(delimiter);
          while (scanner.hasNext()) {
            String line = scanner.next();
            if (shouldExtractLineFromSquashedCommit(appendedCoAuthors, line, author)) {
              appendedCoAuthors.add(line);
              builder.append(line).append("\n");
            }
          }
        }
      }
    }
  }

  private boolean shouldExtractLineFromSquashedCommit(Set<String> extractedSquashedCoAuthors, String line, String author) {
    return line.contains("Co-authored-by") && !line.contains(author) && extractedSquashedCoAuthors.stream().noneMatch(line::contains);
  }

  private List<Changeset> getAllChangesetsForBranch(RepositoryService repositoryService, PullRequest pullRequest) {
    try {
      return getChangesetsFromLogCommand(pullRequest, repositoryService);
    } catch (IOException e) {
      throw new InternalRepositoryException(repositoryService.getRepository(), "Could not read changesets from branch " + pullRequest.getSource(), e);
    }
  }

  private boolean shouldAppendTrailers(MergeStrategy strategy, List<String> approvers) {
    return !approvers.isEmpty() || strategy == MergeStrategy.SQUASH;
  }

  private void appendApproversToCommitMessage(StringBuilder builder, List<String> approvers, String merger) {
    for (String approver : approvers) {
      userDisplayManager.get(approver)
        .ifPresent(reviewer -> {
          if (!reviewer.getDisplayName().equals(merger)) {
            builder.append("Reviewed-by: ").append(reviewer.getDisplayName()).append(" <").append(reviewer.getMail()).append(">\n");
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
