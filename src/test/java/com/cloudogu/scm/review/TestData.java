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

package com.cloudogu.scm.review;


import com.cloudogu.scm.review.comment.service.Comment;
import com.cloudogu.scm.review.comment.service.Location;
import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import com.cloudogu.scm.review.pullrequest.service.PullRequestStatus;

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

  public static Comment createComment() {
    return Comment.createComment("1", "this is my comment", "author", new Location());
  }

  public static Comment createSystemComment() {
    return Comment.createSystemComment("statusToOpen");
  }
}
