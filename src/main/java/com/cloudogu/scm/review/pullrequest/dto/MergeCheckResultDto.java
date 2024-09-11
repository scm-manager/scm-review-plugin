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

import com.cloudogu.scm.review.pullrequest.service.MergeObstacle;
import de.otto.edison.hal.HalRepresentation;
import de.otto.edison.hal.Links;
import lombok.Getter;
import sonia.scm.repository.api.MergePreventReason;

import java.util.Collection;

@Getter
public class MergeCheckResultDto extends HalRepresentation {

  public MergeCheckResultDto(Links links, boolean hasConflicts, Collection<MergeObstacle> mergeObstacles, Collection<MergePreventReason> reasons) {
    super(links);
    this.hasConflicts = hasConflicts;
    this.mergeObstacles = mergeObstacles;
    this.mergePreventReasons = reasons;
  }

  private final boolean hasConflicts;
  private final Collection<MergeObstacle> mergeObstacles;
  private final Collection<MergePreventReason> mergePreventReasons;
}
