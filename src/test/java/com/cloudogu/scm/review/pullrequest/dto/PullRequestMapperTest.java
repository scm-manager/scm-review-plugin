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

package com.cloudogu.scm.review.pullrequest.dto;

import com.cloudogu.scm.review.TestData;
import com.cloudogu.scm.review.comment.service.CommentService;
import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import com.cloudogu.scm.review.pullrequest.service.PullRequestService;
import com.google.common.collect.ImmutableSet;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.repository.Repository;
import sonia.scm.repository.api.Command;
import sonia.scm.repository.api.MergeStrategy;
import sonia.scm.repository.api.RepositoryService;
import sonia.scm.repository.api.RepositoryServiceFactory;
import sonia.scm.user.UserDisplayManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PullRequestMapperTest {

  private static final Repository REPOSITORY = new Repository("id-1", "git", "space", "x");

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private Subject subject;

  @Mock
  private UserDisplayManager userDisplayManager;
  @Mock
  private PullRequestService pullRequestService;
  @Mock
  private CommentService commentService;
  @Mock
  private RepositoryServiceFactory serviceFactory;
  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private RepositoryService service;

  @InjectMocks
  private PullRequestMapperImpl mapper;

  @BeforeEach
  void initSubject() {
    ThreadContext.bind(subject);
  }

  @AfterEach
  void tearDownSubject() {
    ThreadContext.unbindSubject();
  }

  @Test
  void shouldMapPullRequestWithMergeLink() {
    when(subject.getPrincipals().getPrimaryPrincipal()).thenReturn("dent");
    when(serviceFactory.create(REPOSITORY)).thenReturn(service);
    when(service.isSupported(Command.MERGE)).thenReturn(true);
    when(service.getMergeCommand().getSupportedMergeStrategies()).thenReturn(ImmutableSet.of(MergeStrategy.MERGE_COMMIT));
    when(subject.isPermitted("repository:commentPullRequest:id-1")).thenReturn(true);
    when(subject.isPermitted("repository:push:id-1")).thenReturn(true);
    when(subject.isPermitted("repository:mergePullRequest:id-1")).thenReturn(true);

    PullRequest pullRequest = TestData.createPullRequest();
    PullRequestDto dto = mapper.map(pullRequest, REPOSITORY);

    assertThat(dto.getLinks().isEmpty()).isFalse();
    assertThat(dto.getLinks().getLinkBy("merge").isPresent()).isTrue();
    assertThat(dto.getLinks().getLinkBy("defaultCommitMessage").isPresent()).isTrue();
  }

  @Test
  void shouldMapPullRequestWithEmergencyMergeLink() {
    when(subject.getPrincipals().getPrimaryPrincipal()).thenReturn("dent");
    when(serviceFactory.create(REPOSITORY)).thenReturn(service);
    when(service.isSupported(Command.MERGE)).thenReturn(true);
    when(service.getMergeCommand().getSupportedMergeStrategies()).thenReturn(ImmutableSet.of(MergeStrategy.MERGE_COMMIT));
    when(subject.isPermitted("repository:commentPullRequest:id-1")).thenReturn(true);
    when(subject.isPermitted("repository:push:id-1")).thenReturn(true);
    when(subject.isPermitted("repository:mergePullRequest:id-1")).thenReturn(true);
    when(subject.isPermitted("repository:performEmergencyMerge:id-1")).thenReturn(true);

    PullRequest pullRequest = TestData.createPullRequest();
    PullRequestDto dto = mapper.map(pullRequest, REPOSITORY);

    assertThat(dto.getLinks().isEmpty()).isFalse();
    assertThat(dto.getLinks().getLinkBy("emergencyMerge").isPresent()).isTrue();
  }


}
