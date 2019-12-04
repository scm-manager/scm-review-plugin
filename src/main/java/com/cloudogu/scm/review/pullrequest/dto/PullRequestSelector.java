package com.cloudogu.scm.review.pullrequest.dto;

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
      return pullRequest.getStatus().equals(PullRequestStatus.valueOf(name()));
    }
  },
  MINE {
    @Override
    public boolean test(PullRequest pullRequest) {
      return pullRequest.getAuthor().equals(SecurityUtils.getSubject().getPrincipal());
    }
  },
  REVIEWER {
    @Override
    public boolean test(PullRequest pullRequest) {
      return pullRequest.getStatus().equals(PullRequestStatus.OPEN) && pullRequest.getReviewer().keySet().contains(SecurityUtils.getSubject().getPrincipal());
    }
  },
  MERGED {
    @Override
    public boolean test(PullRequest pullRequest) {
      return pullRequest.getStatus().equals(PullRequestStatus.valueOf(name()));
    }
  },
  REJECTED {
    @Override
    public boolean test(PullRequest pullRequest) {
      return pullRequest.getStatus().equals(PullRequestStatus.valueOf(name()));
    }
  }
}
