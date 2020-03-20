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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.NotFoundException;
import sonia.scm.repository.Branch;
import sonia.scm.repository.Branches;
import sonia.scm.repository.Repository;
import sonia.scm.repository.api.BranchesCommandBuilder;
import sonia.scm.repository.api.RepositoryService;
import sonia.scm.repository.api.RepositoryServiceFactory;

import java.io.IOException;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BranchResolverTest {

  private static final Repository REPOSITORY = new Repository("id", "git", "Space", "X");
  private static final Branch EXISTING_BRANCH = Branch.normalBranch("branch", "1");

  @Mock
  private RepositoryServiceFactory repositoryServiceFactory;
  @Mock
  private RepositoryService repositoryService;
  @Mock
  private BranchesCommandBuilder branchesCommandBuilder;
  @Mock
  private Branches branches;
  private BranchResolver branchResolver;

  @BeforeEach
  void init() throws IOException {
    when(repositoryServiceFactory.create(REPOSITORY)).thenReturn(repositoryService);
    when(repositoryService.getBranchesCommand()).thenReturn(branchesCommandBuilder);
    when(branchesCommandBuilder.getBranches()).thenReturn(branches);
    when(branches.getBranches()).thenReturn(singletonList(Branch.normalBranch("branch", "1")));

    branchResolver = new BranchResolver(repositoryServiceFactory);
  }

  @Test
  void shouldThrowNotFoundExceptionWhenBranchNotFound() {
    assertThrows(NotFoundException.class,
      () -> branchResolver.resolve(REPOSITORY, "other"));
  }

  @Test
  void shouldFindExistingBranch() {
    BranchResolver branchResolver = new BranchResolver(repositoryServiceFactory);

    Branch branch = branchResolver.resolve(REPOSITORY, "branch");

    assertThat(branch).isEqualTo(EXISTING_BRANCH);
  }
}
