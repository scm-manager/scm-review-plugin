package com.cloudogu.scm.review.pullrequest.api;

import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import com.cloudogu.scm.review.pullrequest.service.PullRequestStatus;
import org.apache.shiro.SecurityUtils;

import java.util.function.Predicate;

public enum PullRequestSelector implements Predicate<PullRequest> {

  ALL {
    @Override
    public boolean test(PullRequest pullRequest) {
      return true;
    }
  },
  OPEN {
    @Override
    public boolean test(PullRequest pullRequest) {
      return PullRequestStatus.OPEN.equals(pullRequest.getStatus());
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
      return PullRequestStatus.OPEN.equals(pullRequest.getStatus())
        && pullRequest.getReviewer().containsKey(currentUser());
    }
  },
  MERGED {
    @Override
    public boolean test(PullRequest pullRequest) {
      return PullRequestStatus.MERGED.equals(pullRequest.getStatus());
    }
  },
  REJECTED {
    @Override
    public boolean test(PullRequest pullRequest) {
      return PullRequestStatus.REJECTED.equals(pullRequest.getStatus());
    }
  };

  private static String currentUser() {
    return SecurityUtils.getSubject().getPrincipal().toString();
  }
}
