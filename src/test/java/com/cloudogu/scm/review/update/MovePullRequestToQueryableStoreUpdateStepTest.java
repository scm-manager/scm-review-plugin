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

package com.cloudogu.scm.review.update;

import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import com.cloudogu.scm.review.pullrequest.service.PullRequestStatus;
import com.cloudogu.scm.review.pullrequest.service.PullRequestStoreFactory;
import com.cloudogu.scm.review.pullrequest.service.ReviewMark;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.migration.RepositoryUpdateContext;
import sonia.scm.store.DataStore;
import sonia.scm.store.DataStoreFactory;
import sonia.scm.store.InMemoryByteDataStoreFactory;
import sonia.scm.store.QueryableMutableStore;
import sonia.scm.store.QueryableStoreExtension;
import sonia.scm.store.QueryableStoreFactory;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith({MockitoExtension.class, QueryableStoreExtension.class})
@QueryableStoreExtension.QueryableTypes({PullRequest.class})
class MovePullRequestToQueryableStoreUpdateStepTest {

  private final String repositoryId = "Johto";

  private DataStore<LegacyXmlPullRequest> dataStore;
  private MovePullRequestToQueryableStoreUpdateStep updateStep;

  @BeforeEach
  void init(QueryableStoreFactory queryableStoreFactory) {
    DataStoreFactory dataStoreFactory = new InMemoryByteDataStoreFactory();
    dataStore = dataStoreFactory.withType(LegacyXmlPullRequest.class).withName("pullRequest").forRepository(repositoryId).build();
    updateStep = new MovePullRequestToQueryableStoreUpdateStep(dataStoreFactory, queryableStoreFactory);
  }

  @Test
  void shouldMovePullRequestsToQueryableStore(PullRequestStoreFactory storeFactory) throws Exception {
    LegacyXmlPullRequest first = createFirstPullRequest();
    LegacyXmlPullRequest second = createSecondPullRequest();
    dataStore.put(first.getId(), first);
    dataStore.put(second.getId(), second);

    updateStep.doUpdate(new RepositoryUpdateContext(repositoryId));

    Map<String, PullRequest> pullRequests = getAll(storeFactory);
    assertThat(pullRequests).hasSize(2);
    assertThat(pullRequests.get("1")).usingRecursiveComparison().isEqualTo(first);
    assertThat(pullRequests.get("2")).usingRecursiveComparison().isEqualTo(second);
  }

  @Test
  void shouldSetCreationDateAsDefaultValueForLastModified(PullRequestStoreFactory storeFactory) throws Exception {
    LegacyXmlPullRequest first = createFirstPullRequest();
    first.setLastModified(null);
    dataStore.put(first.getId(), first);

    updateStep.doUpdate(new RepositoryUpdateContext(repositoryId));

    Map<String, PullRequest> pullRequests = getAll(storeFactory);
    assertThat(pullRequests).hasSize(1);
    assertThat(pullRequests.get("1").getLastModified()).isEqualTo(first.getCreationDate());
  }

  private LegacyXmlPullRequest createFirstPullRequest() {
    LegacyXmlPullRequest firstPullRequest = new LegacyXmlPullRequest();
    firstPullRequest.setId("1");
    firstPullRequest.setSource("first source");
    firstPullRequest.setTarget("first target");
    firstPullRequest.setTitle("first title");
    firstPullRequest.setDescription("first description");
    firstPullRequest.setAuthor("first author");
    firstPullRequest.setReviser("first reviser");
    firstPullRequest.setCreationDate(Instant.parse("2025-01-01T13:37:00.0Z"));
    firstPullRequest.setLastModified(Instant.parse("2025-01-02T13:37:00.0Z"));
    firstPullRequest.setCloseDate(Instant.parse("2025-01-03T13:37:00.0Z"));
    firstPullRequest.setStatus(PullRequestStatus.OPEN);
    firstPullRequest.setSubscriber(Set.of("first subscriber"));
    firstPullRequest.setLabels(Set.of("first label"));
    firstPullRequest.setReviewer(Map.of("first reviewer", true));
    firstPullRequest.setReviewMarks(Set.of(new ReviewMark("first file", "first user")));
    firstPullRequest.setOverrideMessage("first override message");
    firstPullRequest.setEmergencyMerged(true);
    firstPullRequest.setIgnoredMergeObstacles(List.of("first obstacle"));
    firstPullRequest.setShouldDeleteSourceBranch(true);

    return firstPullRequest;
  }

  private LegacyXmlPullRequest createSecondPullRequest() {
    LegacyXmlPullRequest secondPullRequest = new LegacyXmlPullRequest();
    secondPullRequest.setId("2");
    secondPullRequest.setSource("second source");
    secondPullRequest.setTarget("second target");
    secondPullRequest.setTitle("second title");
    secondPullRequest.setDescription("second description");
    secondPullRequest.setAuthor("second author");
    secondPullRequest.setReviser("second reviser");
    secondPullRequest.setCreationDate(Instant.parse("2025-02-01T13:37:00.0Z"));
    secondPullRequest.setLastModified(Instant.parse("2025-02-02T13:37:00.0Z"));
    secondPullRequest.setCloseDate(Instant.parse("2025-02-03T13:37:00.0Z"));
    secondPullRequest.setStatus(PullRequestStatus.MERGED);
    secondPullRequest.setSubscriber(Set.of("second subscriber"));
    secondPullRequest.setLabels(Set.of("second label"));
    secondPullRequest.setReviewer(Map.of("second reviewer", true));
    secondPullRequest.setReviewMarks(Set.of(new ReviewMark("second file", "second user")));
    secondPullRequest.setOverrideMessage("second override message");
    secondPullRequest.setEmergencyMerged(false);
    secondPullRequest.setIgnoredMergeObstacles(List.of("second obstacle"));
    secondPullRequest.setShouldDeleteSourceBranch(false);

    return secondPullRequest;
  }

  private Map<String, PullRequest> getAll(PullRequestStoreFactory storeFactory) {
    try (QueryableMutableStore<PullRequest> store = storeFactory.getMutable(repositoryId)) {
      return store.getAll();
    }
  }
}
