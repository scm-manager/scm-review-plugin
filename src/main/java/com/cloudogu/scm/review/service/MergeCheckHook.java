package com.cloudogu.scm.review.service;

import com.github.legman.Subscribe;
import sonia.scm.EagerSingleton;
import sonia.scm.plugin.Extension;
import sonia.scm.repository.ChangesetPagingResult;
import sonia.scm.repository.InternalRepositoryException;
import sonia.scm.repository.PostReceiveRepositoryHookEvent;
import sonia.scm.repository.Repository;
import sonia.scm.repository.api.RepositoryService;
import sonia.scm.repository.api.RepositoryServiceFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.util.List;

import static sonia.scm.ContextEntry.ContextBuilder.entity;

@EagerSingleton @Extension
public class MergeCheckHook {

  private final PullRequestService service;
  private final RepositoryServiceFactory serviceFactory;

  @Inject
  public MergeCheckHook(DefaultPullRequestService service, RepositoryServiceFactory serviceFactory) {
    this.service = service;
    this.serviceFactory = serviceFactory;
  }

  @Subscribe(async = false)
  public void checkForMerges(PostReceiveRepositoryHookEvent event) {
    List<PullRequest> pullRequests = service.getAll(event.getRepository().getNamespace(), event.getRepository().getName());
    try (RepositoryService repositoryService = serviceFactory.create(event.getRepository())) {
      pullRequests.forEach(pullRequest -> checkIfMerged(event.getRepository(), repositoryService, pullRequest));
    }
  }

  private void checkIfMerged(Repository repository, RepositoryService repositoryService, PullRequest pullRequest) {
    ChangesetPagingResult changesets;
    try {
      changesets = repositoryService.getLogCommand().setBranch(pullRequest.getSource()).setAncestorChangeset(pullRequest.getTarget()).setPagingLimit(1).getChangesets();
    } catch (IOException e) {
      throw new InternalRepositoryException(entity(PullRequest.class, pullRequest.getId()), "could not load changesets for pull request", e);
    }
    if (changesets.getTotal() == 0) {
      service.setStatus(repository, pullRequest, PullRequestStatus.MERGED);
    }
  }
}
