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

import com.github.legman.Subscribe;
import com.google.common.annotations.VisibleForTesting;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
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
public class PullRequestSuggestionService {

  private static final long PUSH_ENTRY_LIFETIME_IN_HOURS = 2;

  private final List<PushEntry> pushEntries = new ArrayList<>();
  private final PullRequestService pullRequestService;
  private final Clock clock;

  @Inject
  public PullRequestSuggestionService(PullRequestService pullRequestService) {
    this(pullRequestService, Clock.systemUTC());
  }

  @VisibleForTesting
  PullRequestSuggestionService(PullRequestService pullRequestService, Clock clock) {
    this.pullRequestService = pullRequestService;
    this.clock = clock;
  }

  @VisibleForTesting
  List<PushEntry> getPushEntries() {
    return pushEntries;
  }

  @Subscribe(async = false)
  public void onBranchUpdated(PostReceiveRepositoryHookEvent event) {
    Repository repository = event.getRepository();

    if (!pullRequestService.supportsPullRequests(repository)) {
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
            .filter(pushEntry -> pushEntryAlreadyExists(repository.getId(), branch, pushedBy))
            .findFirst()
            .ifPresentOrElse(
              pushEntry -> pushEntry.setPushedAt(now),
              () -> pushEntries.add(new PushEntry(repository.getId(), branch, pushedBy, now))
            )
        );

      deletedBranches.forEach(
        branch -> pushEntries.removeIf(pushEntry -> pushEntry.branch.equals(branch))
      );
    }
  }

  private boolean branchAlreadyHasPullRequestInProgress(List<PullRequest> pullRequests, String branch) {
    return pullRequests.stream().anyMatch(
      pullRequest -> pullRequest.isInProgress() && pullRequest.getSource().equals(branch)
    );
  }

  private boolean pushEntryAlreadyExists(String repositoryId, String branch, String pushedBy) {
    return pushEntries.stream().anyMatch(
      pushEntry -> pushEntry.belongsTo(repositoryId, branch, pushedBy)
    );
  }

  @Subscribe(async = false)
  public void onPullRequestCreated(PullRequestEvent event) {
    if (event.getEventType() != HandlerEventType.CREATE) {
      return;
    }

    synchronized (this) {
      pushEntries.removeIf(pushEntry -> pushEntry.belongsToCreatedPullRequest(event));
    }
  }

  public void removePushEntry(String repositoryId, String branch, String pushedBy) {
    synchronized (this) {
      pushEntries.removeIf(pushEntry -> pushEntry.belongsTo(repositoryId, branch, pushedBy));
    }
  }

  public Collection<PushEntry> getPushEntries(String repositoryId, String pushedBy) {
    synchronized (this) {
      pushEntries.removeIf(this::isExpired);

      return pushEntries
        .stream()
        .filter(pushEntry -> pushEntry.belongsTo(repositoryId, pushedBy))
        .toList();
    }
  }

  private boolean isExpired(PushEntry pushEntry) {
    return ChronoUnit.HOURS.between(pushEntry.pushedAt, Instant.now(clock)) >= PUSH_ENTRY_LIFETIME_IN_HOURS;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class PushEntry {
    private String repositoryId;
    private String branch;
    private String pushedBy;
    private Instant pushedAt;

    boolean belongsTo(String repositoryId, String branch, String pushedBy) {
      return this.repositoryId.equals(repositoryId) &&
        this.branch.equals(branch) &&
        this.pushedBy.equals(pushedBy);
    }

    boolean belongsTo(String repositoryId, String pushedBy) {
      return this.repositoryId.equals(repositoryId) && this.pushedBy.equals(pushedBy);
    }

    boolean belongsToCreatedPullRequest(PullRequestEvent event) {
      return repositoryId.equals(event.getRepository().getId()) && branch.equals(event.getPullRequest().getSource());
    }
  }
}
