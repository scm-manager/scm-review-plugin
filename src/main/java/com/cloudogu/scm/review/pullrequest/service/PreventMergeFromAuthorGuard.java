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

package com.cloudogu.scm.review.pullrequest.service;

import com.cloudogu.scm.review.CurrentUserResolver;
import com.cloudogu.scm.review.config.service.ConfigService;
import sonia.scm.plugin.Extension;
import sonia.scm.repository.Repository;

import javax.inject.Inject;
import java.util.Collection;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;

@Extension
public class PreventMergeFromAuthorGuard implements MergeGuard {

  private final ConfigService configService;

  @Inject
  public PreventMergeFromAuthorGuard(ConfigService configService) {
    this.configService = configService;
  }

  @Override
  public Collection<MergeObstacle> getObstacles(Repository repository, PullRequest pullRequest) {
    if (configService.isPreventMergeFromAuthor(repository) && currentUserIsAuthorOfPullRequest(pullRequest)) {
      return singleton(new MergeObstacle() {
        @Override
        public String getMessage() {
          return "Merge from pull request author is not allowed";
        }

        @Override
        public String getKey() {
          return "scm-review-plugin.pullRequest.guard.mergeFromAuthorNotAllowed.obstacle";
        }

        @Override
        public boolean isOverrideable() {
          return true;
        }
      });
    } else {
      return emptySet();
    }
  }

  private boolean currentUserIsAuthorOfPullRequest(PullRequest pullRequest) {
    return pullRequest.getAuthor().equals(CurrentUserResolver.getCurrentUser().getId());
  }
}
