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

import de.otto.edison.hal.HalRepresentation;
import de.otto.edison.hal.Links;
import lombok.Getter;

@Getter
@SuppressWarnings("squid:S2160") // there is no proper semantic for equals on this dto
public class PullRequestCheckResultDto extends HalRepresentation {

  private final PullRequestCheckStatus status;

  private PullRequestCheckResultDto(Links links, PullRequestCheckStatus status) {
    super(links);
    this.status = status;
  }

  interface Creator {
    PullRequestCheckResultDto create(Links links);
  }

  public enum PullRequestCheckStatus implements Creator {
    PR_VALID, PR_ALREADY_EXISTS, BRANCHES_NOT_DIFFER, SAME_BRANCHES;

    @Override
    public PullRequestCheckResultDto create(Links links) {
      return new PullRequestCheckResultDto(links, this);
    }
  }
}
