package com.cloudogu.scm.review;

import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import com.cloudogu.scm.review.pullrequest.service.PullRequestService;
import com.cloudogu.scm.review.pullrequest.service.PullRequestStatus;
import com.github.legman.Subscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sonia.scm.EagerSingleton;
import sonia.scm.plugin.Extension;
import sonia.scm.repository.PostReceiveRepositoryHookEvent;
import sonia.scm.repository.api.Command;
import sonia.scm.repository.api.RepositoryService;
import sonia.scm.repository.api.RepositoryServiceFactory;

import javax.inject.Inject;
import java.util.List;

import static java.lang.String.format;

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
        if (!repositoryService.isSupported(Command.MERGE)) {
          LOG.trace("ignoring post receive event for repository {}", event.getRepository().getNamespaceAndName());
          return;
        }
        readEffectedBranches(event).forEach(branch -> processBranch(event, branch));
      }
    }
  }

  private void processBranch(PostReceiveRepositoryHookEvent event, String branch) {
    List<PullRequest> pullRequests = service.getAll(event.getRepository().getNamespace(), event.getRepository().getName());
    boolean prFound = new Worker(event).process(pullRequests, branch);
    if (!prFound && PermissionCheck.mayCreate(event.getRepository())) {
      sendCreateMessages(event, branch);
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
        .filter(this::pullRequestHasStatusOpen)
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

    private boolean pullRequestHasStatusOpen(PullRequest pullRequest) {
      return pullRequest.getStatus() == PullRequestStatus.OPEN;
    }

  }
}
