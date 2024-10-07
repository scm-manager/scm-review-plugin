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

package com.cloudogu.scm.review.pullrequest.api;

import com.cloudogu.scm.review.pullrequest.service.PullRequest;

import java.time.Instant;
import java.util.Comparator;

import static java.util.Comparator.comparing;

public enum PullRequestSortSelector {
  ID_ASC(comparing(PullRequest::getId)),
  ID_DESC(comparing(PullRequest::getId).reversed()),
  STATUS_ASC(comparing(PullRequest::getStatus)),
  STATUS_DESC(comparing(PullRequest::getStatus).reversed()),
  LAST_MOD_ASC(comparing(PullRequestSortSelector::computeLastModifiedForSort)),
  LAST_MOD_DESC(comparing(PullRequestSortSelector::computeLastModifiedForSort).reversed());

  private final Comparator<PullRequest> comparator;

  PullRequestSortSelector(Comparator<PullRequest> comparator) {
    this.comparator = comparator;
  }

  public Comparator<PullRequest> compare() {
    return this.comparator;
  }

  private static Instant computeLastModifiedForSort(PullRequest pullRequest) {
    if (pullRequest.getLastModified() == null) {
      return pullRequest.getCreationDate();
    } else {
      return pullRequest.getLastModified();
    }
  }
}
