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

package com.cloudogu.scm.review.pullrequest.service;

import sonia.scm.repository.api.MergePreventReason;

import java.util.Collection;

import static java.util.Collections.unmodifiableCollection;

public class MergeCheckResult {

  private final boolean hasConflicts;
  private final Collection<MergeObstacle> mergeObstacles;
  private final Collection<MergePreventReason> reasons;

  public MergeCheckResult(boolean hasConflicts, Collection<MergeObstacle> mergeObstacles, Collection<MergePreventReason> reasons) {
    this.hasConflicts = hasConflicts;
    this.mergeObstacles = mergeObstacles;
    this.reasons = reasons;
  }

  public boolean hasConflicts() {
    return hasConflicts;
  }

  public Collection<MergeObstacle> getMergeObstacles() {
    return unmodifiableCollection(mergeObstacles);
  }

  public Collection<MergePreventReason> getReasons() {
    return reasons;
  }
}
