package com.cloudogu.scm.review;

import com.cloudogu.scm.review.config.service.ConfigService;
import junit.framework.AssertionFailedError;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.repository.Changeset;
import sonia.scm.repository.PreReceiveRepositoryHookEvent;
import sonia.scm.repository.Repository;
import sonia.scm.repository.api.HookBranchProvider;
import sonia.scm.repository.api.HookChangesetBuilder;
import sonia.scm.repository.api.HookContext;
import sonia.scm.repository.api.HookFeature;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RepositoryHookTest {

  private static final Repository REPOSITORY = new Repository("1", "git", "space", "X");

  @Mock
  ConfigService configService;
  @Mock
  PreReceiveRepositoryHookEvent event;
  @InjectMocks
  RepositoryHook hook;

  @Test
  void shouldNotFailOnEmptyContext() {
    hook.onEvent(event);
  }

  @Nested
  class WithNormalContext {
    @Mock
    HookContext hookContext;
    @Mock
    HookBranchProvider branchProvider;
    @Mock
    HookChangesetBuilder changesetBuilder;

    @BeforeEach
    void setContext() {
      when(event.getContext()).thenReturn(hookContext);
      when(hookContext.isFeatureSupported(HookFeature.BRANCH_PROVIDER)).thenReturn(true);
      when(event.getRepository()).thenReturn(REPOSITORY);
      lenient().when(hookContext.getBranchProvider()).thenReturn(branchProvider);
      lenient().when(hookContext.getChangesetProvider()).thenReturn(changesetBuilder);
    }

    @Test
    void shouldIgnoreDisabledConfig() {
      when(configService.isEnabled(REPOSITORY)).thenReturn(false);

      hook.onEvent(event);
    }

    @Test
    void shouldIgnoreNotProtectedBranch() {
      when(configService.isEnabled(REPOSITORY)).thenReturn(true);
      when(configService.isBranchProtected(REPOSITORY, "feature")).thenReturn(false);

      when(branchProvider.getCreatedOrModified()).thenReturn(asList("feature"));

      hook.onEvent(event);
    }

    @Test
    void shouldIgnoreBranchesWithoutChangesets() {
      when(configService.isEnabled(REPOSITORY)).thenReturn(true);
      when(configService.isBranchProtected(REPOSITORY, "master")).thenReturn(true);

      when(branchProvider.getCreatedOrModified()).thenReturn(asList("master"));
      when(changesetBuilder.getChangesets()).thenReturn(emptyList());

      hook.onEvent(event);
    }

    @Test
    void shouldFailForProtectedBranchWithChangeset() {
      when(configService.isEnabled(REPOSITORY)).thenReturn(true);
      when(configService.isBranchProtected(REPOSITORY, "master")).thenReturn(true);

      when(branchProvider.getCreatedOrModified()).thenReturn(asList("master"));
      Changeset changeset = mock(Changeset.class);
      when(changesetBuilder.getChangesets()).thenReturn(asList(changeset));
      when(changeset.getBranches()).thenReturn(asList("master"));
      when(changeset.getParents()).thenReturn(asList("parent"));

      Assertions.assertThrows(BranchOnlyWritableByMergeException.class, () ->
        hook.onEvent(event)
      );
    }
  }

  @Test
  void shouldIgnoreChecksWhenRunPrivileged() {
    lenient().when(event.getContext()).thenThrow(new AssertionFailedError("this should not have been called"));
    hook.runPrivileged(() -> hook.onEvent(event));
  }
}
