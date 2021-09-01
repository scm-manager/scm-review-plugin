package com.cloudogu.scm.review.pullrequest.service;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.HandlerEventType;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryManager;
import sonia.scm.repository.RepositoryTestData;
import sonia.scm.search.Id;
import sonia.scm.search.Index;
import sonia.scm.search.IndexLog;
import sonia.scm.search.IndexLogStore;
import sonia.scm.search.SearchEngine;
import sonia.scm.search.SerializableIndexTask;

import javax.servlet.ServletContextEvent;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"UnstableApiUsage", "rawtypes", "unchecked"})
class PullRequestIndexerTest {

  private final Repository repository = RepositoryTestData.create42Puzzle();

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

    @Mock
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
    void shouldReindexAllIfLogStoreIsEmpty() {
      Index.Deleter deleter = mock(Index.Deleter.class);
      when(forIndex.get(PullRequest.class)).thenReturn(Optional.empty());
      when(index.delete()).thenReturn(deleter);

      reindexAll.update(index);

      verify(deleter, times(1)).all();
    }

    @Test
    void shouldReindexAllIfLogStoreVersionDiffers() {
      Index.Deleter deleter = mock(Index.Deleter.class);
      when(forIndex.get(PullRequest.class)).thenReturn(Optional.of(new IndexLog(42)));
      when(index.delete()).thenReturn(deleter);

      PullRequest pullRequest = createPullRequest();
      when(repositoryManager.getAll()).thenReturn(ImmutableList.of(repository));
      when(service.getAll(repository.getNamespace(), repository.getName())).thenReturn(ImmutableList.of(pullRequest));

      reindexAll.update(index);

      verify(deleter, times(1)).all();
      verify(index).store(
        Id.of(PullRequest.class, pullRequest.getId()).and(Repository.class, repository.getId()),
        "repository:readPullRequest:id-" + pullRequest.getId(),
        pullRequest
      );
    }
  }


  private PullRequest createPullRequest() {
    return new PullRequest.PullRequestBuilder().id("1").build();
  }
}
