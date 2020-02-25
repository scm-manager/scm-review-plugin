package com.cloudogu.scm.review;

import sonia.scm.web.VndMediaType;

public class PullRequestMediaType {
  public static final String PULL_REQUEST = VndMediaType.PREFIX + "pullRequest" + VndMediaType.SUFFIX;
  public static final String MERGE_COMMAND = VndMediaType.PREFIX + "mergeCommand" + VndMediaType.SUFFIX;
  public static final String MERGE_CHECK_RESULT = VndMediaType.PREFIX + "mergeCheckResult" + VndMediaType.SUFFIX;
  public static final String MERGE_CONFLICT_RESULT = VndMediaType.PREFIX + "mergeConflictsResult" + VndMediaType.SUFFIX;

  private PullRequestMediaType() {

  }
}
