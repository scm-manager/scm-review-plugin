package com.cloudogu.scm.review.comment.service;

import java.util.function.Consumer;

import static com.cloudogu.scm.review.comment.service.CommentType.COMMENT;
import static com.cloudogu.scm.review.comment.service.CommentType.TASK_DONE;
import static com.cloudogu.scm.review.comment.service.CommentType.TASK_TODO;

public enum CommentTransition implements Consumer<Comment> {
  SET_DONE {
    @Override
    public void accept(Comment comment) {
      if (comment.getType() != TASK_TODO) {
        throw new IllegalStateException("cannot set comment with type " + comment.getType() + " done");
      }
      comment.setType(TASK_DONE);
    }
  },
  MAKE_TASK {
    @Override
    public void accept(Comment comment) {
      if (comment.getType() != COMMENT) {
        throw new IllegalStateException("cannot make comment with type " + comment.getType() + " to task");
      }
      comment.setType(TASK_TODO);
    }
  },
  REOPEN {
    @Override
    public void accept(Comment comment) {
      if (comment.getType() != TASK_DONE) {
        throw new IllegalStateException("cannot reopen comment with type " + comment.getType());
      }
      comment.setType(TASK_TODO);
    }
  }
}
