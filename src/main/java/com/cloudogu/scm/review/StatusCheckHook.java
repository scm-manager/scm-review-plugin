package com.cloudogu.scm.review;

import com.cloudogu.scm.review.pullrequest.service.DefaultPullRequestService;
import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import com.cloudogu.scm.review.pullrequest.service.PullRequestRejectedEvent;
import com.cloudogu.scm.review.pullrequest.service.PullRequestStatus;
import com.github.legman.Subscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sonia.scm.EagerSingleton;
import sonia.scm.plugin.Extension;
import sonia.scm.repository.ChangesetPagingResult;
import sonia.scm.repository.InternalRepositoryException;
import sonia.scm.repository.PostReceiveRepositoryHookEvent;
import sonia.scm.repository.Repository;
import sonia.scm.repository.api.Command;
import sonia.scm.repository.api.HookBranchProvider;
import sonia.scm.repository.api.RepositoryService;
import sonia.scm.repository.api.RepositoryServiceFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.util.List;

import static java.lang.String.format;
import static sonia.scm.ContextEntry.ContextBuilder.entity;

@EagerSingleton @Extension
public class StatusCheckHook {

  private static final Logger LOG = LoggerFactory.getLogger(StatusCheckHook.class);

  private final DefaultPullRequestService service;
  private final RepositoryServiceFactory serviceFactory;
  private final MessageSenderFactory messageSenderFactory;

  @Inject
  public StatusCheckHook(DefaultPullRequestService service, RepositoryServiceFactory serviceFactory, MessageSenderFactory messageSenderFactory) {
    this.service = service;
    this.serviceFactory = serviceFactory;
    this.messageSenderFactory = messageSenderFactory;
  }

  @Subscribe(async = false)
  public void checkStatus(PostReceiveRepositoryHookEvent event) {
    try (RepositoryService repositoryService = serviceFactory.create(event.getRepository())) {
      if (!repositoryService.isSupported(Command.MERGE)) {
        LOG.trace("ignoring post receive event for repository {}", event.getRepository().getNamespaceAndName());
        return;
      }
      List<PullRequest> pullRequests = service.getAll(event.getRepository().getNamespace(), event.getRepository().getName());
      new Worker(repositoryService, event).process(pullRequests);
    }
  }

  private class Worker {
    private final RepositoryService repositoryService;
    private final MessageSender messageSender;
    private final Repository repository;
    private final HookBranchProvider branchProvider;

    private Worker(RepositoryService repositoryService, PostReceiveRepositoryHookEvent event) {
      this.repositoryService = repositoryService;
      this.messageSender = messageSenderFactory.create(event);
      this.repository = event.getRepository();
      this.branchProvider = event.getContext().getBranchProvider();
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
      if (branchesAreModified(pullRequest)){
        if (isMerged(pullRequest)) {
          setMerged(pullRequest);
        } else {
          updated(pullRequest);
        }
      } else if (sourceBranchIsDeleted(pullRequest)) {
        setRejected(pullRequest);
      }
    }

    private boolean hasStatusOpen(PullRequest pullRequest) {
      return pullRequest.getStatus() == PullRequestStatus.OPEN;
    }

    private boolean branchesAreModified(PullRequest pullRequest) {
      return
        branchProvider.getCreatedOrModified().contains(pullRequest.getSource()) ||
          branchProvider.getCreatedOrModified().contains(pullRequest.getTarget());
    }

    private boolean isMerged(PullRequest pullRequest) {
      try {
        ChangesetPagingResult changesets = repositoryService
          .getLogCommand()
          .setBranch(pullRequest.getSource())
          .setAncestorChangeset(pullRequest.getTarget())
          .setPagingLimit(1)
          .getChangesets();
        return changesets.getTotal() == 0;
      } catch (IOException e) {
        throw new InternalRepositoryException(entity(PullRequest.class, pullRequest.getId()), "could not load changesets for pull request", e);
      }
    }

    private void setMerged(PullRequest pullRequest) {
      LOG.info("setting pull request {} to status MERGED", pullRequest.getId());
      String message = format("Merged pull request #%s (%s -> %s):", pullRequest.getId(), pullRequest.getSource(), pullRequest.getTarget());
      messageSender.sendMessageForPullRequest(pullRequest, message);
      service.setMerged(repository, pullRequest.getId());
    }

    private boolean sourceBranchIsDeleted(PullRequest pullRequest) {
      return branchProvider.getDeletedOrClosed().contains(pullRequest.getSource());
    }

    private void setRejected(PullRequest pullRequest) {
      LOG.info("setting pull request {} to status REJECTED", pullRequest.getId());
      String message = format("Rejected pull request #%s (%s -> %s):", pullRequest.getId(), pullRequest.getSource(), pullRequest.getTarget());
      messageSender.sendMessageForPullRequest(pullRequest, message);
      service.setRejected(repository, pullRequest.getId(), PullRequestRejectedEvent.RejectionCause.BRANCH_DELETED);
    }

    private void updated(PullRequest pullRequest) {
      LOG.info("pull request {} was updated", pullRequest.getId());
      service.updated(repository, pullRequest.getId());
    }
  }
}