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

import jakarta.inject.Inject;
import sonia.scm.repository.Repository;
import sonia.scm.store.QueryableMutableStore;
import sonia.scm.store.QueryableStore;

public class PullRequestStoreBuilder {

  private final PullRequestStoreFactory storeFactory;

  @Inject
  public PullRequestStoreBuilder(PullRequestStoreFactory storeFactory) {
    this.storeFactory = storeFactory;
  }

  public PullRequestStore create(Repository repository) {
    QueryableMutableStore<PullRequest> store = storeFactory.getMutable(repository.getId());
    return new PullRequestStore(store, repository);
  }

  public QueryableStore<PullRequest> createQueryable(Repository repository) {
    return storeFactory.get(repository.getId());
  }
}
