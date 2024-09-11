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
import sonia.scm.user.User;

@Event
@Getter
public class PullRequestApprovalEvent extends BasicPullRequestEvent {

  private User approver;
  private boolean isNewApprover;
  private final ApprovalCause cause;

  public PullRequestApprovalEvent(Repository repository, PullRequest pullRequest, ApprovalCause cause) {
    super(repository, pullRequest);
    this.cause = cause;
  }

  public PullRequestApprovalEvent(Repository repository, PullRequest pullRequest, User approver, boolean isNewApprover, ApprovalCause cause) {
    this(repository, pullRequest, cause);
    this.approver = approver;
    this.isNewApprover = isNewApprover;
  }

  public enum ApprovalCause {
    APPROVED,
    APPROVAL_REMOVED
  }
}
