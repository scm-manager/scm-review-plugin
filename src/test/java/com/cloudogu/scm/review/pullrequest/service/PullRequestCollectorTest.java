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
package com.cloudogu.scm.review.pullrequest.service;

import com.cloudogu.scm.review.TestData;
import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryTestData;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PullRequestCollectorTest {

  @Mock
  private PullRequestService pullRequestService;

  @InjectMocks
  private PullRequestCollector collector;

  @Test
  void shouldReturnPullRequestWithAffectedSourceBranch() {
    PullRequest pullRequest = TestData.createPullRequest("42", PullRequestStatus.OPEN);
    List<PullRequest> requests = ImmutableList.of(pullRequest);

    Repository repository = RepositoryTestData.createHeartOfGold("git");
    when(pullRequestService.getAll(repository.getNamespace(), repository.getName())).thenReturn(requests);

    List<PullRequest> prs = collector.collectAffectedPullRequests(repository, ImmutableList.of("develop"));
    assertThat(prs).containsOnly(pullRequest);
  }

  @Test
  void shouldReturnPullRequestWithAffectedTargetBranch() {
    PullRequest pullRequest = TestData.createPullRequest("42", PullRequestStatus.OPEN);
    List<PullRequest> requests = ImmutableList.of(pullRequest);

    Repository repository = RepositoryTestData.createHeartOfGold("git");
    when(pullRequestService.getAll(repository.getNamespace(), repository.getName())).thenReturn(requests);

    List<PullRequest> prs = collector.collectAffectedPullRequests(repository, ImmutableList.of("master"));
    assertThat(prs).containsOnly(pullRequest);
  }

  @Test
  void shouldReturnOnlyOpenPullRequest() {
    PullRequest one = TestData.createPullRequest("21", PullRequestStatus.OPEN);
    PullRequest two = TestData.createPullRequest("42", PullRequestStatus.REJECTED);
    PullRequest three = TestData.createPullRequest("64", PullRequestStatus.MERGED);
    List<PullRequest> requests = ImmutableList.of(one, two, three);

    Repository repository = RepositoryTestData.createHeartOfGold("git");
    when(pullRequestService.getAll(repository.getNamespace(), repository.getName())).thenReturn(requests);

    List<PullRequest> prs = collector.collectAffectedPullRequests(repository, ImmutableList.of("master", "develop"));
    assertThat(prs).containsOnly(one);
  }
}
