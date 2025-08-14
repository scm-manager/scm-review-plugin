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

package com.cloudogu.scm.review.comment.api;

import com.cloudogu.scm.review.pullrequest.dto.BranchRevisionResolver;
import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import com.google.common.base.Strings;
import sonia.scm.ConcurrentModificationException;

public class RevisionChecker {
  static void checkRevision(BranchRevisionResolver branchRevisionResolver, String namespace, String name, String pullRequestId, String expectedSourceRevision, String expectedTargetRevision) {
    if (!Strings.isNullOrEmpty(expectedSourceRevision) || !Strings.isNullOrEmpty(expectedTargetRevision)) {
      BranchRevisionResolver.RevisionResult revisions = branchRevisionResolver.getRevisions(namespace, name, pullRequestId);
      String currentSourceRevision = revisions.getSourceRevision();
      String currentTargetRevision = revisions.getTargetRevision();
      if (revisionsMatchIfSpecified(expectedSourceRevision, expectedTargetRevision, currentSourceRevision, currentTargetRevision)) {
        throw new ConcurrentModificationException(PullRequest.class, pullRequestId);
      }
    }
  }
  private static boolean revisionsMatchIfSpecified(String expectedSourceRevision, String expectedTargetRevision, String currentSourceRevision, String currentTargetRevision) {
    return (!Strings.isNullOrEmpty(expectedSourceRevision) && !currentSourceRevision.equals(expectedSourceRevision)) ||
      (!Strings.isNullOrEmpty(expectedTargetRevision) && !currentTargetRevision.equals(expectedTargetRevision));
  }
}
