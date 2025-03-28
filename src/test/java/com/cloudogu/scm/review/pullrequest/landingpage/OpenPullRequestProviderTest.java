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

package com.cloudogu.scm.review.pullrequest.landingpage;

import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import com.cloudogu.scm.review.pullrequest.service.PullRequestService;
import com.cloudogu.scm.review.pullrequest.service.PullRequestStatus;
import junit.framework.AssertionFailedError;
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
import sonia.scm.repository.RepositoryManager;
import sonia.scm.repository.api.Command;
import sonia.scm.repository.api.RepositoryService;
import sonia.scm.repository.api.RepositoryServiceFactory;

import static com.cloudogu.scm.review.pullrequest.service.PullRequestStatus.MERGED;
import static com.cloudogu.scm.review.pullrequest.service.PullRequestStatus.OPEN;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalMatchers.not;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OpenPullRequestProviderTest {
  static final Repository PR_REPOSITORY = new Repository("1", "git", "space", "X");
  static final Repository NO_PR_REPOSITORY = new Repository("1", "svn", "us", "nasa");

  static final PullRequest OPEN_PR = createPullRequest("open", OPEN);
  static final PullRequest CLOSED_PR = createPullRequest("closed", MERGED);

  @Mock
  RepositoryServiceFactory serviceFactory;
  @Mock
  PullRequestService pullRequestService;
  @Mock
  RepositoryManager repositoryManager;
  @Mock
  Subject subject;

  @InjectMocks
  OpenPullRequestProvider provider;

  boolean closureCalled = false;

  @BeforeEach
  void bindSubject() {
    ThreadContext.bind(subject);
    when(subject.isPermitted("repository:readPullRequest:1")).thenReturn(true);
  }

  @AfterEach
  void tearDownSubject() {
    ThreadContext.unbindSubject();
  }

  @BeforeEach
  void mockPrSupport() {
    RepositoryService repositoryServiceWithPr = mockedRepositoryService(true);
    when(serviceFactory.create(PR_REPOSITORY)).thenReturn(repositoryServiceWithPr);
    RepositoryService repositoryServiceWithoutPr = mockedRepositoryService(false);
    when(serviceFactory.create(NO_PR_REPOSITORY)).thenReturn(repositoryServiceWithoutPr);
  }

  @Test
  void shouldFindOpenPullRequests() {
    mockPullRequestsForRepositories();
    provider.findOpenPullRequests((repository, pullRequestStream) -> {
      assertThat(repository).isEqualTo(PR_REPOSITORY);
      assertThat(pullRequestStream).containsExactly(OPEN_PR);
      closureCalled = true;
    });
    assertThat(closureCalled).isTrue();
  }

  private RepositoryService mockedRepositoryService(boolean prSupported) {
    RepositoryService repositoryService = mock(RepositoryService.class);
    when(repositoryService.isSupported(Command.MERGE)).thenReturn(prSupported);
    return repositoryService;
  }

  private void mockPullRequestsForRepositories() {
    when(repositoryManager.getAll()).thenReturn(asList(PR_REPOSITORY, NO_PR_REPOSITORY));
    lenient().when(pullRequestService.getAll(not(eq("space")), not(eq("X")))).thenThrow(new AssertionFailedError("not expected call"));
    when(pullRequestService.getAll("space", "X")).thenReturn(asList(OPEN_PR, CLOSED_PR));
  }

  private static PullRequest createPullRequest(String id, PullRequestStatus status) {
    PullRequest existingPullRequest = new PullRequest();
    existingPullRequest.setId(id);
    existingPullRequest.setTitle("title " + id);
    existingPullRequest.setSource("source");
    existingPullRequest.setTarget("target");
    existingPullRequest.setStatus(status);
    return existingPullRequest;
  }
}
