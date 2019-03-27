package com.cloudogu.scm.review.pullrequest.service;

import sonia.scm.HandlerEventType;
import sonia.scm.event.Event;
import sonia.scm.event.HandlerEvent;
import sonia.scm.repository.Repository;

@Event
public class PullRequestEvent extends BasicPullRequestEvent implements HandlerEvent<PullRequest> {

  private final PullRequest oldPullRequest;
  private final HandlerEventType type;

  public PullRequestEvent(Repository repository, PullRequest pullRequest, PullRequest oldPullRequest, HandlerEventType type) {
    super(repository, pullRequest);
    this.oldPullRequest = oldPullRequest;
    this.type = type;
  }

  @Override
  public PullRequest getItem() {
    return pullRequest;
  }

  @Override
  public PullRequest getOldItem() {
    return oldPullRequest;
  }

  @Override
  public HandlerEventType getEventType() {
    return type;
  }
}
