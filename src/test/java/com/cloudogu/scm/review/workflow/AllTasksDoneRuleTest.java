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

import com.cloudogu.scm.review.TestData;
import com.cloudogu.scm.review.comment.service.Comment;
import com.cloudogu.scm.review.comment.service.CommentService;
import com.cloudogu.scm.review.comment.service.CommentType;
import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryTestData;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AllTasksDoneRuleTest {

  private final Repository repository = RepositoryTestData.create42Puzzle();
  private final PullRequest pullRequest = TestData.createPullRequest();

  @Mock
  private CommentService commentService;

  @InjectMocks
  private AllTasksDoneRule rule;

  @Test
  void shouldReturnSuccess() {
    setUpCommentService(CommentType.COMMENT, CommentType.TASK_DONE);
    Result result = rule.validate(new Context(repository, pullRequest, null));
    assertThat(result.isFailed()).isFalse();
  }

  @Test
  void shouldReturnFailed() {
    setUpCommentService(CommentType.COMMENT, CommentType.TASK_TODO, CommentType.TASK_DONE);
    Result result = rule.validate(new Context(repository, pullRequest, null));
    assertThat(result.isFailed()).isTrue();
    assertThat(result.getContext()).isInstanceOf(AllTasksDoneRule.ResultContext.class);
    AllTasksDoneRule.ResultContext errorContext = (AllTasksDoneRule.ResultContext) result.getContext();
    assertThat(errorContext.getCount()).isEqualTo(1);
  }

  private void setUpCommentService(CommentType... types) {
    List<Comment> comments = Arrays.stream(types).map(type -> {
      Comment c = Comment.createSystemComment("test");
      c.setType(type);
      return c;
    }).collect(Collectors.toList());
    when(commentService.getAll(repository.getNamespace(), repository.getName(), pullRequest.getId()))
      .thenReturn(comments);
  }

}
