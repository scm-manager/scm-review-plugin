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

import com.cloudogu.scm.review.BranchResolver;
import com.github.legman.Subscribe;
import com.google.common.annotations.VisibleForTesting;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.SecurityUtils;
import sonia.scm.HandlerEventType;
import sonia.scm.repository.PostReceiveRepositoryHookEvent;
import sonia.scm.repository.Repository;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Singleton
@Slf4j
public class PullRequestSuggestionService {

  private static final long PUSH_ENTRY_LIFETIME_IN_HOURS = 2;

  private final List<PushEntry> pushEntries = new ArrayList<>();
  private final PullRequestService pullRequestService;
  private final BranchResolver branchResolver;
  private final Clock clock;

  @Inject
  public PullRequestSuggestionService(PullRequestService pullRequestService, BranchResolver branchResolver) {
    this(pullRequestService, branchResolver, Clock.systemUTC());
  }

  @VisibleForTesting
  PullRequestSuggestionService(PullRequestService pullRequestService, BranchResolver branchResolver, Clock clock) {
    this.pullRequestService = pullRequestService;
    this.branchResolver = branchResolver;
    this.clock = clock;
  }

  @VisibleForTesting
  List<PushEntry> getPushEntries() {
    return pushEntries;
  }

  @Subscribe(async = false)
  public void onBranchUpdated(PostReceiveRepositoryHookEvent event) {
    Repository repository = event.getRepository();

    boolean isNotSupported = !pullRequestService.supportsPullRequests(repository);
    boolean isSingleOrNoBranch = branchResolver.getAll(repository).size() < 2;

    if (isNotSupported || isSingleOrNoBranch) {
      return;
    }

    Instant now = Instant.now(clock);
    String pushedBy = SecurityUtils.getSubject().getPrincipal().toString();
    List<String> createdOrModifiedBranches = event.getContext().getBranchProvider().getCreatedOrModified();
    List<String> deletedBranches = event.getContext().getBranchProvider().getDeletedOrClosed();
    List<PullRequest> pullRequests = pullRequestService.getAll(repository.getNamespace(), repository.getName());

    synchronized (this) {
      createdOrModifiedBranches
        .stream()
        .filter(branch -> !branchAlreadyHasPullRequestInProgress(pullRequests, branch))
        .forEach(
          branch -> pushEntries
            .stream()
            .filter(pushEntry -> pushEntry.belongsToRepositoryBranchAndUser(repository.getId(), branch, pushedBy))
            .findFirst()
            .ifPresentOrElse(
              pushEntry -> {
                log.debug(
                  "Update push entries pushed at to {} for repository {} and branch {} from user {}",
                  now.toString(),
                  repository.getId(),
                  branch,
                  pushedBy
                );
                pushEntry.setPushedAt(now);
              },
              () -> {
                log.debug(
                  "create new push entry at {} for repository {} and branch {} from user {}",
                  now.toString(),
                  repository.getId(),
                  branch,
                  pushedBy
                );
                pushEntries.add(new PushEntry(repository.getId(), branch, pushedBy, now));
              }
            )
        );

      deletedBranches.forEach(
        branch -> {
          log.debug(
            "Remove push entries for branch {} of repository {}, because branch was deleted",
            branch,
            repository.getId()
          );
          pushEntries.removeIf(pushEntry -> pushEntry.belongsToRepositoryAndBranch(repository.getId(), branch));
        }
      );
    }
  }

  private boolean branchAlreadyHasPullRequestInProgress(List<PullRequest> pullRequests, String branch) {
    return pullRequests.stream().anyMatch(
      pullRequest -> pullRequest.isInProgress() && pullRequest.getSource().equals(branch)
    );
  }

  @Subscribe(async = false)
  public void onPullRequestCreated(PullRequestEvent event) {
    if (event.getEventType() != HandlerEventType.CREATE) {
      return;
    }

    log.debug(
      "Remove push entries for branch {} of repository {}, because a pull request {} was created",
      event.getPullRequest().getSource(),
      event.getRepository().getId(),
      event.getPullRequest().toString()
    );

    synchronized (this) {
      pushEntries.removeIf(pushEntry -> pushEntry.belongsToCreatedPullRequest(event));
    }
  }

  public void removePushEntry(String repositoryId, String branch, String pushedBy) {
    log.debug(
      "Remove push entry for branch {} of repository {} from user {}",
      branch,
      repositoryId,
      pushedBy
    );

    synchronized (this) {
      pushEntries.removeIf(pushEntry -> pushEntry.belongsToRepositoryBranchAndUser(repositoryId, branch, pushedBy));
    }
  }

  public Collection<PushEntry> getPushEntries(String repositoryId, String pushedBy) {
    synchronized (this) {
      pushEntries.removeIf(this::isExpired);

      return pushEntries
        .stream()
        .filter(pushEntry -> pushEntry.belongsToRepositoryAndUser(repositoryId, pushedBy))
        .toList();
    }
  }

  private boolean isExpired(PushEntry pushEntry) {
    if (ChronoUnit.HOURS.between(pushEntry.pushedAt, Instant.now(clock)) >= PUSH_ENTRY_LIFETIME_IN_HOURS) {
      log.debug("Push entry for repository {}, branch {} from user {} pushed at {} is expired and should be removed",
        pushEntry.getRepositoryId(),
        pushEntry.getBranch(),
        pushEntry.getPushedBy(),
        pushEntry.getPushedAt().toString()
      );
      return true;
    }

    return false;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class PushEntry {
    private String repositoryId;
    private String branch;
    private String pushedBy;
    private Instant pushedAt;

    boolean belongsToRepositoryBranchAndUser(String repositoryId, String branch, String pushedBy) {
      return this.repositoryId.equals(repositoryId) &&
        this.branch.equals(branch) &&
        this.pushedBy.equals(pushedBy);
    }

    boolean belongsToRepositoryAndBranch(String repositoryId, String branch) {
      return this.repositoryId.equals(repositoryId) &&
        this.branch.equals(branch);
    }

    boolean belongsToRepositoryAndUser(String repositoryId, String pushedBy) {
      return this.repositoryId.equals(repositoryId) && this.pushedBy.equals(pushedBy);
    }

    boolean belongsToCreatedPullRequest(PullRequestEvent event) {
      return repositoryId.equals(event.getRepository().getId()) && branch.equals(event.getPullRequest().getSource());
    }
  }
}
