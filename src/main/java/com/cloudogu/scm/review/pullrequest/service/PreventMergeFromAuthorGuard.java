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

import com.cloudogu.scm.review.CurrentUserResolver;
import com.cloudogu.scm.review.config.service.ConfigService;
import jakarta.inject.Inject;
import sonia.scm.plugin.Extension;
import sonia.scm.repository.Repository;

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
