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
import com.cloudogu.scm.review.pullrequest.service.PullRequestStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.Value;

import java.time.Instant;

@Data
@SuppressWarnings("squid:S2160")
public class PullRequestOverviewMcp {
  private String id;
  private Repository repository;
  private PullRequestStatus status;
  private DisplayedUserDto author;
  private String title;
  private Instant creationDate;
  private Instant closeDate;
  @NotBlank
  private String source;
  @NotBlank
  private String target;
  @NotBlank

  @Value
  public static class Repository {
    String namespace;
    String name;
  }
}
