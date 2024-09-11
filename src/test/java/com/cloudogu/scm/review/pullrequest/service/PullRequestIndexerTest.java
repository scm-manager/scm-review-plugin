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

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.HandlerEventType;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryImportEvent;
import sonia.scm.repository.RepositoryManager;
import sonia.scm.search.Id;
import sonia.scm.search.Index;
import sonia.scm.search.IndexLog;
import sonia.scm.search.IndexLogStore;
import sonia.scm.search.SearchEngine;
import sonia.scm.search.SerializableIndexTask;

import jakarta.servlet.ServletContextEvent;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"UnstableApiUsage", "rawtypes", "unchecked"})
class PullRequestIndexerTest {

  private final Repository repository = new Repository("1", "git", "hitchhiker", "42");

  @Mock
  private SearchEngine searchEngine;
  @Mock
  private SearchEngine.ForType<PullRequest> forType;

  @InjectMocks
  private PullRequestIndexer indexer;

  @BeforeEach
  void mockSearchEngine() {
    lenient().when(searchEngine.forType(PullRequest.class)).thenReturn(forType);
  }

  @Test
  void shouldIndexPullRequestForPullRequestEvent() {
    PullRequest pr = createPullRequest();
    indexer.handleEvent(new PullRequestEvent(repository, pr, pr, HandlerEventType.CREATE));

    verify(forType, times(1)).update(any(SerializableIndexTask.class));
  }

  @Test
  void shouldNotIndexPullRequestForPullRequestEventIfWrongHandlerType() {
    PullRequest pr = createPullRequest();
    indexer.handleEvent(new PullRequestEvent(repository, pr, pr, HandlerEventType.DELETE));

    verify(forType, never()).update(any(SerializableIndexTask.class));
  }

  @Test
  void shouldIndexPullRequestForPullRequestMergedEvent() {
    indexer.handleEvent(new PullRequestMergedEvent(repository, createPullRequest()));

    verify(forType, times(1)).update(any(SerializableIndexTask.class));
  }

  @Test
  void shouldIndexPullRequestForPullRequestUpdatedEvent() {
    indexer.handleEvent(new PullRequestUpdatedEvent(repository, createPullRequest()));

    verify(forType, times(1)).update(any(SerializableIndexTask.class));
  }

  @Test
  void shouldIndexPullRequestForPullRequestRejectedEvent() {
    indexer.handleEvent(new PullRequestRejectedEvent(repository, createPullRequest(), PullRequestRejectedEvent.RejectionCause.REJECTED_BY_USER));

    verify(forType, times(1)).update(any(SerializableIndexTask.class));
  }

  @Test
  void shouldIndexPullRequestForPullRequestEmergencyMergedEvent() {
    indexer.handleEvent(new PullRequestEmergencyMergedEvent(repository, createPullRequest()));

    verify(forType, times(1)).update(any(SerializableIndexTask.class));
  }


  @Test
  void shouldReindexAllPullRequestOnStartup() {
    ServletContextEvent event = mock(ServletContextEvent.class);
    indexer.contextInitialized(event);

    verify(forType).update(PullRequestIndexer.ReindexAll.class);
  }

  @Test
  void shouldCreateIndexAfterSuccessfulImport() {
    indexer.handleEvent(new RepositoryImportEvent(repository, false));

    verify(forType).update(any(SerializableIndexTask.class));
  }

  @Test
  void shouldNotCreateIndexAfterFailedImport() {
    indexer.handleEvent(new RepositoryImportEvent(repository, true));

    verify(forType, never()).update(any(SerializableIndexTask.class));
  }

  @Nested
  class ReindexAllTests {
    @Mock
    private RepositoryManager repositoryManager;
    @Mock
    private PullRequestService service;
    @Mock
    private IndexLogStore indexLogStore;
    @Mock
    private IndexLogStore.ForIndex forIndex;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Index<PullRequest> index;

    @InjectMocks
    private PullRequestIndexer.ReindexAll reindexAll;

    @BeforeEach
    void mockLogStore() {
      when(indexLogStore.defaultIndex()).thenReturn(forIndex);
    }

