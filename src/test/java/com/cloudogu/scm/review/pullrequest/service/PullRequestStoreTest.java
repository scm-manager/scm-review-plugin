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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.NotFoundException;
import sonia.scm.repository.Repository;
import sonia.scm.store.QueryableMutableStore;
import sonia.scm.store.QueryableStoreExtension;

import java.util.concurrent.Semaphore;

import static com.cloudogu.scm.review.TestData.createPullRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.fail;

@ExtendWith({MockitoExtension.class, QueryableStoreExtension.class})
@QueryableStoreExtension.QueryableTypes(PullRequest.class)
class PullRequestStoreTest {

  private Repository repository = new Repository("1", "git", "space", "X");

  private PullRequestStore store;
  private QueryableMutableStore<PullRequest> dataStore;

  @BeforeEach
  void init(PullRequestStoreFactory dataStoreFactory) {
    dataStore = dataStoreFactory.getMutable(repository.getId());
    store = new PullRequestStore(dataStore, repository);
  }

  @AfterEach
  void closeStore() {
    dataStore.close();
  }

  @Test
  void shouldCreateIncreasingIdsStartingWithOne() {
    for (int i = 0; i < 12; i++) {
      assertThat(store.add(createPullRequest())).isEqualTo(String.valueOf(i + 1));
    }
  }

  @Test
  void testAddWithMissingPullRequest() {
    store.add(createPullRequest());
    store.add(createPullRequest());
    store.add(createPullRequest());

    dataStore.remove("2");

    assertThat(store.add(createPullRequest())).isEqualTo("4");
  }

  @SuppressWarnings("squid:S2925") // suppress warnings regarding Thread.sleep. Found no other way to test this.
  @Test
  void shouldCreateUniqueIdsWhenAccessedInParallel() throws InterruptedException {
    Semaphore semaphore = new Semaphore(2);
    semaphore.acquire(2);
    store = new PullRequestStore(dataStore, repository) {
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
    Thread first = new Thread(() -> {
      PullRequest pullRequest = createPullRequest();
      pullRequest.setAuthor("first");
      store.add(pullRequest);
      semaphore.release();
    });
    Thread second = new Thread(() -> {
      PullRequest pullRequest = createPullRequest();
      pullRequest.setAuthor("second");
      store.add(pullRequest);
      semaphore.release();
    });
    first.start();
    second.start();
    semaphore.acquire(2);
    assertThat(dataStore.getAll()).hasSize(2);
  }


  @Test
  void shouldGetExistingPullRequest() {
    PullRequest pr = createPullRequest();

    dataStore.put("abc", pr);

    assertThat(store.get("abc")).isEqualTo(pr);
  }

  @Test
  void shouldGetExistingPullRequests() {
    PullRequest pullRequest1 = createPullRequest();
    PullRequest pullRequest2 = createPullRequest();

    dataStore.put("1", pullRequest1);
    dataStore.put("2", pullRequest2);

    assertThat(store.getAll())
      .asList()
      .hasSize(2)
      .containsExactlyInAnyOrder(pullRequest1, pullRequest2);
  }

  @Test
  void shouldThrowNotFoundException() {
    Assertions.assertThrows(NotFoundException.class, () -> store.get("iDontExist"));
  }
}
