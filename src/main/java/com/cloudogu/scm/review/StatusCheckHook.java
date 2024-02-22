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
import com.cloudogu.scm.review.pullrequest.service.MergeObstacle;
import com.cloudogu.scm.review.pullrequest.service.MergeService;
import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import com.cloudogu.scm.review.pullrequest.service.PullRequestRejectedEvent;
import com.cloudogu.scm.review.pullrequest.service.PullRequestUpdatedMailEvent;
import com.github.legman.Subscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sonia.scm.EagerSingleton;
import sonia.scm.NotFoundException;
import sonia.scm.event.ScmEventBus;
import sonia.scm.plugin.Extension;
import sonia.scm.repository.PostReceiveRepositoryHookEvent;
import sonia.scm.repository.Repository;
import sonia.scm.repository.api.HookBranchProvider;
import sonia.scm.repository.api.HookContext;
import sonia.scm.repository.api.HookFeature;
import sonia.scm.repository.spi.HookMergeDetectionProvider;

import jakarta.inject.Inject;
import java.util.List;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

@EagerSingleton
@Extension
public class StatusCheckHook {

  private static final Logger LOG = LoggerFactory.getLogger(StatusCheckHook.class);

  private final DefaultPullRequestService pullRequestService;
  private final MessageSenderFactory messageSenderFactory;
  private final MergeService mergeService;
  private final InternalMergeSwitch internalMergeSwitch;

  @Inject
  public StatusCheckHook(DefaultPullRequestService pullRequestService, MessageSenderFactory messageSenderFactory, MergeService mergeService, InternalMergeSwitch internalMergeSwitch) {
    this.pullRequestService = pullRequestService;
    this.messageSenderFactory = messageSenderFactory;
    this.mergeService = mergeService;
    this.internalMergeSwitch = internalMergeSwitch;
  }

  @Subscribe(async = false)
  public void checkStatus(PostReceiveRepositoryHookEvent event) {
    Repository repository = event.getRepository();
    if (ignoreHook(event.getContext(), repository)) {
      return;
    }
    List<PullRequest> pullRequests = pullRequestService.getAll(repository.getNamespace(), repository.getName());
    new Worker(event).process(pullRequests);
  }

  private boolean ignoreHook(HookContext context, Repository repository) {
    if (internalMergeSwitch.internalMergeRunning()) {
      return true;
    }
    if (!pullRequestService.supportsPullRequests(repository)) {
      return true;
    }
    if (!context.isFeatureSupported(HookFeature.BRANCH_PROVIDER)) {
      LOG.debug("hook event for repository {} does not support branches - cannot check for merges", repository);
      return true;
    }
    return false;
  }

  private class Worker {
    private final MessageSender messageSender;
    private final Repository repository;
    private final HookBranchProvider branchProvider;
    private final HookMergeDetectionProvider mergeDetectionProvider;

    private Worker(PostReceiveRepositoryHookEvent event) {
      this.messageSender = messageSenderFactory.create(event);
      this.repository = event.getRepository();
      this.branchProvider = event.getContext().getBranchProvider();
      if (event.getContext().isFeatureSupported(HookFeature.MERGE_DETECTION_PROVIDER)) {
        this.mergeDetectionProvider = event.getContext().getMergeDetectionProvider();
      } else {
        LOG.debug("hook event for repository {} does not support merge detection - cannot check for merges", repository);
        this.mergeDetectionProvider = null;
      }
    }

    private void process(List<PullRequest> pullRequests) {
      pullRequests.forEach(this::process);
    }

    private void process(PullRequest pullRequest) {
      if (pullRequest.isInProgress()) {
        processInProgress(pullRequest);
      } else {
        LOG.debug("ignoring pull request {}, because it in status {}", pullRequest.getId(), pullRequest.getStatus());
      }
    }

    private void processInProgress(PullRequest pullRequest) {
      if (branchesAreModified(pullRequest)) {
        if (isMerged(pullRequest)) {
          setMerged(pullRequest);
        } else {
          updated(pullRequest);
        }
      } else if (targetBranchWasDeleted(pullRequest)) {
        setRejected(pullRequest, PullRequestRejectedEvent.RejectionCause.TARGET_BRANCH_DELETED);
      } else if (sourceBranchWasDeleted(pullRequest)) {
        setRejected(pullRequest, PullRequestRejectedEvent.RejectionCause.SOURCE_BRANCH_DELETED);
      }
    }

    private boolean branchesAreModified(PullRequest pullRequest) {
      return
        branchProvider.getCreatedOrModified().contains(pullRequest.getSource()) ||
          branchProvider.getCreatedOrModified().contains(pullRequest.getTarget());
    }

    private boolean isMerged(PullRequest pullRequest) {
      if (mergeDetectionProvider == null) {
        return false;
      }
      try {
        return mergeDetectionProvider.branchesMerged(pullRequest.getTarget(), pullRequest.getSource());
      } catch (NotFoundException e) {
        LOG.debug("target or source branch not found for pull request #{}", pullRequest.getId(), e);
        return false;
      }
    }

    private void setMerged(PullRequest pullRequest) {
      LOG.info("setting pull request {} to status MERGED", pullRequest.getId());
      String message = format("Merged pull request #%s (%s -> %s):", pullRequest.getId(), pullRequest.getSource(), pullRequest.getTarget());
      messageSender.sendMessageForPullRequest(pullRequest, message);

      List<String> mergeObstacles =
        mergeService.verifyNoObstacles(PermissionCheck.mayPerformEmergencyMerge(repository), repository, pullRequest)
          .stream()
          .map(MergeObstacle::getKey)
          .collect(toList());

      if (mergeObstacles.isEmpty()) {
        pullRequestService.setMerged(repository, pullRequest.getId());
      } else {
        pullRequestService.setEmergencyMerged(repository, pullRequest.getId(), "merged by a direct push to the repository", mergeObstacles);
      }
    }

    private boolean targetBranchWasDeleted(PullRequest pullRequest) {
      return branchProvider.getDeletedOrClosed().contains(pullRequest.getTarget());
    }

    private boolean sourceBranchWasDeleted(PullRequest pullRequest) {
      return branchProvider.getDeletedOrClosed().contains(pullRequest.getSource());
    }

    private void setRejected(PullRequest pullRequest, PullRequestRejectedEvent.RejectionCause cause) {
      LOG.info("setting pull request {} to status REJECTED", pullRequest.getId());
      String message = format("Rejected pull request #%s (%s -> %s):", pullRequest.getId(), pullRequest.getSource(), pullRequest.getTarget());
      messageSender.sendMessageForPullRequest(pullRequest, message);
      pullRequestService.setRejected(repository, pullRequest.getId(), cause);
    }

    private void updated(PullRequest pullRequest) {
      LOG.info("pull request {} was updated", pullRequest.getId());
      if (branchProvider.getCreatedOrModified().contains(pullRequest.getSource())) {
        LOG.info("source branch of pull request {} was updated", pullRequest.getId());
        pullRequestService.sourceRevisionChanged(repository, pullRequest.getId());
      } else {
        LOG.info("target branch of pull request {} was updated", pullRequest.getId());
        pullRequestService.targetRevisionChanged(repository, pullRequest.getId());
      }
      if(branchProvider.getCreatedOrModified().contains(pullRequest.getSource()) && pullRequest.isInProgress()) {
        ScmEventBus.getInstance().post(new PullRequestUpdatedMailEvent(repository, pullRequest));
      }
    }
  }
}
