package com.cloudogu.scm.review.comment.service;

import com.cloudogu.scm.review.pullrequest.service.BasicPullRequestEvent;
import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import sonia.scm.HandlerEventType;
import sonia.scm.event.HandlerEvent;
import sonia.scm.repository.Repository;

public abstract class BasicCommentEvent<T extends BasicComment> extends BasicPullRequestEvent implements HandlerEvent<T> {

  private final T comment;
  private final T oldComment;
  private final HandlerEventType type;

  BasicCommentEvent(Repository repository, PullRequest pullRequest, T comment, T oldComment, HandlerEventType type) {
    super(repository, pullRequest);
    this.comment = comment;
    this.oldComment = oldComment;
    this.type = type;
  }

  @Override
  public T getItem() {
    return comment;
  }

  @Override
  public T getOldItem() {
    return oldComment;
  }

  @Override
  public HandlerEventType getEventType() {
    return type;
  }
}
