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
