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

import com.cloudogu.scm.review.pullrequest.dto.PullRequestDto;

import java.util.Comparator;

import static java.util.Comparator.comparing;

public enum PullRequestSortSelector {
  ID_ASC(comparing(PullRequestDto::getId)),
  ID_DESC(comparing(PullRequestDto::getId).reversed()),
  STATUS_ASC(comparing(PullRequestDto::getStatus)),
  STATUS_DESC(comparing(PullRequestDto::getStatus).reversed()),
  LAST_MOD_ASC(comparing(PullRequestDto::getLastModifiedDateForSort)),
  LAST_MOD_DESC(comparing(PullRequestDto::getLastModifiedDateForSort).reversed());

  private final Comparator<PullRequestDto> comparator;

  PullRequestSortSelector(Comparator<PullRequestDto> comparator) {
    this.comparator = comparator;
  }

  public Comparator<PullRequestDto> compare() {
    return this.comparator;
  }
}
