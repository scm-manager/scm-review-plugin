package com.cloudogu.scm.review;

import java.time.Instant;

public class TestData {

  public static PullRequest createPullRequest() {
    return createPullRequest("id");
  }

  public static PullRequest createPullRequest(String id) {
    return createPullRequest(id, PullRequestStatus.OPEN);

  }
  public static PullRequest createPullRequest(String id, PullRequestStatus pullRequestStatus) {
    PullRequest pullRequest = new PullRequest("develop", "master", "Awesome PR");
    pullRequest.setId(id);
    pullRequest.setDescription("Hitchhiker's guide to the galaxy");
    pullRequest.setAuthor("dent");
    pullRequest.setCreationDate(Instant.MIN);
    pullRequest.setLastModified(Instant.MIN);
    pullRequest.setStatus(pullRequestStatus);
    return pullRequest;
  }

}
