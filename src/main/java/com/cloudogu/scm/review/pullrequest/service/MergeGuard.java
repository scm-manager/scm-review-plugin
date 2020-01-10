package com.cloudogu.scm.review.pullrequest.service;

import sonia.scm.plugin.ExtensionPoint;

import java.util.Collection;

@ExtensionPoint
public interface MergeGuard {
  Collection<MergeObstacle> getObstacles(PullRequest pullRequest);
}
