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
  private static final Branch EXISTING_BRANCH = new Branch("branch", "1");

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
    when(branches.getBranches()).thenReturn(singletonList(new Branch("branch", "1")));

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
