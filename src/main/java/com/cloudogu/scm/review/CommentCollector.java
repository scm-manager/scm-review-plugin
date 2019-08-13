package com.cloudogu.scm.review;

import com.cloudogu.scm.review.comment.service.Comment;
import com.cloudogu.scm.review.comment.service.CommentService;
import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import sonia.scm.repository.Repository;

import javax.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;

class CommentCollector {

  private final CommentService commentService;

  @Inject
  CommentCollector(CommentService commentService) {
    this.commentService = commentService;
  }

  List<Comment> collectNonOutdated(Repository repository, PullRequest pullRequest) {
    return commentService.getAll(repository.getNamespace(), repository.getName(), pullRequest.getId())
      .stream()
      .filter(c -> !c.isOutdated())
      .collect(Collectors.toList());
  }
}
