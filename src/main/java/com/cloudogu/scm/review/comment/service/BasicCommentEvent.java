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

package com.cloudogu.scm.review.comment.service;

import com.cloudogu.scm.review.pullrequest.service.BasicPullRequestEvent;
import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import sonia.scm.HandlerEventType;
import sonia.scm.event.HandlerEvent;
import sonia.scm.repository.Repository;

public abstract class BasicCommentEvent<T extends BasicComment> extends BasicPullRequestEvent implements HandlerEvent<T> {

  private final T comment;
  private final T oldComment;
  private final HandlerEventType type;

  BasicCommentEvent(Repository repository, PullRequest pullRequest, T comment, T oldComment, HandlerEventType type) {
    super(repository, pullRequest);
    this.comment = comment;
    this.oldComment = oldComment;
    this.type = type;
  }

  @Override
  public T getItem() {
    return comment;
  }

  @Override
  public T getOldItem() {
    return oldComment;
  }

  @Override
  public HandlerEventType getEventType() {
    return type;
  }
}
