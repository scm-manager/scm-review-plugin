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

package com.cloudogu.scm.review;

import sonia.scm.web.VndMediaType;

public class PullRequestMediaType {
  public static final String PULL_REQUEST = VndMediaType.PREFIX + "pullRequest" + VndMediaType.SUFFIX;
  public static final String PULL_REQUEST_CHECK_RESULT = VndMediaType.PREFIX + "pullRequestCheckResult" + VndMediaType.SUFFIX;
  public static final String PULL_REQUEST_COLLECTION = VndMediaType.PREFIX + "pullRequestCollection" + VndMediaType.SUFFIX;
  public static final String MERGE_COMMAND = VndMediaType.PREFIX + "mergeCommand" + VndMediaType.SUFFIX;
  public static final String MERGE_CHECK_RESULT = VndMediaType.PREFIX + "mergeCheckResult" + VndMediaType.SUFFIX;
  public static final String MERGE_CONFLICT_RESULT = VndMediaType.PREFIX + "mergeConflictsResult" + VndMediaType.SUFFIX;
  public static final String MERGE_STRATEGY_INFO = VndMediaType.PREFIX + "mergeStrategyInfo" + VndMediaType.SUFFIX;

  private PullRequestMediaType() {

  }
}
