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

package com.cloudogu.scm.review.pullrequest;

import lombok.AllArgsConstructor;
import lombok.Getter;

public final class PullRequestUtil {
  private PullRequestUtil() {}

  public static PullRequestTitleAndDescription determineTitleAndDescription(String changesetDescription) {
    String[] split = changesetDescription.split("\\R", 2);
    String title = split[0];
    String description = "";

    if (split.length > 1) {
      description = split[1];
    }

    if (title.length() > 80) {
      description = "..." + title.substring(69).trim() + System.lineSeparator() + description;
      title = title.substring(0, 69).trim() + "...";
    }

    return new PullRequestTitleAndDescription(title.trim(), description.trim());
  }

  @AllArgsConstructor
  @Getter
  public static class PullRequestTitleAndDescription {
    private final String title;
    private final String description;
  }
}
