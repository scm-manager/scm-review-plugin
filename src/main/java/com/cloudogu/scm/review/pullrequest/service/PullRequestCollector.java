package com.cloudogu.scm.review.pullrequest.service;

import sonia.scm.repository.Repository;

import javax.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;

public class PullRequestCollector {

  private final PullRequestService pullRequestService;

  @Inject
  public PullRequestCollector(PullRequestService pullRequestService) {
    this.pullRequestService = pullRequestService;
  }

  public List<PullRequest> collectAffectedPullRequests(Repository repository, List<String> affectedBranches) {
    return pullRequestService.getAll(repository.getNamespace(), repository.getName())
      .stream()
      .filter(pr -> pr.getStatus() == PullRequestStatus.OPEN)
      .filter(pr -> affectedBranches.contains(pr.getSource()) || affectedBranches.contains(pr.getTarget()))
      .collect(Collectors.toList());
  }
}
