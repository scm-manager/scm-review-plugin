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

package com.cloudogu.scm.review.workflow;

import com.cloudogu.scm.review.comment.service.Comment;
import com.cloudogu.scm.review.comment.service.CommentService;
import com.cloudogu.scm.review.comment.service.CommentType;
import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import sonia.scm.plugin.Extension;
import sonia.scm.repository.Repository;

import javax.inject.Inject;
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

  private static class ResultContext {
    private final int count;

    private ResultContext(int count) {
      this.count = count;
    }

    public int getCount() {
      return count;
    }
  }
}
