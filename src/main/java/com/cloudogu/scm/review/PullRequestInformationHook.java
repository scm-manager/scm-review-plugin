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

import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import com.cloudogu.scm.review.pullrequest.service.PullRequestService;
import com.github.legman.Subscribe;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sonia.scm.EagerSingleton;
import sonia.scm.plugin.Extension;
import sonia.scm.repository.Branches;
import sonia.scm.repository.PostReceiveRepositoryHookEvent;
import sonia.scm.repository.api.Command;
import sonia.scm.repository.api.RepositoryService;
import sonia.scm.repository.api.RepositoryServiceFactory;

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
