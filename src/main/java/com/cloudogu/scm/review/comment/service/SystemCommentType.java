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

public enum SystemCommentType {

  MERGED("merged"),
  REJECTED("rejected"),
  SOURCE_DELETED("sourceDeleted"),
  TARGET_DELETED("targetDeleted"),
  TARGET_CHANGED("targetChanged"),
  STATUS_TO_DRAFT("statusToDraft"),
  STATUS_TO_OPEN("statusToOpen"),
  REOPENED("reopened");

  private final String key;

  SystemCommentType(String key) {
    this.key = key;
  }

  public String getKey() {
    return key;
  }
}
