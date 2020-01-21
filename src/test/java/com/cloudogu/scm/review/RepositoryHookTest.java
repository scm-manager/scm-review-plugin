package com.cloudogu.scm.review;

import com.cloudogu.scm.review.config.service.ConfigService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.repository.PreReceiveRepositoryHookEvent;
import sonia.scm.repository.Repository;
import sonia.scm.repository.api.HookBranchProvider;
import sonia.scm.repository.api.HookContext;
import sonia.scm.repository.api.HookFeature;

import static java.util.Arrays.asList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RepositoryHookTest {

  private static final Repository REPOSITORY = new Repository("1", "git", "space", "X");

  @Mock
  ConfigService configService;
  @Mock
  PreReceiveRepositoryHookEvent event;

  @Test
  void shouldNotFailOnEmptyContext() {
    new RepositoryHook(configService).onEvent(event);
  }

  @Nested
  class WithNormalContext {
    @Mock
    HookContext hookContext;
    @Mock
    HookBranchProvider branchProvider;

    @BeforeEach
    void setContext() {
      when(event.getContext()).thenReturn(hookContext);
      when(hookContext.isFeatureSupported(HookFeature.BRANCH_PROVIDER)).thenReturn(true);
      when(event.getRepository()).thenReturn(REPOSITORY);
      lenient().when(hookContext.getBranchProvider()).thenReturn(branchProvider);
    }

    @Test
    void shouldIgnoreDisabledConfig() {
      when(configService.isEnabled(REPOSITORY)).thenReturn(false);

      new RepositoryHook(configService).onEvent(event);
    }

    @Test
    void shouldIgnoreNotProtectedBranch() {
      when(configService.isEnabled(REPOSITORY)).thenReturn(true);
      when(configService.isBranchProtected(REPOSITORY, "feature")).thenReturn(false);

      when(branchProvider.getCreatedOrModified()).thenReturn(asList("feature"));

      new RepositoryHook(configService).onEvent(event);
    }

    @Test
    void shouldFailForProtectedBranch() {
      when(configService.isEnabled(REPOSITORY)).thenReturn(true);
      when(configService.isBranchProtected(REPOSITORY, "master")).thenReturn(true);

      when(branchProvider.getCreatedOrModified()).thenReturn(asList("master"));

      Assertions.assertThrows(BranchOnlyWritableByMergeException.class, () ->
        new RepositoryHook(configService).onEvent(event)
      );
    }
  }
}
