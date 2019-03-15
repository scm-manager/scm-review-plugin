package com.cloudogu.scm.review.pullrequest.service;

import sonia.scm.event.Event;
import sonia.scm.repository.Repository;

@Event
public class PullRequestMergedEvent {

  final private PullRequest pullRequest;

  public PullRequestMergedEvent(PullRequest pullRequest, Repository repository) {
    this.pullRequest = pullRequest;
    this.repository = repository;
  }

  public PullRequest getPullRequest() {
    return pullRequest;
  }

  public Repository getRepository() {
    return repository;
  }

  final private Repository repository;




}
