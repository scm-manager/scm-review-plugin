package com.cloudogu.scm.review;

import com.google.common.collect.Maps;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.NotFoundException;
import sonia.scm.repository.Repository;
import sonia.scm.store.DataStore;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.Semaphore;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PullRequestStoreTest {

  private static final Repository REPOSITORY = new Repository("1", "git", "space", "X");

  private final Map<String, PullRequest> backingMap = Maps.newHashMap();
  @Mock
  private DataStore<PullRequest> dataStore;

  @InjectMocks
  private PullRequestStore store;

  private void setUpStore() {
    // delegate store methods to backing map
    when(dataStore.getAll()).thenReturn(backingMap);
    doAnswer(invocationOnMock -> {
      String id = invocationOnMock.getArgument(0);
      PullRequest pr = invocationOnMock.getArgument(1);
      backingMap.put(id, pr);
      return null;
    }).when(dataStore).put(anyString(), any(PullRequest.class));
  }

  @Test
  public void testAdd() {
    setUpStore();
    assertThat(store.add(REPOSITORY, createPullRequest())).isEqualTo("1");
    assertThat(store.add(REPOSITORY, createPullRequest())).isEqualTo("2");
    assertThat(store.add(REPOSITORY, createPullRequest())).isEqualTo("3");
  }

  @SuppressWarnings("squid:S2925") // suppress warnings regarding Thread.sleep. Found no other way to test this.
  @Test
  public void shouldCreateUniqueIdsWhenAccessedInParallel() throws InterruptedException {
    setUpStore();
    Semaphore semaphore = new Semaphore(2);
    semaphore.acquire(2);
    store = new PullRequestStore(dataStore) {
      @Override
      String createId() {
        String id = super.createId();
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
          fail("got interrupted", e);
        }
        return id;
      }
    };
    Thread first = new Thread() {
      @Override
      public void run() {
        PullRequest pullRequest = createPullRequest();
        pullRequest.setAuthor("first");
        store.add(REPOSITORY, pullRequest);
        semaphore.release();
      }
    };
    Thread second = new Thread() {
      @Override
      public void run() {
        PullRequest pullRequest = createPullRequest();
        pullRequest.setAuthor("second");
        store.add(REPOSITORY, pullRequest);
        semaphore.release();
      }
    };
    first.start();
    second.start();
    semaphore.acquire(2);
    assertThat(backingMap.size()).isEqualTo(2);
  }

  private PullRequest createPullRequest() {
    PullRequest pullRequest = new PullRequest("develop", "master", "Awesome PR");
    pullRequest.setDescription("Hitchhiker's guide to the galaxy");
    pullRequest.setAuthor("dent");
    return pullRequest;
  }

  @Test
  void shouldGetExistingPullRequest() {
    PullRequest pr = new PullRequest();
    when(dataStore.get("abc")).thenReturn(pr);
    assertThat(store.get(REPOSITORY, "abc")).isEqualTo(pr);
  }

  @Test
  void shouldThrowNotFoundException() {
    Assertions.assertThrows(NotFoundException.class, () -> store.get(REPOSITORY, "iDontExist"));
  }

  @Test
  void shouldSetCreationDateOnAdd() {
    PullRequest pr = mock(PullRequest.class);
    store.add(REPOSITORY, pr);
    verify(pr).setCreationDate(any(Instant.class));
  }
}