    @Test
    void shouldNotReindexAllIfVersionHasNotChanged() {
      when(forIndex.get(PullRequest.class)).thenReturn(Optional.of(new IndexLog(1)));

      reindexAll.update(index);

      verify(index, never()).delete();
    }

    @Test
    void shouldNotReindexRepositoryIfDoesNotSupportPullRequests() {
      Index.Deleter deleter = mock(Index.Deleter.class);
      when(service.supportsPullRequests(repository)).thenReturn(false);
      when(forIndex.get(PullRequest.class)).thenReturn(Optional.of(new IndexLog(42)));
      when(index.delete()).thenReturn(deleter);

      when(repositoryManager.getAll()).thenReturn(ImmutableList.of(repository));

      reindexAll.update(index);

      verify(deleter, times(1)).all();
      verify(index, never()).store(
        any(Id.class),
        anyString(),
        any(PullRequest.class)
      );
    }

    @Test
    void shouldReindexAllIfLogStoreIsEmpty() {
      Index.Deleter deleter = mock(Index.Deleter.class);
      when(forIndex.get(PullRequest.class)).thenReturn(Optional.empty());
      when(index.delete()).thenReturn(deleter);

      reindexAll.update(index);

      verify(deleter, times(1)).all();
    }

    @Test
    void shouldReindexAllIfLogStoreVersionDiffers() {
      when(service.supportsPullRequests(repository)).thenReturn(true);
      when(forIndex.get(PullRequest.class)).thenReturn(Optional.of(new IndexLog(42)));

      PullRequest pullRequest = createPullRequest();
      when(repositoryManager.getAll()).thenReturn(ImmutableList.of(repository));
      when(service.getAll(repository.getNamespace(), repository.getName())).thenReturn(ImmutableList.of(pullRequest));

      reindexAll.update(index);

      verify(index.delete()).all();
      verify(index).store(
        Id.of(PullRequest.class, pullRequest.getId()).and(Repository.class, repository.getId()),
        "repository:readPullRequest:" + pullRequest.getId(),
        pullRequest
      );
    }
  }

  @Nested
  class IndexRepositoryTaskTaskTests {

    @Mock
    private PullRequestService service;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Index<PullRequest> index;

    @Test
    void shouldReindex() {
      when(service.supportsPullRequests(repository)).thenReturn(true);

      PullRequest pullRequest = createPullRequest();
      when(service.getAll(repository.getNamespace(), repository.getName())).thenReturn(ImmutableList.of(pullRequest));

      PullRequestIndexer.IndexRepositoryTask indexRepositoryTask = new PullRequestIndexer.IndexRepositoryTask(repository);
      indexRepositoryTask.setPullRequestService(service);

      indexRepositoryTask.update(index);

      verify(index).store(
        Id.of(PullRequest.class, pullRequest.getId()).and(Repository.class, repository.getId()),
        "repository:readPullRequest:" + pullRequest.getId(),
        pullRequest
      );
    }
  }

  @Nested
  class ReIndexRepositoryTaskTests {

    @Mock
    private PullRequestService service;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Index<PullRequest> index;

    @Test
    void shouldReindex() {
      when(service.supportsPullRequests(repository)).thenReturn(true);

      PullRequest pullRequest = createPullRequest();
      when(service.getAll(repository.getNamespace(), repository.getName())).thenReturn(ImmutableList.of(pullRequest));

      PullRequestIndexer.ReindexRepositoryTask reindexRepositoryTask = new PullRequestIndexer.ReindexRepositoryTask(repository);
      reindexRepositoryTask.setPullRequestService(service);

      reindexRepositoryTask.update(index);

      verify(index.delete()).by(Repository.class, repository);
      verify(index).store(
        Id.of(PullRequest.class, pullRequest.getId()).and(Repository.class, repository.getId()),
        "repository:readPullRequest:" + pullRequest.getId(),
        pullRequest
      );
    }
  }

  private PullRequest createPullRequest() {
    return new PullRequest.PullRequestBuilder().id("1").build();
  }
}
