/*
 * MIT License
 *
 * Copyright (c) 2020-present Cloudogu GmbH and Contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
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
