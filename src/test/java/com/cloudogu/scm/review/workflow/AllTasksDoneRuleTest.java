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
