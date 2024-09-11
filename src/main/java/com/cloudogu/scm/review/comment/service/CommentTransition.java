/*
 * Copyright (c) 2020 - present Cloudogu GmbH
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Affero General Public License as published by the Free
 * Software Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see https://www.gnu.org/licenses/.
 */

package com.cloudogu.scm.review.comment.service;

import java.util.function.Consumer;

import static com.cloudogu.scm.review.comment.service.CommentType.COMMENT;
import static com.cloudogu.scm.review.comment.service.CommentType.TASK_DONE;
import static com.cloudogu.scm.review.comment.service.CommentType.TASK_TODO;
import static sonia.scm.ScmConstraintViolationException.Builder.doThrow;

public enum CommentTransition implements Transition {

  SET_DONE(comment -> {
    doThrow()
      .violation("cannot set comment with type " + comment.getType() + " done", "transition", "name")
      .when(comment.getType() != TASK_TODO);
    comment.setType(TASK_DONE);
  }),
  MAKE_TASK(comment -> {
    doThrow()
      .violation("cannot make comment with type " + comment.getType() + " to task", "transition", "name")
      .when(comment.getType() != COMMENT);
    comment.setType(TASK_TODO);
  }),
  MAKE_COMMENT(comment -> {
    doThrow()
      .violation("cannot make comment with type " + comment.getType() + " to comment", "transition", "name")
      .when(comment.getType() != TASK_DONE && comment.getType() != TASK_TODO);
    comment.setType(COMMENT);
  }),
  REOPEN(comment -> {
    doThrow()
      .violation("cannot reopen comment with type " + comment.getType(), "transition", "name")
      .when(comment.getType() != TASK_DONE);
    comment.setType(TASK_TODO);
  });

  private final Consumer<Comment> transition;

  CommentTransition(Consumer<Comment> transition) {
    this.transition = transition;
  }

  void accept(Comment comment) {
    this.transition.accept(comment);
  }
}
