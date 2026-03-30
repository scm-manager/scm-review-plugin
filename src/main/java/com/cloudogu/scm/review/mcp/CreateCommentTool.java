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
import com.cloudogu.scm.review.comment.service.CommentType;
import com.cloudogu.scm.review.comment.service.Location;
import com.cloudogu.scm.review.comment.service.LocationForCommentNotFoundException;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import sonia.scm.plugin.Extension;
import sonia.scm.plugin.Requires;

@Extension
@Requires("scm-mcp-plugin")
class CreateCommentTool implements TypedTool<CreateCommentToolInput> {

  private final CommentService commentService;

  @Inject
  public CreateCommentTool(CommentService commentService) {
    this.commentService = commentService;
  }

  @Override
  public Class<? extends CreateCommentToolInput> getInputClass() {
    return CreateCommentToolInput.class;
  }

  @Override
  public ToolResult execute(CreateCommentToolInput input) {
    Comment newComment = new Comment();
    newComment.setComment(input.getText());
    newComment.setType(input.getType() == CreateCommentType.SIMPLE ? CommentType.COMMENT : CommentType.TASK_TODO);
    FileReference fileReference = input.getFileReference();
    if (fileReference != null) {
      String filename = getFilenameWithoutLeadingSlash(fileReference);
      newComment.setLocation(new Location(filename));
      newComment.getLocation().setNewLineNumber(fileReference.getNewLineNumber());
      newComment.getLocation().setOldLineNumber(fileReference.getOldLineNumber());
    }
    try {
      String newId = commentService.add(input.getNamespace(), input.getName(), input.pullRequestId, newComment);
      return OkResultRenderer
        .success("The comment has been submitted.")
        .withInfoText("The id of the comment is `" + newId + "`.")
        .render();
    } catch (LocationForCommentNotFoundException e) {
      return ToolResult.error(
        "The location could not be found in the diff. Please make sure, that you have picked the correct file name " +
          "and line number from the diff.");
    }
  }

  private static String getFilenameWithoutLeadingSlash(FileReference fileReference) {
    String filename = fileReference.getFilename();
    while (filename != null && filename.startsWith("/")) {
      filename = filename.substring(1);
    }
    return filename;
  }

  @Override
  public String getName() {
    return "create-pull-request-comment";
  }

  @Override
  public String getDescription() {
    return """
      Creates a new comment or task on a pull request.
      This can be created for the pull request as such, specific for a modified file,
      or concrete for an added or deleted line in the diff.""";
  }
}

@Data
class CreateCommentToolInput {
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
  @JsonPropertyDescription("The markdown-formatted text of the comment")
  String text;

  @NotNull
  @JsonPropertyDescription("Whether this is a SIMPLE comment or an OPEN_TASK.")
  CreateCommentType type;

  @JsonPropertyDescription("When the comment references a specific file or a changed line in a file, specify this. Leave null for global PR comments.")
  FileReference fileReference;
}

@Data
class FileReference {
  @NotBlank
  @JsonPropertyDescription("The complete path of the file. If the comment is not meant to reference the whole file as such, specify either 'addedLineNumber' or 'deletedLineNumber', too.")
  String filename;

  @JsonPropertyDescription("To comment on an added or an unchanged line, set this to the new line number.")
  Integer newLineNumber;
  @JsonPropertyDescription("To comment on a deleted line or an unchanged line, set this to the old line number.")
  Integer oldLineNumber;
}

enum CreateCommentType {
  SIMPLE,
  OPEN_TASK
}
