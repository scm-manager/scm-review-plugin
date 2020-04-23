package com.cloudogu.scm.review.workflow;

import com.cloudogu.scm.review.comment.service.Comment;
import com.cloudogu.scm.review.comment.service.CommentService;
import com.cloudogu.scm.review.comment.service.CommentType;
import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import sonia.scm.plugin.Extension;
import sonia.scm.repository.Repository;

import javax.inject.Inject;
import java.util.List;

@Extension
public class AllTasksDoneRule implements Rule {

  private final CommentService commentService;

  @Inject
  public AllTasksDoneRule(CommentService commentService) {
    this.commentService = commentService;
  }

  @Override
  public Result validate(Context context) {
    if (hasOpenTasks(context)) {
      return failed();
    }
    return success();
  }

  private boolean hasOpenTasks(Context context) {
    return getComments(context).stream().anyMatch(c -> c.getType() == CommentType.TASK_TODO);
  }

  private List<Comment> getComments(Context context) {
    Repository repository = context.getRepository();
    PullRequest pullRequest = context.getPullRequest();
    return commentService.getAll(repository.getNamespace(), repository.getName(), pullRequest.getId());
  }
}
