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

import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import com.cloudogu.scm.review.pullrequest.service.PullRequestService;
import com.github.legman.Subscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sonia.scm.EagerSingleton;
import sonia.scm.plugin.Extension;
import sonia.scm.repository.Branches;
import sonia.scm.repository.PostReceiveRepositoryHookEvent;
import sonia.scm.repository.api.Command;
import sonia.scm.repository.api.RepositoryService;
import sonia.scm.repository.api.RepositoryServiceFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.util.List;

import static java.lang.String.format;
import static sonia.scm.repository.api.HookFeature.MESSAGE_PROVIDER;

@EagerSingleton @Extension
public class PullRequestInformationHook {

  private static final Logger LOG = LoggerFactory.getLogger(PullRequestInformationHook.class);

  private final PullRequestService service;
  private final RepositoryServiceFactory serviceFactory;
  private final MessageSenderFactory messageSenderFactory;

  @Inject
  public PullRequestInformationHook(PullRequestService service, RepositoryServiceFactory serviceFactory, MessageSenderFactory messageSenderFactory) {
    this.service = service;
    this.serviceFactory = serviceFactory;
    this.messageSenderFactory = messageSenderFactory;
  }

  @Subscribe(async = false)
  public void checkForInformation(PostReceiveRepositoryHookEvent event) {
    if (PermissionCheck.mayRead(event.getRepository())) {
      try (RepositoryService repositoryService = serviceFactory.create(event.getRepository())) {
        if (!repositoryService.isSupported(Command.MERGE) || !event.getContext().isFeatureSupported(MESSAGE_PROVIDER)) {
          LOG.trace("ignoring post receive event for repository {}", event.getRepository().getNamespaceAndName());
          return;
        }
        readEffectedBranches(event).forEach(branch -> processBranch(event, repositoryService, branch));
      }
    }
  }

  private void processBranch(PostReceiveRepositoryHookEvent event, RepositoryService repositoryService, String branch) {
    List<PullRequest> pullRequests = service.getAll(event.getRepository().getNamespace(), event.getRepository().getName());
    boolean prFound = new Worker(event).process(pullRequests, branch);
    if (
      !prFound
        && PermissionCheck.mayCreate(event.getRepository())
        && checkIfMultipleBranchesExist(repositoryService)
    ) {
      sendCreateMessages(event, branch);
    }
  }

  private boolean checkIfMultipleBranchesExist(RepositoryService repositoryService) {
	  try {
		  Branches branches = repositoryService.getBranchesCommand().getBranches();
		  int branchCount = branches.getBranches().size();
		  return branchCount > 1;
	  } catch (IOException ex) {
		  LOG.warn("could not read branches for repository {}, assuming only one branch exists", repositoryService.getRepository());
		  return false;
	  }
  }

  private void sendCreateMessages(PostReceiveRepositoryHookEvent event, String branch) {
    String message = format("Create new pull request for branch %s:", branch);
    messageSenderFactory.create(event)
      .sendCreatePullRequestMessage(branch, message);
  }

  private List<String> readEffectedBranches(PostReceiveRepositoryHookEvent event) {
    return event
      .getContext()
      .getBranchProvider()
      .getCreatedOrModified();
  }

  private class Worker {
    private final MessageSender messageSender;

    private boolean prFound = false;

    private Worker(PostReceiveRepositoryHookEvent event) {
      this.messageSender = messageSenderFactory.create(event);
    }

    private boolean process(List<PullRequest> pullRequests, String branch) {
      pullRequests
        .stream()
        .filter(PullRequest::isInProgress)
        .filter(pr -> branch.equals(pr.getSource()))
        .forEach(this::messageForExistingPullRequest);
      return prFound;
    }

    private void messageForExistingPullRequest(PullRequest pullRequest) {
      this.prFound = true;
      sendExistingMessages(pullRequest);
    }

    private void sendExistingMessages(PullRequest pullRequest) {
      String message = format("Check existing pull request #%s (%s -> %s):", pullRequest.getId(), pullRequest.getSource(), pullRequest.getTarget());
      messageSender.sendMessageForPullRequest(pullRequest, message);
    }
  }
}
