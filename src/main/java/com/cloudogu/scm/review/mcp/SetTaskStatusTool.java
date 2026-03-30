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

import com.cloudogu.mcp.OkResultRenderer;
import com.cloudogu.mcp.ToolResult;
import com.cloudogu.mcp.TypedTool;
import com.cloudogu.scm.review.comment.service.Comment;
import com.cloudogu.scm.review.comment.service.CommentService;
import com.cloudogu.scm.review.comment.service.CommentTransition;
import com.cloudogu.scm.review.comment.service.CommentType;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import sonia.scm.plugin.Extension;
import sonia.scm.plugin.Requires;

@Extension
@Requires("scm-mcp-plugin")
class SetTaskStatusTool implements TypedTool<SetTaskStatusToolInput> {

  private final CommentService commentService;

  @Inject
  public SetTaskStatusTool(CommentService commentService) {
    this.commentService = commentService;
  }

  @Override
  public Class<? extends SetTaskStatusToolInput> getInputClass() {
    return SetTaskStatusToolInput.class;
  }

  @Override
  public ToolResult execute(SetTaskStatusToolInput input) {
    Comment comment = commentService.get(
      input.getNamespace(),
      input.getName(),
      input.getPullRequestId(),
      input.getCommentId()
    );
    if (comment == null) {
      return ToolResult.error("The task with the given ID does not exist. Please check your values.");
    }
    if (comment.getType() == CommentType.COMMENT) {
      return ToolResult.error("The comment with the given ID is no task and cannot be set to done or be reopened.");
    }
    if (comment.getType() != CommentType.TASK_TODO) {
      return ToolResult.error("The task is already completed.");
    }
    commentService.transform(
      input.getNamespace(),
      input.getName(),
      input.getPullRequestId(),
      input.getCommentId(),
      CommentTransition.SET_DONE
    );
    return OkResultRenderer.success("The task has been modified.").render();
  }

  @Override
  public String getName() {
    return "set-pull-request-task-done";
  }

  @Override
  public String getDescription() {
    return "Set the status of an existing open task in a pull request do 'done'.";
  }
}

@Data
@NoArgsConstructor
@AllArgsConstructor
class SetTaskStatusToolInput {

  @NotBlank
  @JsonPropertyDescription("The namespace of the repository.")
  String namespace;

  @NotBlank
  @JsonPropertyDescription("The name of the repository.")
  String name;

  @NotBlank
  @JsonPropertyDescription("The ID of the pull request.")
  String pullRequestId;

  @NotBlank
  @JsonPropertyDescription("The unique ID of the task comment (e.g., 'Bff3Ikf54a').")
  String commentId;
}
