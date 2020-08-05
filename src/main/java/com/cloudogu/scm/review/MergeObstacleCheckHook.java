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
import java.util.List;

@EagerSingleton
@Extension
public class MergeObstacleCheckHook {

  private static final Logger LOG = LoggerFactory.getLogger(MergeObstacleCheckHook.class);

  private final DefaultPullRequestService pullRequestService;
  private final MergeService mergeService;

  @Inject
  public MergeObstacleCheckHook(DefaultPullRequestService pullRequestService, MergeService mergeService) {
    this.pullRequestService = pullRequestService;
    this.mergeService = mergeService;
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

    private Worker(PreReceiveRepositoryHookEvent event) {
      this.repository = event.getRepository();
      this.branchProvider = event.getContext().getBranchProvider();
      this.mergeDetectionProvider = event.getContext().getMergeDetectionProvider();
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
        mergeService.verifyNoObstacles(PermissionCheck.mayPerformEmergencyMerge(repository), repository, pullRequest);
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
