package com.cloudogu.scm.review.pullrequest.service;

import sonia.scm.plugin.ExtensionPoint;
import sonia.scm.repository.Repository;

import java.util.Collection;

@ExtensionPoint
public interface MergeGuard {
  Collection<MergeObstacle> getObstacles(Repository repository, PullRequest pullRequest);
}
