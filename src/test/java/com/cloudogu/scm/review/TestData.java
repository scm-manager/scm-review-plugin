package com.cloudogu.scm.review;

import java.time.Instant;

public class TestData {

  public static PullRequest createPullRequest() {
    PullRequest pullRequest = new PullRequest("develop", "master", "Awesome PR");
    pullRequest.setDescription("Hitchhiker's guide to the galaxy");
    pullRequest.setAuthor("dent");
    pullRequest.setCreationDate(Instant.MIN);
    return pullRequest;
  }

  public static PullRequest createPullRequest(String id) {
    PullRequest pullRequest = new PullRequest("develop", "master", "Awesome PR");
    pullRequest.setId(id);
    pullRequest.setDescription("Hitchhiker's guide to the galaxy");
    pullRequest.setAuthor("dent");
    pullRequest.setCreationDate(Instant.MIN);
    return pullRequest;
  }

}
