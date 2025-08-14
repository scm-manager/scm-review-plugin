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

import com.cloudogu.scm.landingpage.mydata.MyData;
import com.cloudogu.scm.review.pullrequest.dto.PullRequestDto;
import com.cloudogu.scm.review.pullrequest.dto.PullRequestMapper;
import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import com.cloudogu.scm.review.pullrequest.service.PullRequestStatus;
import com.cloudogu.scm.review.pullrequest.service.PullRequestStoreFactory;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryManager;
import sonia.scm.store.QueryableMutableStore;
import sonia.scm.store.QueryableStoreExtension;

import static com.cloudogu.scm.review.pullrequest.service.PullRequestStatus.DRAFT;
import static com.cloudogu.scm.review.pullrequest.service.PullRequestStatus.MERGED;
import static com.cloudogu.scm.review.pullrequest.service.PullRequestStatus.OPEN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, QueryableStoreExtension.class})
@QueryableStoreExtension.QueryableTypes({PullRequest.class})
class MyPullRequestsTest {

  @Mock
  PullRequestMapper mapper;
  @Mock
  RepositoryManager repositoryManager;

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
  void mockMapper() {
    when(mapper.map(any(), any())).thenAnswer(invocationOnMock -> {
      PullRequestDto dto = new PullRequestDto();
      dto.setId(invocationOnMock.getArgument(0, PullRequest.class).getId());
      return dto;
    });
  }

  @BeforeEach
  void setUpMyPullRequests(PullRequestStoreFactory storeFactory) {
    myPullRequests = new MyPullRequests(mapper, storeFactory, repositoryManager);
  }

  @Test
  void shouldFindMyPullRequests(PullRequestStoreFactory storeFactory) {
    try (QueryableMutableStore<PullRequest> store = storeFactory.getMutable("1")) {
      store.put("open_dent", createPullRequest("open_dent", OPEN, "dent"));
      store.put("closed_dent", createPullRequest("closed_dent", MERGED, "dent"));
      store.put("open_tricia", createPullRequest("open_tricia", OPEN, "tricia"));
    }
    try (QueryableMutableStore<PullRequest> store = storeFactory.getMutable("2")) {
      store.put("draft_dent", createPullRequest("draft_dent", DRAFT, "dent"));
    }
    when(repositoryManager.get("1")).thenReturn(new Repository("1", "git", "space", "X"));
    when(repositoryManager.get("2")).thenReturn(new Repository("2", "git", "space", "balls"));

    Iterable<MyData> data = myPullRequests.getData();
    assertThat(data)
      .extracting("pullRequest")
      .extracting("id")
      .containsExactlyInAnyOrder("open_dent", "draft_dent");
    assertThat(data)
      .extracting("name")
      .containsExactlyInAnyOrder("X", "balls");
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
