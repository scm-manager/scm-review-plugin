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
