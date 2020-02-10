package com.cloudogu.scm.review.comment.service;

import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import sonia.scm.repository.Repository;

import javax.inject.Inject;
import java.util.stream.Stream;

public class CommentCollector {

  private final CommentService commentService;

  @Inject
  public CommentCollector(CommentService commentService) {
    this.commentService = commentService;
  }

  public Stream<Comment> collectNonOutdated(Repository repository, PullRequest pullRequest) {
    return commentService.getAll(repository.getNamespace(), repository.getName(), pullRequest.getId())
      .stream()
      .filter(c -> !c.isOutdated());
  }
}
