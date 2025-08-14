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

import sonia.scm.HandlerEventType;
import sonia.scm.event.Event;
import sonia.scm.event.HandlerEvent;
import sonia.scm.repository.Repository;

@Event
public class PullRequestEvent extends BasicPullRequestEvent implements HandlerEvent<PullRequest> {

  private final PullRequest oldPullRequest;
  private final HandlerEventType type;

  public PullRequestEvent(Repository repository, PullRequest pullRequest, PullRequest oldPullRequest, HandlerEventType type) {
    super(repository, pullRequest);
    this.oldPullRequest = oldPullRequest;
    this.type = type;
  }

  @Override
  public PullRequest getItem() {
    return pullRequest;
  }

  @Override
  public PullRequest getOldItem() {
    return oldPullRequest;
  }

  @Override
  public HandlerEventType getEventType() {
    return type;
  }
}
