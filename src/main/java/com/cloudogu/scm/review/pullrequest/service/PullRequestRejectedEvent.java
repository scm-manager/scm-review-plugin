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
