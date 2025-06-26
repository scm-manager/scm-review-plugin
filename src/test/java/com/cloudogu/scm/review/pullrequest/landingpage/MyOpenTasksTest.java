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

import com.cloudogu.scm.landingpage.mytasks.MyTask;
import com.cloudogu.scm.review.comment.service.Comment;
import com.cloudogu.scm.review.comment.service.CommentStoreFactory;
import com.cloudogu.scm.review.comment.service.CommentType;
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

import static com.cloudogu.scm.review.comment.service.CommentType.COMMENT;
import static com.cloudogu.scm.review.comment.service.CommentType.TASK_DONE;
import static com.cloudogu.scm.review.comment.service.CommentType.TASK_TODO;
import static com.cloudogu.scm.review.pullrequest.service.PullRequestStatus.OPEN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, QueryableStoreExtension.class})
@QueryableStoreExtension.QueryableTypes({PullRequest.class, Comment.class})
class MyOpenTasksTest {

  static final Repository PR_REPOSITORY = new Repository("1", "git", "space", "X");

  static final PullRequest OPEN_PR_FOR_USER_WITHOUT_TASK = createPullRequest("open_dent", OPEN, "dent");
  static final PullRequest OPEN_PR_FOR_USER_WITH_TASK = createPullRequest("open_dent_tasks", OPEN, "dent");
  static final PullRequest OPEN_PR_FOR_OTHER = createPullRequest("open_tricia", OPEN, "tricia");

  @Mock
  OpenPullRequestProvider pullRequestProvider;
  @Mock
  PullRequestMapper mapper;
  @Mock
  RepositoryManager repositoryManager;

  MyOpenTasks myOpenTasks;

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
  void mockData(PullRequestStoreFactory pullRequestStoreFactory, CommentStoreFactory commentStoreFactory) {
    try (QueryableMutableStore<PullRequest> prStore = pullRequestStoreFactory.getMutable(PR_REPOSITORY.getId())) {
      prStore.put(OPEN_PR_FOR_USER_WITH_TASK.getId(), OPEN_PR_FOR_USER_WITH_TASK);
      prStore.put(OPEN_PR_FOR_USER_WITHOUT_TASK.getId(), OPEN_PR_FOR_USER_WITHOUT_TASK);
      prStore.put(OPEN_PR_FOR_OTHER.getId(), OPEN_PR_FOR_OTHER);
    }

    try (QueryableMutableStore<Comment> storeWithOpenTask = commentStoreFactory.getMutable(PR_REPOSITORY.getId(), "open_dent_tasks")) {
      storeWithOpenTask.put(createComment(COMMENT));
      storeWithOpenTask.put(createComment(TASK_TODO));
    }
    try (QueryableMutableStore<Comment> storeWithClosedTask = commentStoreFactory.getMutable(PR_REPOSITORY.getId(), "open_dent")) {
      storeWithClosedTask.put(createComment(COMMENT));
      storeWithClosedTask.put(createComment(TASK_DONE));
    }

    when(repositoryManager.get(PR_REPOSITORY.getId())).thenReturn(PR_REPOSITORY);
  }

  @BeforeEach
  void initTasks(PullRequestStoreFactory pullRequestStoreFactory, CommentStoreFactory commentStoreFactory) {
    myOpenTasks = new MyOpenTasks(mapper, pullRequestStoreFactory, commentStoreFactory, repositoryManager);
  }

  @Test
  void shouldFindMyPullRequests() {
    Iterable<MyTask> data = myOpenTasks.getTasks();
    assertThat(data).extracting("pullRequest").extracting("id").containsExactly("open_dent_tasks");
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

  private static Comment createComment(CommentType type) {
    Comment comment = new Comment();
    comment.setType(type);
    return comment;
  }
}
