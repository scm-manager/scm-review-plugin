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

package com.cloudogu.scm.review;

import sonia.scm.ContextEntry;
import sonia.scm.repository.NamespaceAndName;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryManager;
import sonia.scm.repository.api.Command;
import sonia.scm.repository.api.RepositoryService;
import sonia.scm.repository.api.RepositoryServiceFactory;

import jakarta.inject.Inject;

import static sonia.scm.NotFoundException.notFound;

public class RepositoryResolver {

  private final RepositoryManager manager;
  private final RepositoryServiceFactory serviceFactory;

  @Inject
  public RepositoryResolver(RepositoryManager manager, RepositoryServiceFactory serviceFactory) {
    this.manager = manager;
    this.serviceFactory = serviceFactory;
  }

  public Repository resolve(NamespaceAndName namespaceAndName) {
    Repository repository = manager.get(namespaceAndName);
    if (repository == null) {
      throw notFound(ContextEntry.ContextBuilder.entity(namespaceAndName));
    }

    if (!repositorySupportsMerge(repository)) {
      throw new PullRequestNotSupportedException(repository);
    }

    return repository;
  }

  private boolean repositorySupportsMerge(Repository repository) {
    try (RepositoryService service = serviceFactory.create(repository)) {
      return service.isSupported(Command.MERGE);
    }
  }
}
