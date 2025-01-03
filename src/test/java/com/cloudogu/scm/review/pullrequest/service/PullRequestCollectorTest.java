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
