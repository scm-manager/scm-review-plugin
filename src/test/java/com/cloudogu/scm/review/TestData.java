/**
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
}
