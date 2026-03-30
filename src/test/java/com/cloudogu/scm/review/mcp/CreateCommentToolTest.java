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
import com.cloudogu.scm.review.comment.service.CommentService;
import com.cloudogu.scm.review.comment.service.CommentType;
import com.cloudogu.scm.review.comment.service.Location;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateCommentToolTest {

  @Mock
  private CommentService commentService;
  @InjectMocks
  private CreateCommentTool tool;

  private final CreateCommentToolInput input = new CreateCommentToolInput();

  @BeforeEach
  void prepareInput() {
    input.setNamespace("hitchhiker");
    input.setName("hog");
    input.setPullRequestId("42");
    input.setText("Nice one!");
  }

  @Test
  void shouldCreateGlobalComment() {
    when(commentService.add(anyString(), anyString(), anyString(), any()))
      .thenReturn("1337");

    input.setType(CreateCommentType.SIMPLE);
    ToolResult result = tool.execute(input);

    assertThat(result.getContent().get(0))
      .isEqualTo("""
        STATUS: [SUCCESS] The comment has been submitted.
        INFO: The id of the comment is `1337`.
        """);
    Mockito.verify(commentService).add(
      eq("hitchhiker"),
      eq("hog"),
      eq("42"),
      argThat(comment -> {
        assertThat(comment.getComment()).isEqualTo("Nice one!");
        assertThat(comment.getType()).isEqualTo(CommentType.COMMENT);
        assertThat(comment.getLocation()).isNull();
        return true;
      })
    );
  }

  @Test
  void shouldCreateGlobalTask() {
    when(commentService.add(anyString(), anyString(), anyString(), any()))
      .thenReturn("1337");

    input.setType(CreateCommentType.OPEN_TASK);
    ToolResult result = tool.execute(input);

    assertThat(result.getContent().get(0))
      .isEqualTo("""
        STATUS: [SUCCESS] The comment has been submitted.
        INFO: The id of the comment is `1337`.
        """);
    Mockito.verify(commentService).add(
      eq("hitchhiker"),
      eq("hog"),
      eq("42"),
      argThat(comment -> {
        assertThat(comment.getComment()).isEqualTo("Nice one!");
        assertThat(comment.getType()).isEqualTo(CommentType.TASK_TODO);
        assertThat(comment.getLocation()).isNull();
        return true;
      })
    );
  }

  @Test
  void shouldCreateFileComment() {
    FileReference fileReference = new FileReference();
    fileReference.setFilename("drive.md");
    input.setFileReference(fileReference);

    ToolResult result = tool.execute(input);

    assertThat(result.getContent().get(0))
      .startsWith("STATUS: [SUCCESS] The comment has been submitted.");
    Mockito.verify(commentService).add(
      eq("hitchhiker"),
      eq("hog"),
      eq("42"),
      argThat(comment -> {
        assertThat(comment.getComment()).isEqualTo("Nice one!");
        Location location = comment.getLocation();
        assertThat(location).isNotNull();
        assertThat(location.getFile()).isEqualTo("drive.md");
        assertThat(location.getHunk()).isNull();
        assertThat(location.getNewLineNumber()).isNull();
        assertThat(location.getOldLineNumber()).isNull();
        return true;
      })
    );
  }

  @Test
  void shouldCreateFileCommentAndRemoveLeadingSlash() {
    FileReference fileReference = new FileReference();
    fileReference.setFilename("/drive.md");
    input.setFileReference(fileReference);

    tool.execute(input);

    Mockito.verify(commentService).add(
      any(),
      anyString(),
      anyString(),
      argThat(comment -> {
        Location location = comment.getLocation();
        assertThat(location.getFile()).isEqualTo("drive.md");
        return true;
      })
    );
  }

  @Test
  void shouldCreateLineCommentForAddedLine() {
    FileReference fileReference = new FileReference();
    fileReference.setFilename("drive.md");
    fileReference.setNewLineNumber(3);
    input.setFileReference(fileReference);

    ToolResult result = tool.execute(input);

    assertThat(result.getContent().get(0))
      .startsWith("STATUS: [SUCCESS] The comment has been submitted.");
    Mockito.verify(commentService).add(
      eq("hitchhiker"),
      eq("hog"),
      eq("42"),
      argThat(comment -> {
        assertThat(comment.getComment()).isEqualTo("Nice one!");
        Location location = comment.getLocation();
        assertThat(location).isNotNull();
        assertThat(location.getFile()).isEqualTo("drive.md");
        assertThat(location.getNewLineNumber()).isEqualTo(3);
        assertThat(location.getOldLineNumber()).isNull();
        return true;
      })
    );
  }

  @Test
  void shouldCreateLineCommentForDeletedLine() {
    FileReference fileReference = new FileReference();
    fileReference.setFilename("drive.md");
    fileReference.setOldLineNumber(1);
    input.setFileReference(fileReference);

    ToolResult result = tool.execute(input);

    assertThat(result.getContent().get(0))
      .startsWith("STATUS: [SUCCESS] The comment has been submitted.");
    Mockito.verify(commentService).add(
      eq("hitchhiker"),
      eq("hog"),
      eq("42"),
      argThat(comment -> {
        assertThat(comment.getComment()).isEqualTo("Nice one!");
        Location location = comment.getLocation();
        assertThat(location).isNotNull();
        assertThat(location.getFile()).isEqualTo("drive.md");
        assertThat(location.getNewLineNumber()).isNull();
        assertThat(location.getOldLineNumber()).isEqualTo(1);
        return true;
      })
    );
  }
}
