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

import com.cloudogu.scm.review.comment.service.Comment;
import com.cloudogu.scm.review.comment.service.Location;
import com.cloudogu.scm.review.mcp.CommentGrouper.FileRelatedComments;
import com.cloudogu.scm.review.mcp.CommentGrouper.GroupedComments;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// AI generated
class CommentGrouperTest {

  @Test
  void shouldGroupGlobalCommentsAndFallbackGlobalComments() {
    // Given
    Comment pureGlobal = createComment(null);
    Comment fallbackGlobal = createComment(new Location(null, null, null, null));

    // When
    GroupedComments result = CommentGrouper.group(List.of(pureGlobal, fallbackGlobal));

    // Then
    assertThat(result.globalComments())
      .as("Global comments should contain both pure null locations and locations missing a file")
      .containsExactlyInAnyOrder(pureGlobal, fallbackGlobal);

    assertThat(result.fileRelatedCommentsMap())
      .as("File related map should be empty when only global comments exist")
      .isEmpty();
  }

  @Test
  void shouldGroupFileRelatedComments() {
    // Given
    Location fileLoc1 = new Location("src/Main.java");
    Location fileLoc2 = new Location("src/Utils.java");

    Comment fileComment1 = createComment(fileLoc1);
    Comment fileComment2 = createComment(fileLoc2);

    // When
    GroupedComments result = CommentGrouper.group(List.of(fileComment1, fileComment2));

    // Then
    assertThat(result.globalComments()).isEmpty();

    assertThat(result.fileRelatedCommentsMap())
      .containsOnlyKeys("src/Main.java", "src/Utils.java");

    assertThat(result.fileRelatedCommentsMap().get("src/Main.java").fileComments())
      .containsExactly(fileComment1);
    assertThat(result.fileRelatedCommentsMap().get("src/Main.java").lineComments())
      .isEmpty();

    assertThat(result.fileRelatedCommentsMap().get("src/Utils.java").fileComments())
      .containsExactly(fileComment2);
  }

  @Test
  void shouldGroupLineRelatedComments() {
    // Given
    Location hunkLoc = new Location("src/Main.java", "@@ -1,5 +1,5 @@", null, null);
    Location lineLoc = new Location("src/Main.java", "@@ -10,5 +10,5 @@", 10, null);

    Comment hunkComment = createComment(hunkLoc);
    Comment lineComment = createComment(lineLoc);

    // When
    GroupedComments result = CommentGrouper.group(List.of(hunkComment, lineComment));

    // Then
    assertThat(result.globalComments()).isEmpty();

    assertThat(result.fileRelatedCommentsMap())
      .containsOnlyKeys("src/Main.java");

    FileRelatedComments fileGroup = result.fileRelatedCommentsMap().get("src/Main.java");

    assertThat(fileGroup.fileComments()).isEmpty();
    assertThat(fileGroup.lineComments())
      .containsExactlyInAnyOrder(hunkComment, lineComment);
  }

  @Test
  void shouldHandleMixedCommentsCorrectly() {
    // Given
    Comment global = createComment(null);
    Comment fileComment = createComment(new Location("README.md"));
    Comment lineComment = createComment(new Location("README.md", "@@ -1,5 +1,5 @@", null, 5));

    // When
    GroupedComments result = CommentGrouper.group(List.of(global, fileComment, lineComment));

    // Then
    assertThat(result.globalComments()).containsExactly(global);

    assertThat(result.fileRelatedCommentsMap()).containsOnlyKeys("README.md");

    FileRelatedComments readmeGroup = result.fileRelatedCommentsMap().get("README.md");
    assertThat(readmeGroup.fileComments()).containsExactly(fileComment);
    assertThat(readmeGroup.lineComments()).containsExactly(lineComment);
  }

  private static Comment createComment(Location location) {
    Comment comment = new Comment();
    comment.setLocation(location);
    return comment;
  }
}
