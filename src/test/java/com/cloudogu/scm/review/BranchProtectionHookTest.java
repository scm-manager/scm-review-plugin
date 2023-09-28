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

import com.cloudogu.scm.review.config.service.ConfigService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.repository.Added;
import sonia.scm.repository.Changeset;
import sonia.scm.repository.Modification;
import sonia.scm.repository.Modifications;
import sonia.scm.repository.PreReceiveRepositoryHookEvent;
import sonia.scm.repository.Repository;
import sonia.scm.repository.api.HookBranchProvider;
import sonia.scm.repository.api.HookChangesetBuilder;
import sonia.scm.repository.api.HookContext;
import sonia.scm.repository.api.HookFeature;
import sonia.scm.repository.api.ModificationsCommandBuilder;
import sonia.scm.repository.api.RepositoryService;
import sonia.scm.repository.api.RepositoryServiceFactory;

import java.io.IOException;
import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("java:S2699") // We have some tests that should just assert that no exceptions are throws, so we have no asserts
class BranchProtectionHookTest {

  private static final Repository REPOSITORY = new Repository("1", "git", "space", "X");

  @Mock
  ConfigService configService;
  @Mock
  PreReceiveRepositoryHookEvent event;
  @Mock
  InternalMergeSwitch internalMergeSwitch;
  @Mock
  RepositoryServiceFactory serviceFactory;
  @Mock
  RepositoryService repositoryService;
  @Mock(answer = Answers.RETURNS_SELF)
  ModificationsCommandBuilder modificationsCommandBuilder;
  @InjectMocks
  BranchProtectionHook hook;

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
      when(event.getRepository()).thenReturn(REPOSITORY);
      lenient().when(hookContext.isFeatureSupported(HookFeature.BRANCH_PROVIDER)).thenReturn(true);
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

      when(branchProvider.getCreatedOrModified()).thenReturn(singletonList("feature"));

      hook.onEvent(event);
    }

    @Test
    void shouldIgnoreBranchesWithoutChangesets() {
      when(configService.isEnabled(REPOSITORY)).thenReturn(true);
      when(configService.isBranchProtected(REPOSITORY, "master")).thenReturn(true);

      when(branchProvider.getCreatedOrModified()).thenReturn(singletonList("master"));
      when(changesetBuilder.getChangesets()).thenReturn(emptyList());

      hook.onEvent(event);
    }

    @Test
    void shouldFailForProtectedBranchWithChangeset() throws IOException {
      when(serviceFactory.create(REPOSITORY)).thenReturn(repositoryService);
      when(repositoryService.getModificationsCommand()).thenReturn(modificationsCommandBuilder);
      when(modificationsCommandBuilder.getModifications()).thenReturn(new Modifications("123", List.of(new Added("forbidden"))));
      when(configService.isEnabled(REPOSITORY)).thenReturn(true);
      when(configService.isBranchProtected(REPOSITORY, "master")).thenReturn(true);
      when(configService.isBranchPathProtected(REPOSITORY, "master", "forbidden")).thenReturn(true);

      when(branchProvider.getCreatedOrModified()).thenReturn(singletonList("master"));
      Changeset changeset = mock(Changeset.class);
      when(changesetBuilder.getChangesets()).thenReturn(singletonList(changeset));
      when(changeset.getBranches()).thenReturn(singletonList("master"));
      when(changeset.getParents()).thenReturn(singletonList("parent"));

      assertThrows(BranchOnlyWritableByMergeException.class, () ->
        hook.onEvent(event)
      );
    }

    @Test
    void shouldNotFailForProtectedBranchWithChangesetAndNotProtectedPath() throws IOException {
      when(serviceFactory.create(REPOSITORY)).thenReturn(repositoryService);
      when(repositoryService.getModificationsCommand()).thenReturn(modificationsCommandBuilder);
      when(modificationsCommandBuilder.getModifications()).thenReturn(new Modifications("123", List.of(new Added("allowed"))));
      when(configService.isEnabled(REPOSITORY)).thenReturn(true);
      when(configService.isBranchProtected(REPOSITORY, "master")).thenReturn(true);
      when(configService.isBranchPathProtected(REPOSITORY, "master", "allowed")).thenReturn(false);

      when(branchProvider.getCreatedOrModified()).thenReturn(singletonList("master"));
      Changeset changeset = mock(Changeset.class);
      when(changesetBuilder.getChangesets()).thenReturn(singletonList(changeset));
      when(changeset.getBranches()).thenReturn(singletonList("master"));
      when(changeset.getParents()).thenReturn(singletonList("parent"));

      assertDoesNotThrow(() -> hook.onEvent(event));
    }

    @Test
    void shouldNotFailForNotProtectedBranchButProtectedPath() {
      when(configService.isEnabled(REPOSITORY)).thenReturn(true);
      when(configService.isBranchProtected(REPOSITORY, "master")).thenReturn(false);

      when(branchProvider.getCreatedOrModified()).thenReturn(singletonList("master"));

      assertDoesNotThrow(() -> hook.onEvent(event));
    }

    @Test
    void shouldIgnoreChecksWhenRunPrivileged() {
      when(internalMergeSwitch.internalMergeRunning()).thenReturn(true);

      hook.onEvent(event);

      verify(configService, never()).isEnabled(any());
    }
  }
}
