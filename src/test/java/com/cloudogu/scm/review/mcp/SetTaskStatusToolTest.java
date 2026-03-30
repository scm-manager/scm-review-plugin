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

package com.cloudogu.scm.review.mcp;

import com.cloudogu.mcp.ToolResult;
import com.cloudogu.scm.review.comment.service.Comment;
import com.cloudogu.scm.review.comment.service.CommentService;
import com.cloudogu.scm.review.comment.service.CommentTransition;
import com.cloudogu.scm.review.comment.service.CommentType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SetTaskStatusToolTest {

  @Mock
  private CommentService commentService;
  @InjectMocks
  private SetTaskStatusTool tool;

  @Test
  void shouldSetTaskDone() {
    Comment task = new Comment();
    task.setType(CommentType.TASK_TODO);
    when(commentService.get("hitchhiker", "hog", "42", "1337"))
      .thenReturn(task);

    SetTaskStatusToolInput input = new SetTaskStatusToolInput("hitchhiker", "hog", "42", "1337");

    ToolResult result = tool.execute(input);

    assertThat(result.getContent().get(0))
      .isEqualTo("STATUS: [SUCCESS] The task has been modified.\n");
    verify(commentService)
      .transform(
        "hitchhiker",
        "hog",
        "42",
        "1337",
        CommentTransition.SET_DONE
      );
  }

  @Test
  void shouldFailIfTaskDoesNotExist() {
    when(commentService.get("hitchhiker", "hog", "42", "1337"))
      .thenReturn(null);

    SetTaskStatusToolInput input = new SetTaskStatusToolInput("hitchhiker", "hog", "42", "1337");

    ToolResult result = tool.execute(input);

    assertThat(result.isError()).isTrue();
    assertThat(result.getMessage())
      .isEqualTo("The task with the given ID does not exist. Please check your values.");
  }

  @Test
  void shouldFailIfNoTask() {
    Comment comment = new Comment();
    comment.setType(CommentType.COMMENT);
    when(commentService.get("hitchhiker", "hog", "42", "1337"))
      .thenReturn(comment);

    SetTaskStatusToolInput input = new SetTaskStatusToolInput("hitchhiker", "hog", "42", "1337");

    ToolResult result = tool.execute(input);

    assertThat(result.isError()).isTrue();
    assertThat(result.getMessage())
      .isEqualTo("The comment with the given ID is no task and cannot be set to done or be reopened.");
    verify(commentService, never())
      .transform(
        anyString(),
        anyString(),
        anyString(),
        anyString(),
        any()
      );
  }

  @Test
  void shouldFailIfTaskNotOpen() {
    Comment task = new Comment();
    task.setType(CommentType.TASK_DONE);
    when(commentService.get("hitchhiker", "hog", "42", "1337"))
      .thenReturn(task);

    SetTaskStatusToolInput input = new SetTaskStatusToolInput("hitchhiker", "hog", "42", "1337");

    ToolResult result = tool.execute(input);

    assertThat(result.isError()).isTrue();
    assertThat(result.getMessage())
      .isEqualTo("The task is already completed.");
    verify(commentService, never())
      .transform(
        anyString(),
        anyString(),
        anyString(),
        anyString(),
        any()
      );
  }
}
