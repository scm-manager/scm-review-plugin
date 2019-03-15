package com.cloudogu.scm.review.pullrequest.service;

import lombok.Getter;
import sonia.scm.repository.Repository;

@Getter
public class BasicPullRequestEvent {
  protected final Repository repository;
  protected final PullRequest pullRequest;

  public BasicPullRequestEvent(Repository repository, PullRequest pullRequest) {
    this.repository = repository;
    this.pullRequest = pullRequest;
  }
}
