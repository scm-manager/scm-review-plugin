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

import com.cloudogu.scm.review.pullrequest.service.PullRequestStatus;
import de.otto.edison.hal.Embedded;
import de.otto.edison.hal.HalRepresentation;
import de.otto.edison.hal.Links;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.Optional.ofNullable;

@Getter
@Setter
@NoArgsConstructor
@SuppressWarnings("squid:S2160")
public class PullRequestDto extends HalRepresentation {
  private String id;
  private DisplayedUserDto author;
  private DisplayedUserDto reviser;
  private Instant closeDate;
  @NotBlank
  private String source;
  @NotBlank
  private String target;
  @NotBlank
  private String title;
  private String description;
  private Instant creationDate;
  private Instant lastModified;
  private PullRequestStatus status;
  private Set<ReviewerDto> reviewer = new HashSet<>();
  private Set<String> labels = new HashSet<>();
  private TasksDto tasks;
  private String sourceRevision;
  private String targetRevision;
  private Collection<String> markedAsReviewed;
  private boolean emergencyMerged;
  private List<String> ignoredMergeObstacles;
  private List<String> initialTasks = new ArrayList<>();
  private boolean shouldDeleteSourceBranch;

  public PullRequestDto(Links links, Embedded embedded) {
    super(links, embedded);
  }

  public Instant getLastModifiedDateForSort() {
    return ofNullable(this.lastModified).orElse(this.creationDate);
  }
}
