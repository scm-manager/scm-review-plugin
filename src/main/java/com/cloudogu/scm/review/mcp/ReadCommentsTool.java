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
import com.cloudogu.scm.review.comment.service.ContextLine;
import com.cloudogu.scm.review.comment.service.Reply;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.google.common.base.MoreObjects;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import sonia.scm.plugin.Extension;
import sonia.scm.plugin.Requires;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.OptionalInt;

@Extension
@Requires("scm-mcp-plugin")
class ReadCommentsTool implements TypedTool<ReadCommentsToolInput> {

  public static final int PADDING_WIDTH = 4;
  public static final String FORMAT_TEMPLATE = "[ %" + PADDING_WIDTH + "s | %" + PADDING_WIDTH + "s ] ";

  private final CommentService commentService;

  @Inject
  public ReadCommentsTool(CommentService commentService) {
    this.commentService = commentService;
  }

  @Override
  public Class<? extends ReadCommentsToolInput> getInputClass() {
    return ReadCommentsToolInput.class;
  }

  @Override
  public ToolResult execute(ReadCommentsToolInput input) {
    List<Comment> comments = commentService.getAll(input.getNamespace(), input.getName(), input.getId());
    OkResultRenderer resultRenderer = OkResultRenderer.success("Comments have been read successfully.");

    CommentGrouper.GroupedComments groupedComments = CommentGrouper.group(comments);

    if (!groupedComments.globalComments().isEmpty()) {
      resultRenderer.appendLine("# Global Comments");
      resultRenderer.appendLine("");

      groupedComments.globalComments().forEach(comment -> renderComment(resultRenderer, comment));
    }

    groupedComments.fileRelatedCommentsMap().forEach((file, fileComments) -> {

      resultRenderer.append("# File Comments: `").append(file).appendLine("`");
      resultRenderer.appendLine("");

      fileComments.fileComments().forEach(comment -> renderComment(resultRenderer, comment));

      fileComments.lineComments().forEach(comment -> renderLineComment(resultRenderer, comment));
    });

    return resultRenderer.render();
  }

  @Override
  public String getName() {
    return "read-pull-request-comments";
  }

  @Override
  public String getDescription() {
    return "Read all comments, tasks and replies for a pull request.";
  }

  private void renderComment(OkResultRenderer resultRenderer, Comment comment) {
    renderCommentHeader(resultRenderer, comment);
    renderPureComment(resultRenderer, comment);
    renderReplies(resultRenderer, comment);
  }

  private void renderLineComment(OkResultRenderer resultRenderer, Comment comment) {
    renderCommentHeader(resultRenderer, comment);
    renderCommentContext(resultRenderer, comment);
    renderPureComment(resultRenderer, comment);
    renderReplies(resultRenderer, comment);
  }

  private void renderPureComment(OkResultRenderer resultRenderer, Comment comment) {
    resultRenderer.append("<comment_body id='")
      .append(comment.getId())
      .append("' timestamp='")
      .append(comment.getDate())
      .appendLine("'>");
    resultRenderer.appendLine(comment.getComment());
    resultRenderer.appendLine("</comment_body>");
    resultRenderer.appendLine("");
  }

  private void renderCommentContext(OkResultRenderer resultRenderer, Comment comment) {
    resultRenderer.append("*The comment refers to ")
      .append(comment.getLocation().getNewLineNumber() == null ? "old" : "new")
      .append(" line number ")
      .append(MoreObjects.firstNonNull(comment.getLocation().getNewLineNumber(),
        comment.getLocation().getOldLineNumber()
      ).toString());
    resultRenderer.appendLine(" of the file.*");
    resultRenderer.appendLine("The diff features explicit side-by-side line numbers (formatted as `[ old | new ]`)");
    resultRenderer.appendLine("");
    resultRenderer.appendLine("```diff");
    comment.getContext().getLines().forEach(line -> renderContextLine(resultRenderer, line));
    resultRenderer.appendLine("```");
    resultRenderer.appendLine("");
  }

  private void renderContextLine(OkResultRenderer resultRenderer, ContextLine line) {
    resultRenderer.append(
      String.format(
        FORMAT_TEMPLATE,
        lineNumberOrDash(line.getOldLineNumber()),
        lineNumberOrDash(line.getNewLineNumber())
      )
    );

    if (line.getNewLineNumber().isEmpty()) {
      resultRenderer.append("-");
    } else if (line.getOldLineNumber().isEmpty()) {
      resultRenderer.append("+");
    } else {
      resultRenderer.append(" ");
    }
    resultRenderer.appendLine(line.getContent());
  }

  private String lineNumberOrDash(OptionalInt lineNumber) {
    if (lineNumber.isPresent()) {
      return Integer.toString(lineNumber.getAsInt());
    }
    return "-";
  }

  private void renderCommentHeader(OkResultRenderer resultRenderer, Comment comment) {
    resultRenderer.append("**Type:** ")
      .append(computeType(comment))
      .append(" | **Author:** ")
      .appendLine(comment.getAuthor());
    resultRenderer.appendLine("");
  }

  private void renderReplies(OkResultRenderer resultRenderer, Comment comment) {
    for (Iterator<Reply> iterator = comment.getReplies().iterator(); iterator.hasNext(); ) {
      Reply reply = iterator.next();
      resultRenderer.append("> **Reply from:** ").appendLine(reply.getAuthor());
      resultRenderer.append("> <reply_body").append(" timestamp='").append(reply.getDate()).appendLine("'>");
      Arrays.stream(reply.getComment().split("\n")).forEach(line -> resultRenderer.append("> ").appendLine(line));
      resultRenderer.appendLine("> </reply_body>");
      if (iterator.hasNext()) {
        resultRenderer.appendLine(">");
      } else {
        resultRenderer.appendLine("");
      }
    }
  }

  private static String computeType(Comment comment) {
    return switch (comment.getType()) {
      case COMMENT -> "Simple";
      case TASK_TODO -> "Open Task";
      case TASK_DONE -> "Closed Task";
    };
  }
}

@Data
@NoArgsConstructor
@AllArgsConstructor
class ReadCommentsToolInput {
  @NotBlank
  @JsonPropertyDescription("The namespace of the repository with the pull request.")
  private String namespace;

  @NotBlank
  @JsonPropertyDescription("The name of the repository with the pull request.")
  private String name;

  @NotBlank
  @JsonPropertyDescription("The ID of the pull request.")
  private String id;
}
