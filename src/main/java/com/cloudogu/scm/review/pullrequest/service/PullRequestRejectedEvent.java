package com.cloudogu.scm.review.pullrequest.service;

import sonia.scm.event.Event;
import sonia.scm.repository.Repository;

@Event
public class PullRequestRejectedEvent extends BasicPullRequestEvent {

  private final RejectionCause cause;

  public PullRequestRejectedEvent(Repository repository, PullRequest pullRequest, RejectionCause cause) {
    super(repository, pullRequest);
    this.cause = cause;
  }

  public RejectionCause getCause() {
    return cause;
  }

  public enum RejectionCause {
    BRANCH_DELETED,
    REJECTED_BY_USER
  }
}
