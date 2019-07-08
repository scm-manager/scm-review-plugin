package com.cloudogu.scm.review.comment.service;

import com.cloudogu.scm.review.pullrequest.service.BasicPullRequestEvent;
import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import sonia.scm.HandlerEventType;
import sonia.scm.event.Event;
import sonia.scm.event.HandlerEvent;
import sonia.scm.repository.Repository;

@Event
public class CommentEvent extends BasicPullRequestEvent implements HandlerEvent<BasicComment> {

  private final BasicComment comment;
  private final BasicComment oldComment;
  private final HandlerEventType type;

  public CommentEvent(Repository repository, PullRequest pullRequest, BasicComment comment, BasicComment oldComment, HandlerEventType type) {
    super(repository, pullRequest);
    this.comment = comment;
    this.oldComment = oldComment;
    this.type = type;
  }

  @Override
  public BasicComment getItem() {
    return comment;
  }

  @Override
  public BasicComment getOldItem() {
    return oldComment;
  }

  @Override
  public HandlerEventType getEventType() {
    return type;
  }
}
