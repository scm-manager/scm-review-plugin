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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// AI generated
class CommentGrouper {

  static GroupedComments group(Collection<Comment> comments) {
    List<Comment> globalComments = new ArrayList<>();
    Map<String, CommentAccumulator> accumulatorMap = new HashMap<>();

    for (Comment comment : comments) {
      Location loc = comment.getLocation();

      // 1. Handle Global Comments
      if (loc == null) {
        globalComments.add(comment);
        continue;
      }

      String file = loc.getFile();
      // Fallback: If location exists but has no file, treat as global
      if (file == null) {
        globalComments.add(comment);
        continue;
      }

      // 2. Separate File vs. Line Comments
      boolean isFileOnly = loc.getHunk() == null;

      CommentAccumulator acc = accumulatorMap.computeIfAbsent(file, k -> new CommentAccumulator());

      if (isFileOnly) {
        acc.fileComments.add(comment);
      } else {
        acc.lineComments.add(comment);
      }
    }

    // 3. Convert accumulated data into your immutable records
    Map<String, FileRelatedComments> fileRelatedCommentsMap = new HashMap<>();
    for (Map.Entry<String, CommentAccumulator> entry : accumulatorMap.entrySet()) {
      fileRelatedCommentsMap.put(
        entry.getKey(),
        new FileRelatedComments(
          entry.getValue().fileComments,
          entry.getValue().lineComments
        )
      );
    }

    return new GroupedComments(globalComments, fileRelatedCommentsMap);
  }

  /**
   * A simple private helper class to accumulate comments before
   * locking them into the immutable FileRelatedComments record.
   */
  private static class CommentAccumulator {
    List<Comment> fileComments = new ArrayList<>();
    List<Comment> lineComments = new ArrayList<>();
  }

  record GroupedComments(
    Collection<Comment> globalComments,
    Map<String, FileRelatedComments> fileRelatedCommentsMap
  ) {}

  record FileRelatedComments(
    Collection<Comment> fileComments,
    Collection<Comment> lineComments
  ) {}
}
