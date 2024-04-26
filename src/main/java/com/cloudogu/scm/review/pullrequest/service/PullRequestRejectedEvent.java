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
package com.cloudogu.scm.review.pullrequest.service;

import lombok.Getter;
import sonia.scm.event.Event;
import sonia.scm.repository.Repository;

@Event
@Getter
public class PullRequestRejectedEvent extends BasicPullRequestEvent {

  private final RejectionCause cause;
  private final String message;
  private final PullRequestStatus previousStatus;

  public PullRequestRejectedEvent(Repository repository, PullRequest pullRequest, RejectionCause cause) {
    this(repository, pullRequest, cause, null, null);
  }

  public PullRequestRejectedEvent(Repository repository, PullRequest pullRequest, RejectionCause cause, String message) {
    this(repository, pullRequest, cause, message, null);
  }

  public PullRequestRejectedEvent(Repository repository, PullRequest pullRequest, RejectionCause cause, String message, PullRequestStatus previousStatus) {
    super(repository, pullRequest);
    this.cause = cause;
    this.message = message;
    this.previousStatus = previousStatus;
  }

  public enum RejectionCause {
    SOURCE_BRANCH_DELETED,
    TARGET_BRANCH_DELETED,
    REJECTED_BY_USER
  }
}
