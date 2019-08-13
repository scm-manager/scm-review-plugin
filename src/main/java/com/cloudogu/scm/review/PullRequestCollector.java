package com.cloudogu.scm.review;

import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import com.cloudogu.scm.review.pullrequest.service.PullRequestService;
import com.cloudogu.scm.review.pullrequest.service.PullRequestStatus;
import sonia.scm.repository.Repository;

import javax.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;

class PullRequestCollector {

  private final PullRequestService pullRequestService;

  @Inject
  PullRequestCollector(PullRequestService pullRequestService) {
    this.pullRequestService = pullRequestService;
  }

  List<PullRequest> collectAffectedPullRequests(Repository repository, List<String> affectedBranches) {
    return pullRequestService.getAll(repository.getNamespace(), repository.getName())
      .stream()
      .filter(pr -> pr.getStatus() == PullRequestStatus.OPEN)
      .filter(pr -> affectedBranches.contains(pr.getSource()) || affectedBranches.contains(pr.getTarget()))
      .collect(Collectors.toList());
  }
}
