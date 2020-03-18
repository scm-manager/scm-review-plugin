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
