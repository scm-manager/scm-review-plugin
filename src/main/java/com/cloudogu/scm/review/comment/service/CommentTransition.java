/*
 * MIT License
 *
 * Copyright (c) 2020-present Cloudogu GmbH and Contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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
