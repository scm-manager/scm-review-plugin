package com.cloudogu.scm.review.pullrequest.service;

import sonia.scm.event.Event;
import sonia.scm.repository.Repository;

@Event
public class PullRequestUpdatedEvent extends BasicPullRequestEvent {
  public PullRequestUpdatedEvent(Repository repository, PullRequest pullRequest) {
    super(repository, pullRequest);
  }
}
