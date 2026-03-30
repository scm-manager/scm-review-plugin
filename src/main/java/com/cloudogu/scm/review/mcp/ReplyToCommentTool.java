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
import com.cloudogu.scm.review.comment.service.Reply;
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
class ReplyToCommentTool implements TypedTool<ReplyToCommentToolInput> {

  private final CommentService commentService;

  @Inject
  public ReplyToCommentTool(CommentService commentService) {
    this.commentService = commentService;
  }

  @Override
  public Class<? extends ReplyToCommentToolInput> getInputClass() {
    return ReplyToCommentToolInput.class;
  }

  @Override
  public ToolResult execute(ReplyToCommentToolInput input) {
    Comment comment = commentService.get(
      input.getNamespace(),
      input.getName(),
      input.getPullRequestId(),
      input.getCommentId()
    );
    if (comment == null) {
      return ToolResult.error("The comment with the given ID does not exist. Please check your values.");
    }
    commentService.reply(
      input.getNamespace(),
      input.getName(),
      input.getPullRequestId(),
      input.getCommentId(),
      Reply.createNewReply(input.getText())
    );
    return OkResultRenderer.success("The reply has been submitted.").render();
  }

  @Override
  public String getName() {
    return "reply-to-pull-request-comment";
  }

  @Override
  public String getDescription() {
    return "Reply to comments or tasks in a pull request.";
  }
}

@Data
@NoArgsConstructor
@AllArgsConstructor
class ReplyToCommentToolInput {

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
  @JsonPropertyDescription("The unique ID of the comment you are replying to (e.g., 'Bff3Ikf54a').")
  String commentId;

  @NotBlank
  @JsonPropertyDescription("The markdown-formatted text of your reply.")
  String text;
}
