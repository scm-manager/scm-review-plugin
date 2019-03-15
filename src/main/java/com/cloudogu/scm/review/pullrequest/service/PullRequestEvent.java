package com.cloudogu.scm.review.pullrequest.service;

import sonia.scm.HandlerEventType;
import sonia.scm.event.Event;
import sonia.scm.event.HandlerEvent;
import sonia.scm.repository.Repository;

@Event
public class PullRequestEvent implements HandlerEvent<PullRequest> {

  final private PullRequest pullRequest;
  final private PullRequest oldPullRequest;

  final private Repository repository;

  final private HandlerEventType type;

  public PullRequestEvent(PullRequest pullRequest, PullRequest oldPullRequest, Repository repository, HandlerEventType type) {
    this.pullRequest = pullRequest;
    this.oldPullRequest = oldPullRequest;
    this.repository = repository;
    this.type = type;
  }

  public PullRequest getPullRequest() {
    return pullRequest;
  }

  public Repository getRepository() {
    return repository;
  }

  @Override
  public HandlerEventType getEventType() {
    return type;
  }

  @Override
  public PullRequest getItem() {
    return pullRequest;
  }

  @Override
  public PullRequest getOldItem() {
    return oldPullRequest;
  }
}
