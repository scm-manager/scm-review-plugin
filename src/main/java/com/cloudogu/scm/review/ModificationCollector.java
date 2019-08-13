package com.cloudogu.scm.review;

import sonia.scm.repository.Changeset;
import sonia.scm.repository.Modifications;
import sonia.scm.repository.Repository;
import sonia.scm.repository.api.RepositoryService;
import sonia.scm.repository.api.RepositoryServiceFactory;

import javax.inject.Inject;
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
    paths.addAll(modifications.getAdded());
    paths.addAll(modifications.getModified());
    paths.addAll(modifications.getRemoved());
  }
}
