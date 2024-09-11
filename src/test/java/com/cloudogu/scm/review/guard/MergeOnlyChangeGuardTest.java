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

import com.cloudogu.scm.editor.ChangeGuard.Changes;
import com.cloudogu.scm.editor.ChangeObstacle;
import com.cloudogu.scm.review.config.service.ConfigService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryManager;

import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MergeOnlyChangeGuardTest {

  private static final Repository REPOSITORY = new Repository("1", "git", "space", "X");

  @Mock
  RepositoryManager repositoryManager;
  @Mock
  ConfigService configService;
  @InjectMocks
  MergeOnlyChangeGuard changeGuard;

  @Test
  void shouldProtectProtectedBranchesAndPath() {
    when(repositoryManager.get(REPOSITORY.getNamespaceAndName())).thenReturn(REPOSITORY);
    when(configService.isBranchPathProtected(REPOSITORY, "master", "src/main")).thenReturn(true);
    Changes changes = mock(Changes.class);
    when(changes.getFilesToCreate()).thenReturn(List.of("src/main"));
    Collection<ChangeObstacle> obstacles = changeGuard.getObstacles(REPOSITORY.getNamespaceAndName(), "master", changes);

    assertThat(obstacles).hasSize(1);
  }

  @Test
  void shouldNotCareForUnprotectedBranchesAndPath() {
    when(repositoryManager.get(REPOSITORY.getNamespaceAndName())).thenReturn(REPOSITORY);
    when(configService.isBranchPathProtected(REPOSITORY, "develop", "src/main")).thenReturn(false);
    Changes changes = mock(Changes.class);
    when(changes.getFilesToCreate()).thenReturn(List.of("src/main"));
    Collection<ChangeObstacle> obstacles = changeGuard.getObstacles(REPOSITORY.getNamespaceAndName(), "develop", changes);

    assertThat(obstacles).isEmpty();
  }
}
