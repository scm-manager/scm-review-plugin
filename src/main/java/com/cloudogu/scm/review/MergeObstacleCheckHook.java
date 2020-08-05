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
package com.cloudogu.scm.review;

import com.cloudogu.scm.review.pullrequest.service.DefaultPullRequestService;
import com.cloudogu.scm.review.pullrequest.service.MergeNotAllowedException;
import com.cloudogu.scm.review.pullrequest.service.MergeObstacle;
import com.cloudogu.scm.review.pullrequest.service.MergeService;
import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import com.cloudogu.scm.review.pullrequest.service.PullRequestStatus;
import com.github.legman.Subscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sonia.scm.EagerSingleton;
import sonia.scm.NotFoundException;
import sonia.scm.plugin.Extension;
import sonia.scm.repository.PreReceiveRepositoryHookEvent;
import sonia.scm.repository.Repository;
import sonia.scm.repository.api.HookBranchProvider;
import sonia.scm.repository.spi.HookMergeDetectionProvider;

import javax.inject.Inject;
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

  @Inject
  public MergeObstacleCheckHook(DefaultPullRequestService pullRequestService, MergeService mergeService, MessageSenderFactory messageSenderFactory) {
    this.pullRequestService = pullRequestService;
    this.mergeService = mergeService;
    this.messageSenderFactory = messageSenderFactory;
  }

  @Subscribe(async = false)
  public void checkForObstacles(PreReceiveRepositoryHookEvent event) {
    if (!pullRequestService.supportsPullRequests(event.getRepository())) {
      return;
    }
    List<PullRequest> pullRequests = pullRequestService.getAll(event.getRepository().getNamespace(), event.getRepository().getName());
    new Worker(event).process(pullRequests);
  }

  private class Worker {
    private final Repository repository;
    private final HookBranchProvider branchProvider;
    private final HookMergeDetectionProvider mergeDetectionProvider;
    private final MessageSender messageSender;

    private Worker(PreReceiveRepositoryHookEvent event) {
      this.repository = event.getRepository();
      this.branchProvider = event.getContext().getBranchProvider();
      this.mergeDetectionProvider = event.getContext().getMergeDetectionProvider();
      this.messageSender = messageSenderFactory.create(event);
    }

    private void process(List<PullRequest> pullRequests) {
      pullRequests.forEach(this::process);
    }

    private void process(PullRequest pullRequest) {
      if (hasStatusOpen(pullRequest)) {
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
            LOG.info("about to merge pull request #{} for repository {} with {} obstacles by push", pullRequest.getId(), repository.getNamespaceAndName(), mergeObstacles.size());
            String[] message =
              {
                format("This merges pull request #%s (%s -> %s) with %s obstacles.", pullRequest.getId(), pullRequest.getSource(), pullRequest.getTarget(), mergeObstacles.size()),
                "This merge is permitted only because you have the permission for emergency merges",
                "or the obstacles are not blocking ones.",
                "Please check this pull request:"
              };
            messageSender.sendMessageForPullRequest(pullRequest, message);
          }
        } catch (MergeNotAllowedException e) {
          Collection<MergeObstacle> mergeObstacles = e.getObstacles();
          LOG.debug("merge of pull request #{} for repository {} was rejected due to {} obstacle(s)", pullRequest.getId(), repository.getNamespaceAndName(), mergeObstacles.size());
          String[] message =
            {
              format("This would merge pull request #%s (%s -> %s) with %s obstacle(s).", pullRequest.getId(), pullRequest.getSource(), pullRequest.getTarget(), mergeObstacles.size()),
              "You do not have the permission to execute emergency merges.",
              "Please check this pull request for obstacles:"
            };
          messageSender.sendMessageForPullRequest(pullRequest, message);
          throw e;
        }
      }
    }

    private boolean hasStatusOpen(PullRequest pullRequest) {
      return pullRequest.getStatus() == PullRequestStatus.OPEN;
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
