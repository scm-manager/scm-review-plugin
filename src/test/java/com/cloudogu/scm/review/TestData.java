package com.cloudogu.scm.review;

import com.cloudogu.scm.review.service.PullRequest;
import com.cloudogu.scm.review.service.PullRequestStatus;

import java.time.Instant;

public class TestData {

  public static PullRequest createPullRequest() {
    return createPullRequest("id");
  }

  public static PullRequest createPullRequest(String id) {
    return createPullRequest(id, PullRequestStatus.OPEN);

  }
  public static PullRequest createPullRequest(String id, PullRequestStatus pullRequestStatus) {
    PullRequest pullRequest = new PullRequest();
    pullRequest.setSource("develop");
    pullRequest.setTarget("master");
    pullRequest.setTitle("PR");
    pullRequest.setId(id);
    pullRequest.setDescription("Hitchhiker's guide to the galaxy");
    pullRequest.setAuthor("dent");
    pullRequest.setCreationDate(Instant.MIN);
    pullRequest.setLastModified(Instant.MIN);
    pullRequest.setStatus(pullRequestStatus);
    return pullRequest;
  }

}
