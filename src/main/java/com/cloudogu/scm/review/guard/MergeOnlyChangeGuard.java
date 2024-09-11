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

package com.cloudogu.scm.review.guard;

import com.cloudogu.scm.editor.ChangeGuard;
import com.cloudogu.scm.editor.ChangeObstacle;
import com.cloudogu.scm.review.config.service.ConfigService;
import sonia.scm.plugin.Extension;
import sonia.scm.plugin.Requires;
import sonia.scm.repository.NamespaceAndName;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryManager;

import jakarta.inject.Inject;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Stream;

@Extension
@Requires("scm-editor-plugin")
public class MergeOnlyChangeGuard implements ChangeGuard {

  private final RepositoryManager repositoryManager;
  private final ConfigService configService;

  @Inject
  public MergeOnlyChangeGuard(RepositoryManager repositoryManager, ConfigService configService) {
    this.repositoryManager = repositoryManager;
    this.configService = configService;
  }

  @Override
  public Collection<ChangeObstacle> getObstacles(NamespaceAndName namespaceAndName, String branch, Changes changes) {
    Repository repository = repositoryManager.get(namespaceAndName);
    if (preventModification(repository, branch, changes)) {
      return Collections.singleton(new ChangeObstacle() {
        @Override
        public String getMessage() {
          return "The branch " + branch + " can only be written using pull requests";
        }

        @Override
        public String getKey() {
          return "scm-review-plugin.obstacle";
        }
      });
    } else {
      return Collections.emptyList();
    }
  }

  private boolean preventModification(Repository repository, String branch, Changes changes) {
    return Stream.of(changes.getFilesToCreate(), changes.getFilesToModify(), changes.getFilesToDelete())
      .flatMap(Collection::stream)
      .anyMatch(path -> configService.isBranchPathProtected(repository, branch, path))
      || (changes.getPathForCreate().isPresent() && configService.isBranchPathProtected(repository, branch, changes.getPathForCreate().get()));
  }
}
