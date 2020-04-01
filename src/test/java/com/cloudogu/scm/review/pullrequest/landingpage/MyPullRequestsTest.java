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

import com.cloudogu.scm.landingpage.mydata.MyData;
import com.cloudogu.scm.review.pullrequest.dto.PullRequestDto;
import com.cloudogu.scm.review.pullrequest.dto.PullRequestMapper;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MyPullRequestsTest {

  static final Repository PR_REPOSITORY = new Repository("1", "git", "space", "X");
  static final Repository NO_PR_REPOSITORY = new Repository("1", "svn", "us", "nasa");

  static final PullRequest OPEN_PR_FOR_USER = createPullRequest("open_dent", OPEN, "dent");
  static final PullRequest OPEN_PR_FOR_OTHER = createPullRequest("open_tricia", OPEN, "tricia");
  static final PullRequest CLOSED_PR_FOR_USER = createPullRequest("closed_dent", MERGED, "dent");

  @Mock
  RepositoryServiceFactory serviceFactory;
  @Mock
  RepositoryManager repositoryManager;
  @Mock
  PullRequestService pullRequestService;
  @Mock
  PullRequestMapper mapper;

  @InjectMocks
  MyPullRequests myPullRequests;

  @Mock
  Subject subject;

  @BeforeEach
  void mockUser() {
    ThreadContext.bind(subject);
    when(subject.getPrincipal()).thenReturn("dent");
  }

  @AfterEach
  void unbindUser() {
    ThreadContext.unbindSubject();
  }

  @BeforeEach
  void mockPrSupport() {
    RepositoryService repositoryServiceWithPr = mockedRepositoryService(true);
    when(serviceFactory.create(PR_REPOSITORY)).thenReturn(repositoryServiceWithPr);
    RepositoryService repositoryServiceWithoutPr = mockedRepositoryService(false);
    when(serviceFactory.create(NO_PR_REPOSITORY)).thenReturn(repositoryServiceWithoutPr);
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
    when(repositoryManager.getAll()).thenReturn(asList(PR_REPOSITORY, NO_PR_REPOSITORY));
    mockPullRequestsForRepositories();
    Iterable<MyData> data = myPullRequests.getData();
    assertThat(data).extracting("pullRequest").extracting("id").containsExactly("open_dent");
  }

  private RepositoryService mockedRepositoryService(boolean prSupported) {
    RepositoryService repositoryService = mock(RepositoryService.class);
    when(repositoryService.isSupported(Command.MERGE)).thenReturn(prSupported);
    return repositoryService;
  }

  private void mockPullRequestsForRepositories() {
    lenient().when(pullRequestService.getAll(not(eq("space")), not(eq("X")))).thenThrow(new AssertionFailedError("not expected call"));
    when(pullRequestService.getAll("space", "X")).thenReturn(asList(OPEN_PR_FOR_USER, OPEN_PR_FOR_OTHER, CLOSED_PR_FOR_USER));
  }

  private static PullRequest createPullRequest(String id, PullRequestStatus status, String author) {
    PullRequest existingPullRequest = new PullRequest();
    existingPullRequest.setId(id);
    existingPullRequest.setTitle("title " + id);
    existingPullRequest.setSource("source");
    existingPullRequest.setTarget("target");
    existingPullRequest.setStatus(status);
    existingPullRequest.setAuthor(author);
    return existingPullRequest;
  }
}
