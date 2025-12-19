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

import com.cloudogu.scm.review.pullrequest.dto.DisplayedUserDto;
import com.cloudogu.scm.review.pullrequest.dto.ReviewerDto;
import com.cloudogu.scm.review.pullrequest.dto.TasksDto;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
@SuppressWarnings("squid:S2160")
public class PullRequestDetailMcp extends PullRequestOverviewMcp {
  private DisplayedUserDto author;
  private DisplayedUserDto reviser;
  private String description;
  private Instant lastModified;
  private Set<ReviewerDto> reviewer = new HashSet<>();
  private Set<String> labels = new HashSet<>();
  private TasksDto tasks;
  private String sourceRevision;
  private String targetRevision;
  private boolean emergencyMerged;
  private List<String> ignoredMergeObstacles;
  private List<String> initialTasks = new ArrayList<>();
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  private Collection<String> mergeObstacles;
}
