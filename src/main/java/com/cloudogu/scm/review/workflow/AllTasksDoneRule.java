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

package com.cloudogu.scm.review.workflow;

import com.cloudogu.scm.review.comment.service.Comment;
import com.cloudogu.scm.review.comment.service.CommentService;
import com.cloudogu.scm.review.comment.service.CommentType;
import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import sonia.scm.plugin.Extension;
import sonia.scm.repository.Repository;

import jakarta.inject.Inject;
import java.util.List;

@Extension
public class AllTasksDoneRule implements Rule {

  private final CommentService commentService;

  @Inject
  public AllTasksDoneRule(CommentService commentService) {
    this.commentService = commentService;
  }

  @Override
  public Result validate(Context context) {
    int openTaskCount = countOpenTasks(context);
    if (openTaskCount > 0) {
      return failed(new ResultContext(openTaskCount));
    }
    return success();
  }

  private int countOpenTasks(Context context) {
    return (int) getComments(context).stream().filter(c -> c.getType() == CommentType.TASK_TODO).count();
  }

  private List<Comment> getComments(Context context) {
    Repository repository = context.getRepository();
    PullRequest pullRequest = context.getPullRequest();
    return commentService.getAll(repository.getNamespace(), repository.getName(), pullRequest.getId());
  }

  static class ResultContext {
    private final int count;

    private ResultContext(int count) {
      this.count = count;
    }

    public int getCount() {
      return count;
    }
  }
}
