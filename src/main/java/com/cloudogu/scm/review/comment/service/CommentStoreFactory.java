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

package com.cloudogu.scm.review.comment.service;

import sonia.scm.repository.Repository;
import sonia.scm.security.KeyGenerator;
import sonia.scm.store.DataStore;
import sonia.scm.store.DataStoreFactory;

import jakarta.inject.Inject;

public class CommentStoreFactory {

  private static final String PULL_REQUEST_COMMENT_STORE_NAME = "pullRequestComment";

  private final DataStoreFactory dataStoreFactory;
  private final KeyGenerator keyGenerator;

  @Inject
  public CommentStoreFactory(DataStoreFactory dataStoreFactory, KeyGenerator keyGenerator) {
    this.dataStoreFactory = dataStoreFactory;
    this.keyGenerator = keyGenerator;
  }

  public CommentStore create(Repository repository) {
    DataStore<PullRequestComments> store = dataStoreFactory.withType(PullRequestComments.class).withName(PULL_REQUEST_COMMENT_STORE_NAME).forRepository(repository).build();
    return new CommentStore(store, keyGenerator);
  }

}
