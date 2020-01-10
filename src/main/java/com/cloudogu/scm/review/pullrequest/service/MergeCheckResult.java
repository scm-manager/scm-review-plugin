package com.cloudogu.scm.review.pullrequest.service;

import java.util.Collection;

import static java.util.Collections.unmodifiableCollection;

public class MergeCheckResult {

  private final boolean hasConflicts;

  private final Collection<MergeObstacle> mergeObstacles;

  public MergeCheckResult(boolean hasConflicts, Collection<MergeObstacle> mergeObstacles) {
    this.hasConflicts = hasConflicts;
    this.mergeObstacles = mergeObstacles;
  }

  public boolean hasConflicts() {
    return hasConflicts;
  }

  public Collection<MergeObstacle> getMergeObstacles() {
    return unmodifiableCollection(mergeObstacles);
  }
}
