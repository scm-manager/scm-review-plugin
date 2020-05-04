/*
 * MIT License
 *
 * Copyright (c) 2020-present Cloudogu GmbH and Contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.cloudogu.scm.review.workflow;

import com.cloudogu.scm.review.pullrequest.service.MergeGuard;
import com.cloudogu.scm.review.pullrequest.service.MergeObstacle;
import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import sonia.scm.plugin.Extension;
import sonia.scm.repository.Repository;

import javax.inject.Inject;
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
    if (!engine.validate(repository, pullRequest).isValid()) {
      return engine.validate(repository, pullRequest)
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
    private final String errorCode;

    public WorkflowMergeObstacle(Result result) {
      this.ruleMessageKey = result.getRule().getSimpleName();
      if (result.getContext() instanceof ResultContextWithTranslationCode) {
        this.errorCode = ((ResultContextWithTranslationCode) result.getContext()).getTranslationCode();
      } else {
        this.errorCode = null;
      }
    }

    @Override
    public String getMessage() {
      return "rule failed: " + ruleMessageKey;
    }

    @Override
    public String getKey() {
      return ruleMessageKey;
    }

    @Override
    public boolean isOverrideable() {
      return true;
    }
  }
}
