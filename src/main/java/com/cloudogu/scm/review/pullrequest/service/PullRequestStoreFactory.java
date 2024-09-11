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

import sonia.scm.repository.Repository;
import sonia.scm.store.DataStore;
import sonia.scm.store.DataStoreFactory;

import jakarta.inject.Inject;

public class PullRequestStoreFactory {

  private static final String PULL_REQUEST_STORE_NAME = "pullRequest";

  private final DataStoreFactory dataStoreFactory;

  @Inject
  public PullRequestStoreFactory(DataStoreFactory dataStoreFactory) {
    this.dataStoreFactory = dataStoreFactory;
  }

  public PullRequestStore create(Repository repository) {
    DataStore<PullRequest> store = dataStoreFactory.withType(PullRequest.class).withName(PULL_REQUEST_STORE_NAME).forRepository(repository).build();
    return new PullRequestStore(store, repository);
  }

}
