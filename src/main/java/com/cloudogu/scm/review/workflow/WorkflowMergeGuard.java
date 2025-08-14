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

package com.cloudogu.scm.review.workflow;

import com.cloudogu.scm.review.pullrequest.service.MergeGuard;
import com.cloudogu.scm.review.pullrequest.service.MergeObstacle;
import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import jakarta.inject.Inject;
import sonia.scm.plugin.Extension;
import sonia.scm.repository.Repository;

import java.util.Collection;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;

@Extension
public class WorkflowMergeGuard implements MergeGuard {

  private final Engine engine;

  @Inject
  public WorkflowMergeGuard(Engine engine) {
    this.engine = engine;
  }

  @Override
  public Collection<MergeObstacle> getObstacles(Repository repository, PullRequest pullRequest) {
    Results validation = engine.validate(repository, pullRequest);
    if (!validation.isValid()) {
      return validation
        .getRuleResults()
        .stream()
        .filter(Result::isFailed)
        .map(WorkflowMergeObstacle::new)
        .collect(Collectors.toList());
    }
    return emptyList();
  }

  private static class WorkflowMergeObstacle implements MergeObstacle {
    private final String ruleMessageKey;

    public WorkflowMergeObstacle(Result result) {
      this.ruleMessageKey = result.getRule().getSimpleName();
    }

    @Override
    public String getMessage() {
      return "rule failed: " + ruleMessageKey;
    }

    @Override
    public String getKey() {
      return "workflow.rule." + ruleMessageKey + ".obstacle";
    }

    @Override
    public boolean isOverrideable() {
      return true;
    }
  }
}
