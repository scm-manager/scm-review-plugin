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
import jakarta.inject.Inject;
import org.mapstruct.Mapper;
import sonia.scm.migration.RepositoryUpdateContext;
import sonia.scm.migration.RepositoryUpdateStep;
import sonia.scm.plugin.Extension;
import sonia.scm.store.DataStore;
import sonia.scm.store.DataStoreFactory;
import sonia.scm.store.QueryableMutableStore;
import sonia.scm.store.QueryableStoreFactory;
import sonia.scm.version.Version;

@Extension
public class MovePullRequestToQueryableStoreUpdateStep implements RepositoryUpdateStep {
  private final DataStoreFactory dataStoreFactory;
  private final QueryableStoreFactory queryableStoreFactory;

  @Inject
  public MovePullRequestToQueryableStoreUpdateStep(DataStoreFactory dataStoreFactory, QueryableStoreFactory queryableStoreFactory) {
    this.dataStoreFactory = dataStoreFactory;
    this.queryableStoreFactory = queryableStoreFactory;
  }

  @Override
  public void doUpdate(RepositoryUpdateContext repositoryUpdateContext) throws Exception {
    String repositoryId = repositoryUpdateContext.getRepositoryId();

    DataStore<LegacyXmlPullRequest> xmlStore = dataStoreFactory.withType(LegacyXmlPullRequest.class).withName("pullRequest").forRepository(repositoryId).build();
    try (QueryableMutableStore<PullRequest> qStore = queryableStoreFactory.getMutable(PullRequest.class, repositoryId)) {
      qStore.transactional(
        () -> {
          xmlStore.getAll()
            .forEach(
              (id, pr) -> {
                if (pr.getLastModified() == null) {
                  pr.setLastModified(pr.getCreationDate());
                }
                qStore.put(id, new LegacyPullRequestMapperImpl().map(pr));
              }
            );
          return true;
        }
      );
    }
  }

  @Override
  public Version getTargetVersion() {
    return Version.parse("2.0.0");
  }

  @Override
  public String getAffectedDataType() {
    return "pullRequestStore";
  }
}

@Mapper
interface LegacyPullRequestMapper {
  PullRequest map(LegacyXmlPullRequest legacyPullRequest);
}
