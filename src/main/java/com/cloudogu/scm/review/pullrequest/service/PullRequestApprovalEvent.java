package com.cloudogu.scm.review.pullrequest.service;

import sonia.scm.event.Event;
import sonia.scm.repository.Repository;

@Event
public class PullRequestApprovalEvent extends BasicPullRequestEvent {

  private final ApprovalCause cause;

  public PullRequestApprovalEvent(Repository repository, PullRequest pullRequest, ApprovalCause cause) {
    super(repository, pullRequest);
    this.cause = cause;
  }

  public ApprovalCause getCause() {
    return cause;
  }

  public enum ApprovalCause {
    APPROVED,
    APPROVAL_REMOVED
  }
}
