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

package com.cloudogu.scm.review.pullrequest.landingpage;

import com.cloudogu.scm.landingpage.mytasks.MyTask;
import com.cloudogu.scm.review.comment.service.Comment;
import com.cloudogu.scm.review.comment.service.CommentType;
import com.cloudogu.scm.review.pullrequest.dto.PullRequestDto;
import com.cloudogu.scm.review.pullrequest.dto.PullRequestMapper;
import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import com.cloudogu.scm.review.pullrequest.service.PullRequestStatus;
import com.google.common.collect.ImmutableMap;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.repository.Repository;

import static com.cloudogu.scm.review.pullrequest.service.PullRequestStatus.OPEN;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MyOpenReviewsTest {

  static final Repository PR_REPOSITORY = new Repository("1", "git", "space", "X");

  static final PullRequest OPEN_PR_WITH_USER_AS_REVIEWER_NOT_REVIEWED = createPullRequest("open_dent_not_reviewed", OPEN, "trillian", false);
  static final PullRequest OPEN_PR_WITH_USER_AS_REVIEWER_REVIEWED = createPullRequest("open_dent_reviewed", OPEN, "trillian", true);
  static final PullRequest OPEN_PR_WITHOUT_USER_AS_REVIEWER = createPullRequest("open_dent", OPEN, "ziltoid", false);

  @Mock
  OpenPullRequestProvider pullRequestProvider;
  @Mock
  PullRequestMapper mapper;

  @InjectMocks
  MyOpenReviews myOpenReviews;

  @Mock
  Subject subject;

  @BeforeEach
  void mockUser() {
    ThreadContext.bind(subject);
    when(subject.getPrincipal()).thenReturn("trillian");
  }

  @AfterEach
  void unbindUser() {
    ThreadContext.unbindSubject();
  }

  @BeforeEach
  void mockMapper() {
    when(mapper.map(any(), any())).thenAnswer(invocationOnMock -> {
      PullRequestDto dto = new PullRequestDto();
      dto.setId(invocationOnMock.getArgument(0, PullRequest.class).getId());
      return dto;
    });
  }

  @Test
  void shouldFindMyPullRequests() {
    doAnswer(invocationOnMock -> {
        invocationOnMock.getArgument(0, OpenPullRequestProvider.RepositoryAndPullRequestConsumer.class)
          .accept(PR_REPOSITORY, asList(OPEN_PR_WITH_USER_AS_REVIEWER_NOT_REVIEWED, OPEN_PR_WITH_USER_AS_REVIEWER_REVIEWED, OPEN_PR_WITHOUT_USER_AS_REVIEWER).stream());
        return null;
      }
    ).when(pullRequestProvider).findOpenPullRequests(any());
    Iterable<MyTask> data = myOpenReviews.getTasks();
    assertThat(data).extracting("pullRequest").extracting("id").containsExactly("open_dent_not_reviewed");
  }

  private static PullRequest createPullRequest(String id, PullRequestStatus status, String reviewer, boolean reviewStatus) {
    PullRequest existingPullRequest = new PullRequest();
    existingPullRequest.setId(id);
    existingPullRequest.setTitle("title " + id);
    existingPullRequest.setSource("source");
    existingPullRequest.setTarget("target");
    existingPullRequest.setStatus(status);
    existingPullRequest.setAuthor("dent");
    existingPullRequest.setReviewer(ImmutableMap.of("someone", false, reviewer, reviewStatus));
    return existingPullRequest;
  }

  private static Comment createComment(CommentType type) {
    Comment comment = new Comment();
    comment.setType(type);
    return comment;
  }
}
