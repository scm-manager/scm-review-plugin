package com.cloudogu.scm.review.comment.service;

import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import sonia.scm.HandlerEventType;
import sonia.scm.event.Event;
import sonia.scm.event.HandlerEvent;
import sonia.scm.repository.Repository;

@Event
public class CommentEvent implements HandlerEvent<PullRequestComment> {

  final private PullRequestComment comment;
  final private PullRequestComment oldComment;
  final private PullRequest pullRequest;

  final private Repository repository;

  final private HandlerEventType type;

  public CommentEvent(PullRequestComment comment, PullRequestComment oldComment, PullRequest pullRequest, Repository repository, HandlerEventType type) {
    this.comment = comment;
    this.oldComment = oldComment;
    this.pullRequest = pullRequest;
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
  public PullRequestComment getItem() {
    return comment;
  }

  @Override
  public PullRequestComment getOldItem() {
    return oldComment;
  }
}
