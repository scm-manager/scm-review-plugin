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

import sonia.scm.repository.Changeset;
import sonia.scm.repository.Modifications;
import sonia.scm.repository.Repository;
import sonia.scm.repository.api.RepositoryService;
import sonia.scm.repository.api.RepositoryServiceFactory;

import jakarta.inject.Inject;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

class ModificationCollector {

  private final RepositoryServiceFactory repositoryServiceFactory;

  @Inject
  ModificationCollector(RepositoryServiceFactory repositoryServiceFactory) {
    this.repositoryServiceFactory = repositoryServiceFactory;
  }

  Set<String> collect(Repository repository, Iterable<Changeset> changesets) throws IOException {
    Set<String> paths = new HashSet<>();
    try (RepositoryService repositoryService = repositoryServiceFactory.create(repository)) {
      for (Changeset changeset : changesets) {
        append(paths, repositoryService, changeset);
      }

      return Collections.unmodifiableSet(paths);
    }
  }

  private void append(Set<String> paths, RepositoryService repositoryService, Changeset changeset) throws IOException {
    Modifications modifications = repositoryService.getModificationsCommand().revision(changeset.getId()).getModifications();
    paths.addAll(modifications.getEffectedPaths());
  }
}
