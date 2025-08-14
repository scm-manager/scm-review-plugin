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

import com.cloudogu.scm.review.pullrequest.service.PullRequestChange;
import com.cloudogu.scm.review.pullrequest.service.PullRequestChangeStoreFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith({QueryableStoreExtension.class})
@QueryableStoreExtension.QueryableTypes({PullRequestChange.class})
public class MovePullRequestChangeToQueryableStoreUpdateStepTest {

  private final String repositoryId = "test-repo";

  private DataStore<PullRequestChangeContainer> oldStore;
  private MovePullRequestChangeToQueryableStoreUpdateStep updateStep;

  @BeforeEach
  void init(QueryableStoreFactory queryableStoreFactory) {

    DataStoreFactory dataStoreFactory = new InMemoryByteDataStoreFactory();

    oldStore = dataStoreFactory
      .withType(PullRequestChangeContainer.class)
      .withName("pullRequestChanges")
      .forRepository(repositoryId)
      .build();

    updateStep = new MovePullRequestChangeToQueryableStoreUpdateStep(
      dataStoreFactory,
      queryableStoreFactory
    );
  }

  @Test
  void shouldMigrateLegacyChanges(PullRequestChangeStoreFactory storeFactory) throws Exception {
    String pullRequestId = "1";

    PullRequestChangeContainer container = new PullRequestChangeContainer();
    List<LegacyXmlPullRequestChange> legacyList = createLegacyChanges(pullRequestId);
    container.getAllChanges().addAll(legacyList);

    oldStore.put(pullRequestId, container);
    updateStep.doUpdate(new RepositoryUpdateContext(repositoryId));

    try (QueryableMutableStore<PullRequestChange> queryableStore = storeFactory.getMutable(repositoryId, pullRequestId)) {
      Map<String, PullRequestChange> all = queryableStore.getAll();


      assertEquals(2, all.size());
      assertThat(all.get("1")).usingRecursiveComparison().isEqualTo(legacyList.get(0));
      assertThat(all.get("2")).usingRecursiveComparison().isEqualTo(legacyList.get(1));
    }
  }

  private List<LegacyXmlPullRequestChange> createLegacyChanges(String pullRequestId) {
    List<LegacyXmlPullRequestChange> changes = List.of(
      new LegacyXmlPullRequestChange(
        pullRequestId,
        "username",
        "displayName",
        "email@email.com",
        Instant.now(),
        "previous",
        "current",
        "property",
        Map.of("key1", "value1")
      ),
      new LegacyXmlPullRequestChange(
        pullRequestId,
        "username2",
        "displayName2",
        "email2@email.com",
        Instant.now(),
        "previous2",
        "current2",
        "property2",
        Map.of("key2", "value2")
      )
    );
    return changes;
  }
}
