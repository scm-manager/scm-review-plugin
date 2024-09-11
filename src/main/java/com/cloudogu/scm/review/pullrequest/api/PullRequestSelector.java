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

package com.cloudogu.scm.review.pullrequest.api;

import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import org.apache.shiro.SecurityUtils;

import java.util.function.Predicate;

public enum PullRequestSelector implements Predicate<PullRequest> {

  ALL {
    @Override
    public boolean test(PullRequest pullRequest) {
      return true;
    }
  },
  IN_PROGRESS {
    @Override
    public boolean test(PullRequest pullRequest) {
      return pullRequest.isInProgress();
    }
  },
  /**
   * @deprecated Replaced with {@link #IN_PROGRESS}
   */
  @Deprecated
  OPEN {
    @Override
    public boolean test(PullRequest pullRequest) {
      return IN_PROGRESS.test(pullRequest);
    }
  },
  MINE {
    @Override
    public boolean test(PullRequest pullRequest) {
      return pullRequest.getAuthor().equals(currentUser());
    }
  },
  REVIEWER {
    @Override
    public boolean test(PullRequest pullRequest) {
      return pullRequest.isOpen()
        && pullRequest.getReviewer().containsKey(currentUser());
    }
  },
  MERGED {
    @Override
    public boolean test(PullRequest pullRequest) {
      return pullRequest.isMerged();
    }
  },
  REJECTED {
    @Override
    public boolean test(PullRequest pullRequest) {
      return pullRequest.isRejected();
    }
  };

  private static String currentUser() {
    return SecurityUtils.getSubject().getPrincipal().toString();
  }
}
