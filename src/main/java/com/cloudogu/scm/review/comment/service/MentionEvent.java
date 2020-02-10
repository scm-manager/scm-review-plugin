package com.cloudogu.scm.review.comment.service;

import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import sonia.scm.HandlerEventType;
import sonia.scm.event.Event;
import sonia.scm.repository.Repository;

@Event
public class MentionEvent extends BasicCommentEvent<Comment> {
  public MentionEvent(Repository repository, PullRequest pullRequest, Comment comment, Comment oldComment, HandlerEventType type) {
    super(repository, pullRequest, comment, oldComment, type);
  }
}

