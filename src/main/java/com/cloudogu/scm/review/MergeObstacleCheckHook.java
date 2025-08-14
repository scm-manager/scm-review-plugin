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

package com.cloudogu.scm.review;

import com.cloudogu.scm.review.pullrequest.service.DefaultPullRequestService;
import com.cloudogu.scm.review.pullrequest.service.MergeNotAllowedException;
import com.cloudogu.scm.review.pullrequest.service.MergeObstacle;
import com.cloudogu.scm.review.pullrequest.service.MergeService;
import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import com.github.legman.Subscribe;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sonia.scm.EagerSingleton;
import sonia.scm.NotFoundException;
import sonia.scm.plugin.Extension;
import sonia.scm.repository.PreReceiveRepositoryHookEvent;
import sonia.scm.repository.Repository;
import sonia.scm.repository.api.HookBranchProvider;
import sonia.scm.repository.api.HookContext;
import sonia.scm.repository.api.HookFeature;
import sonia.scm.repository.spi.HookMergeDetectionProvider;

import java.util.Collection;
import java.util.List;

import static java.lang.String.format;

@EagerSingleton
@Extension
public class MergeObstacleCheckHook {

  private static final Logger LOG = LoggerFactory.getLogger(MergeObstacleCheckHook.class);

  private final DefaultPullRequestService pullRequestService;
  private final MergeService mergeService;
  private final MessageSenderFactory messageSenderFactory;
  private final InternalMergeSwitch internalMergeSwitch;

  @Inject
  public MergeObstacleCheckHook(DefaultPullRequestService pullRequestService, MergeService mergeService, MessageSenderFactory messageSenderFactory, InternalMergeSwitch internalMergeSwitch) {
    this.pullRequestService = pullRequestService;
    this.mergeService = mergeService;
    this.messageSenderFactory = messageSenderFactory;
    this.internalMergeSwitch = internalMergeSwitch;
  }

  @Subscribe(async = false)
  public void checkForObstacles(PreReceiveRepositoryHookEvent event) {
    HookContext context = event.getContext();
    Repository repository = event.getRepository();
    if (ignoreHook(context, repository)) {
      return;
    }
    List<PullRequest> pullRequests = pullRequestService.getAll(repository.getNamespace(), repository.getName());
    new Worker(event).process(pullRequests);
  }

  private boolean ignoreHook(HookContext context, Repository repository) {
    return internalMergeSwitch.internalMergeRunning()
      || !context.isFeatureSupported(HookFeature.MERGE_DETECTION_PROVIDER)
      || !pullRequestService.supportsPullRequests(repository);
  }

  private class Worker {
    private final Repository repository;
    private final HookBranchProvider branchProvider;
    private final HookMergeDetectionProvider mergeDetectionProvider;
    private final MessageSender messageSender;

    private Worker(PreReceiveRepositoryHookEvent event) {
      HookContext context = event.getContext();
      this.repository = event.getRepository();
      this.branchProvider = context.getBranchProvider();
      this.mergeDetectionProvider = context.getMergeDetectionProvider();
      this.messageSender = messageSenderFactory.create(event);
    }

    private void process(List<PullRequest> pullRequests) {
      pullRequests.forEach(this::process);
    }

    private void process(PullRequest pullRequest) {
      if (pullRequest.isOpen()) {
        processOpen(pullRequest);
      } else {
        LOG.debug("ignoring pull request {}, because it in status {}", pullRequest.getId(), pullRequest.getStatus());
      }
    }

    private void processOpen(PullRequest pullRequest) {
      if (branchesAreModified(pullRequest) && isMerged(pullRequest)) {
        try {
          Collection<MergeObstacle> mergeObstacles = mergeService.verifyNoObstacles(PermissionCheck.mayPerformEmergencyMerge(repository), repository, pullRequest);
          if (!mergeObstacles.isEmpty()) {
            LOG.info("about to merge pull request #{} for repository {} with {} violated rule(s) by push", pullRequest.getId(), repository.getNamespaceAndName(), mergeObstacles.size());
            String[] message =
              {
                format("This merges pull request #%s (%s -> %s) with %s violated rule(s).", pullRequest.getId(), pullRequest.getSource(), pullRequest.getTarget(), mergeObstacles.size()),
                "This merge is permitted only because you have the permission for emergency merges",
                "or the violated rules are not blocking ones.",
                "Please check this pull request:"
              };
            messageSender.sendMessageForPullRequest(pullRequest, message);
          }
        } catch (MergeNotAllowedException e) {
          Collection<MergeObstacle> mergeObstacles = e.getObstacles();
          LOG.debug("merge of pull request #{} for repository {} was rejected due to {} violated rule(s)", pullRequest.getId(), repository.getNamespaceAndName(), mergeObstacles.size());
          String[] message =
            {
              format("This would merge pull request #%s (%s -> %s) with %s violated rule(s).", pullRequest.getId(), pullRequest.getSource(), pullRequest.getTarget(), mergeObstacles.size()),
              "You do not have the permission to execute emergency merges.",
              "Please check this pull request for details about the rules:"
            };
          messageSender.sendMessageForPullRequest(pullRequest, message);
          throw e;
        }
      }
    }

    private boolean branchesAreModified(PullRequest pullRequest) {
      List<String> createdOrModified = branchProvider.getCreatedOrModified();
      return
        createdOrModified.contains(pullRequest.getSource()) ||
          createdOrModified.contains(pullRequest.getTarget());
    }

    private boolean isMerged(PullRequest pullRequest) {
      try {
        return mergeDetectionProvider.branchesMerged(pullRequest.getTarget(), pullRequest.getSource());
      } catch (NotFoundException e) {
        LOG.debug("target or source branch not found for pull request #{}", pullRequest.getId(), e);
        return false;
      }
    }
  }
}
