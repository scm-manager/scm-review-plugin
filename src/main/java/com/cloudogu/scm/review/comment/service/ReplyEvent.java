package com.cloudogu.scm.review.comment.service;

import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import sonia.scm.HandlerEventType;
import sonia.scm.repository.Repository;

public class ReplyEvent extends BasicCommentEvent<Reply> {
  ReplyEvent(Repository repository, PullRequest pullRequest, Reply comment, Reply oldComment, HandlerEventType type) {
    super(repository, pullRequest, comment, oldComment, type);
  }
}
