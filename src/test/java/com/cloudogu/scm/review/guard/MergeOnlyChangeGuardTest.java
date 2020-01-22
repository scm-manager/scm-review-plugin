package com.cloudogu.scm.review.guard;

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

import static org.assertj.core.api.Assertions.assertThat;
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
  void shouldProtectProtectedBranches() {
    when(repositoryManager.get(REPOSITORY.getNamespaceAndName())).thenReturn(REPOSITORY);
    when(configService.isBranchProtected(REPOSITORY, "master")).thenReturn(true);

    Collection<ChangeObstacle> obstacles = changeGuard.getObstacles(REPOSITORY.getNamespaceAndName(), "master", null);

    assertThat(obstacles).hasSize(1);
  }

  @Test
  void shouldNotCareForUnprotectedBranches() {
    when(repositoryManager.get(REPOSITORY.getNamespaceAndName())).thenReturn(REPOSITORY);
    when(configService.isBranchProtected(REPOSITORY, "develop")).thenReturn(false);

    Collection<ChangeObstacle> obstacles = changeGuard.getObstacles(REPOSITORY.getNamespaceAndName(), "develop", null);

    assertThat(obstacles).isEmpty();
  }
}
