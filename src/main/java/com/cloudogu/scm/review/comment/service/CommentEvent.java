package com.cloudogu.scm.review.comment.service;

import com.cloudogu.scm.review.pullrequest.service.BasicPullRequestEvent;
import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import sonia.scm.HandlerEventType;
import sonia.scm.event.Event;
import sonia.scm.event.HandlerEvent;
import sonia.scm.repository.Repository;

@Event
public class CommentEvent extends BasicPullRequestEvent implements HandlerEvent<PullRequestComment> {

  private final PullRequestComment comment;
  private final PullRequestComment oldComment;
  private final HandlerEventType type;

  public CommentEvent(Repository repository, PullRequest pullRequest, PullRequestComment comment, PullRequestComment oldComment, HandlerEventType type) {
    super(repository, pullRequest);
    this.comment = comment;
    this.oldComment = oldComment;
    this.type = type;
  }

  @Override
  public PullRequestComment getItem() {
    return comment;
  }

  @Override
  public PullRequestComment getOldItem() {
    return oldComment;
  }

  @Override
  public HandlerEventType getEventType() {
    return type;
  }
}
