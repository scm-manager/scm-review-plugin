package com.cloudogu.scm.review;

import com.cloudogu.scm.review.comment.service.CommentService;
import com.cloudogu.scm.review.comment.service.SystemCommentType;
import com.cloudogu.scm.review.pullrequest.service.DefaultPullRequestService;
import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import com.cloudogu.scm.review.pullrequest.service.PullRequestMergedEvent;
import com.cloudogu.scm.review.pullrequest.service.PullRequestRejectedEvent;
import com.cloudogu.scm.review.pullrequest.service.PullRequestStatus;
import com.github.legman.Subscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sonia.scm.EagerSingleton;
import sonia.scm.event.ScmEventBus;
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
public class MergeCheckHook {

  private static final Logger LOG = LoggerFactory.getLogger(MergeCheckHook.class);

  private final DefaultPullRequestService service;
  private final RepositoryServiceFactory serviceFactory;
  private final ScmEventBus eventBus;

  private final CommentService commentService;
  private final MessageSenderFactory messageSenderFactory;

  @Inject
  public MergeCheckHook(DefaultPullRequestService service, RepositoryServiceFactory serviceFactory, MessageSenderFactory messageSenderFactory, CommentService commentService, ScmEventBus eventBus) {
    this.service = service;
    this.serviceFactory = serviceFactory;
    this.commentService = commentService;
    this.messageSenderFactory = messageSenderFactory;
    this.eventBus = eventBus;
  }

  @Subscribe(async = false)
  public void checkForMerges(PostReceiveRepositoryHookEvent event) {
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
      pullRequests
        .stream()
        .filter(this::pullRequestHasStatusOpen)
        .filter(this::pullRequestBranchesAreModified)
        .filter(this::pullRequestIsMerged)
        .forEach(this::setPullRequestMerged);
      pullRequests
        .stream()
        .filter(this::pullRequestHasStatusOpen)
        .filter(this::pullRequestSourceBranchIsDeleted)
        .forEach(this::setPullRequestRejected);
    }

    private boolean pullRequestHasStatusOpen(PullRequest pullRequest) {
      return pullRequest.getStatus() == PullRequestStatus.OPEN;
    }

    private boolean pullRequestBranchesAreModified(PullRequest pullRequest) {
      return
        branchProvider.getCreatedOrModified().contains(pullRequest.getSource()) ||
          branchProvider.getCreatedOrModified().contains(pullRequest.getTarget());
    }

    private boolean pullRequestIsMerged(PullRequest pullRequest) {
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

    private void setPullRequestMerged(PullRequest pullRequest) {
      LOG.info("setting pull request {} to status MERGED", pullRequest.getId());
      String message = format("Merged pull request #%s (%s -> %s):", pullRequest.getId(), pullRequest.getSource(), pullRequest.getTarget());
      messageSender.sendMessageForPullRequest(pullRequest, message);
      service.setStatus(repository, pullRequest, PullRequestStatus.MERGED);
      commentService.addStatusChangedComment(repository, pullRequest.getId(), SystemCommentType.MERGED);
      eventBus.post(new PullRequestMergedEvent(repository,pullRequest));
    }

    private boolean pullRequestSourceBranchIsDeleted(PullRequest pullRequest) {
      return branchProvider.getDeletedOrClosed().contains(pullRequest.getSource());
    }

    private void setPullRequestRejected(PullRequest pullRequest) {
      LOG.info("setting pull request {} to status REJECTED", pullRequest.getId());
      String message = format("Rejected pull request #%s (%s -> %s):", pullRequest.getId(), pullRequest.getSource(), pullRequest.getTarget());
      messageSender.sendMessageForPullRequest(pullRequest, message);
      service.setStatus(repository, pullRequest, PullRequestStatus.REJECTED);
      commentService.addStatusChangedComment(repository, pullRequest.getId(), SystemCommentType.SOURCE_DELETED);
      eventBus.post(new PullRequestRejectedEvent(repository,pullRequest));
    }
  }
}
