package com.cloudogu.scm.review.comment.service;

import java.util.function.Consumer;

import static com.cloudogu.scm.review.comment.service.CommentType.COMMENT;
import static com.cloudogu.scm.review.comment.service.CommentType.TASK_DONE;
import static com.cloudogu.scm.review.comment.service.CommentType.TASK_TODO;
import static sonia.scm.ScmConstraintViolationException.Builder.doThrow;

public enum CommentTransition implements Consumer<Comment>, Transition {
  SET_DONE {
    @Override
    public void accept(Comment comment) {
      doThrow()
        .violation("cannot set comment with type " + comment.getType() + " done", "transition", "name")
        .when(comment.getType() != TASK_TODO);
      comment.setType(TASK_DONE);
    }
  },
  MAKE_TASK {
    @Override
    public void accept(Comment comment) {
      doThrow()
        .violation("cannot make comment with type " + comment.getType() + " to task", "transition", "name")
        .when(comment.getType() != COMMENT);
      comment.setType(TASK_TODO);
    }
  },
  REOPEN {
    @Override
    public void accept(Comment comment) {
      doThrow()
        .violation("cannot reopen comment with type " + comment.getType(), "transition", "name")
        .when(comment.getType() != TASK_DONE);
      comment.setType(TASK_TODO);
    }
  }
}
