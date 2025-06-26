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
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import sonia.scm.migration.RepositoryUpdateContext;
import sonia.scm.migration.RepositoryUpdateStep;
import sonia.scm.plugin.Extension;
import sonia.scm.store.DataStore;
import sonia.scm.store.DataStoreFactory;
import sonia.scm.store.QueryableMutableStore;
import sonia.scm.store.QueryableStoreFactory;
import sonia.scm.version.Version;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Extension
public class MovePullRequestChangeToQueryableStoreUpdateStep implements RepositoryUpdateStep {
  private final DataStoreFactory dataStoreFactory;
  private final QueryableStoreFactory queryableStoreFactory;

  @Inject
  public MovePullRequestChangeToQueryableStoreUpdateStep(DataStoreFactory dataStoreFactory, QueryableStoreFactory queryableStoreFactory) {
    this.dataStoreFactory = dataStoreFactory;
    this.queryableStoreFactory = queryableStoreFactory;
  }

  @Override
  public void doUpdate(RepositoryUpdateContext repositoryUpdateContext) throws Exception {
    String repositoryId = repositoryUpdateContext.getRepositoryId();

    DataStore<PullRequestChangeContainer> oldStore = dataStoreFactory
      .withType(PullRequestChangeContainer.class)
      .withName("pullRequestChanges")
      .forRepository(repositoryId)
      .build();

    oldStore.getAll().forEach(
      (prId, changeContainer) -> {
        try (QueryableMutableStore<PullRequestChange> prChangeStore = queryableStoreFactory.getMutable(PullRequestChange.class, repositoryId, prId)) {
          List<LegacyXmlPullRequestChange> changes = oldStore.get(prId).getAllChanges();

          prChangeStore.transactional(() -> {
            AtomicInteger counter = new AtomicInteger(1);
            changes.stream()
              .map(change -> new PullRequestChange(
                prId,
                change.getUsername(),
                change.getDisplayName(),
                change.getMail(),
                change.getChangedAt(),
                change.getPreviousValue(),
                change.getCurrentValue(),
                change.getProperty(),
                change.getAdditionalInfo()
              ))
              .forEach(change -> prChangeStore.put(String.valueOf(counter.getAndIncrement()), change));
            return true;
          });
        } catch (Exception ex) {
          log.error("Could not migrate pull request changes from repository {} to queryable store for PR {}", repositoryId, prId, ex);
        }

      }
    );
  }

  @Override
  public Version getTargetVersion() {
    return Version.parse("2.0.0");
  }

  @Override
  public String getAffectedDataType() {
    return "pullRequestChanges";
  }

}


