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

package com.cloudogu.scm.review.pullrequest.dto;

import de.otto.edison.hal.Embedded;
import de.otto.edison.hal.HalRepresentation;
import de.otto.edison.hal.Links;
import lombok.Getter;

import java.util.List;
import java.util.Set;

@Getter
@SuppressWarnings("java:S2160")
public class PullRequestTemplateDto extends HalRepresentation {
  private final String title;
  private final String description;
  private final Set<DisplayedUserDto> defaultReviewers;
  private final Set<String> availableLabels;
  private final List<String> defaultTasks;
  private final boolean shouldDeleteSourceBranch;

  public PullRequestTemplateDto(Links links, Embedded embedded, String title, String description, Set<DisplayedUserDto> defaultReviewers, Set<String> availableLabels, List<String> defaultTasks, boolean shouldDeleteSourceBranch) {
    super(links, embedded);
    this.title = title;
    this.description = description;
    this.defaultReviewers = defaultReviewers;
    this.availableLabels = availableLabels;
    this.defaultTasks = defaultTasks;
    this.shouldDeleteSourceBranch = shouldDeleteSourceBranch;
  }
}
