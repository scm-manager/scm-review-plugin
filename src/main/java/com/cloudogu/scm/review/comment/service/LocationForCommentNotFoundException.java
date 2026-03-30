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

import sonia.scm.BadRequestException;
import sonia.scm.repository.NamespaceAndName;

import static sonia.scm.ContextEntry.ContextBuilder.entity;

public class LocationForCommentNotFoundException extends BadRequestException {

  LocationForCommentNotFoundException(NamespaceAndName namespaceAndName, String pullRequestId, String fileName) {
    super(entity("File", fileName)
      .in("PullRequest", pullRequestId)
      .in(namespaceAndName)
      .build(),
      "file not found in diff for pull request"
    );
  }

  LocationForCommentNotFoundException(NamespaceAndName namespaceAndName, String pullRequestId, Location location) {
    super(
      entity("Hunk", location.getHunk())
        .in("File", location.getFile())
        .in("PullRequest", pullRequestId)
        .in(namespaceAndName)
        .build(),
      "hunk not found in diff for pull request"
    );
  }

  public LocationForCommentNotFoundException(NamespaceAndName namespaceAndName, String pullRequestId, Location location, Integer lineNumber) {
    super(
      entity(location.getNewLineNumber() != null && location.getNewLineNumber().equals(lineNumber) ?
             "NewLineNumber" :
             "OldLineNumber",
        Integer.toString(lineNumber)
      )
        .in("File", location.getFile())
        .in("PullRequest", pullRequestId)
        .in(namespaceAndName)
        .build(),
      "line number not found in diff for pull request"
    );
  }

  @Override
  public String getCode() {
    return "DpVC2GL8S1";
  }
}
