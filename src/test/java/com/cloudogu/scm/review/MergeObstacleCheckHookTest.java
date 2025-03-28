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

package com.cloudogu.scm.review;

import com.cloudogu.scm.review.pullrequest.service.DefaultPullRequestService;
import com.cloudogu.scm.review.pullrequest.service.MergeNotAllowedException;
import com.cloudogu.scm.review.pullrequest.service.MergeObstacle;
import com.cloudogu.scm.review.pullrequest.service.MergeService;
import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import com.cloudogu.scm.review.pullrequest.service.PullRequestStatus;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import sonia.scm.config.ScmConfiguration;
import sonia.scm.repository.PreReceiveRepositoryHookEvent;
import sonia.scm.repository.Repository;
import sonia.scm.repository.api.HookBranchProvider;
import sonia.scm.repository.api.HookContext;
import sonia.scm.repository.api.HookMessageProvider;
import sonia.scm.repository.spi.HookMergeDetectionProvider;

import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static sonia.scm.repository.api.HookFeature.BRANCH_PROVIDER;
import static sonia.scm.repository.api.HookFeature.MERGE_DETECTION_PROVIDER;
import static sonia.scm.repository.api.HookFeature.MESSAGE_PROVIDER;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MergeObstacleCheckHookTest {

  private static final String NAMESPACE = "space";
  private static final String NAME = "name";
  private static final Repository REPOSITORY = new Repository("id", "git", NAMESPACE, NAME);

  @Mock
  private DefaultPullRequestService pullRequestService;
  @Mock
  private MergeService mergeService;

  @Mock
  private ScmConfiguration configuration;
  @InjectMocks
  private MessageSenderFactory messageSenderFactory;

  private MergeObstacleCheckHook hook;

  @Mock
  private PreReceiveRepositoryHookEvent event;
  @Mock
  private HookContext hookContext;
  @Mock
  private HookMergeDetectionProvider mergeDetectionProvider;
  @Mock
  private HookMessageProvider messageProvider;
  @Mock
  private HookBranchProvider branchProvider;
  @Mock
  private InternalMergeSwitch internalMergeSwitch;

  @Mock
  private Subject subject;

  @BeforeEach
  void initBasics() {
    hook = new MergeObstacleCheckHook(pullRequestService, mergeService, messageSenderFactory, internalMergeSwitch);
    when(pullRequestService.supportsPullRequests(REPOSITORY)).thenReturn(true);
    when(configuration.getBaseUrl()).thenReturn("http://example.com/");
    when(hookContext.isFeatureSupported(MERGE_DETECTION_PROVIDER)).thenReturn(true);
    when(hookContext.getMergeDetectionProvider()).thenReturn(mergeDetectionProvider);
    when(hookContext.isFeatureSupported(BRANCH_PROVIDER)).thenReturn(true);
    when(hookContext.getBranchProvider()).thenReturn(branchProvider);
    when(hookContext.isFeatureSupported(MESSAGE_PROVIDER)).thenReturn(true);
    when(hookContext.getMessageProvider()).thenReturn(messageProvider);
  }

  @BeforeEach
  void initEvent() {
    when(event.getContext()).thenReturn(hookContext);
  }

  @BeforeEach
  void initRepository() {
    when(event.getRepository()).thenReturn(REPOSITORY);
  }

  @BeforeEach
  void mockSubject() {
    ThreadContext.bind(subject);
  }

  @AfterEach
  void cleanupSubject() {
    ThreadContext.unbindSubject();
  }

  @Nested
  class WithOpenPullRequest {

    PullRequest pullRequest;

    @BeforeEach
    void mockChangedBranch() {
      when(branchProvider.getCreatedOrModified()).thenReturn(singletonList("target"));
    }

    @BeforeEach
    void mockPullRequest() {
      pullRequest = new PullRequest("pr", "source", "target");
      pullRequest.setStatus(PullRequestStatus.OPEN);
      when(pullRequestService.getAll(NAMESPACE, NAME)).thenReturn(singletonList(pullRequest));

      doReturn(singletonList(new TestObstacle()))
        .when(mergeService).verifyNoObstacles(true, REPOSITORY, pullRequest);
      doThrow(new MergeNotAllowedException(REPOSITORY, pullRequest, singletonList(new TestObstacle())))
        .when(mergeService).verifyNoObstacles(false, REPOSITORY, pullRequest);
    }

    @Nested
    class WithEmergencyPermission {

      @BeforeEach
      void mockPermission() {
        when(subject.isPermitted("repository:performEmergencyMerge:id")).thenReturn(true);
      }

      @Test
      void shouldPassByMergeOfPullRequests() {
        when(mergeDetectionProvider.branchesMerged("target", "source")).thenReturn(true);

        hook.checkForObstacles(event);

        // nothing more expected to happen
      }

      @Test
      void shouldSendMessageToUser() {
        when(mergeDetectionProvider.branchesMerged("target", "source")).thenReturn(true);

        hook.checkForObstacles(event);

        verify(messageProvider, atLeastOnce()).sendMessage(any());
      }

      @Test
      void shouldNotCheckForInternalMerge() {
        when(internalMergeSwitch.internalMergeRunning()).thenReturn(true);

        hook.checkForObstacles(event);

        verify(mergeDetectionProvider, never()).branchesMerged(any(), any());
        verify(messageProvider, never()).sendMessage(any());
      }
    }

    @Nested
    class WithoutEmergencyPermission {

      @BeforeEach
      void mockPermission() {
        when(subject.isPermitted("repository:performEmergencyMerge:id")).thenReturn(false);
      }

      @Test
      void shouldRejectToMergePullRequestsWithObstacles() {
        when(mergeDetectionProvider.branchesMerged("target", "source")).thenReturn(true);

        Assertions.assertThrows(MergeNotAllowedException.class, () -> hook.checkForObstacles(event));
      }


      @Test
      void shouldSendMessageToUser() {
        when(mergeDetectionProvider.branchesMerged("target", "source")).thenReturn(true);

        try {
          hook.checkForObstacles(event);
        } catch (Exception e) {
          // exception is of no interest in this test
        }

        verify(messageProvider, atLeastOnce()).sendMessage(any());
      }
    }

    @Test
    void shouldIgnoreChangeOfIrrelevantBranch() {
      when(branchProvider.getCreatedOrModified()).thenReturn(singletonList("other"));

      hook.checkForObstacles(event);

      verify(mergeDetectionProvider, never()).branchesMerged(any(), any());
    }
  }

  @Test
  void shouldIgnoreClosedPullRequest() {
    PullRequest pullRequest = new PullRequest("pr", "source", "target");
    pullRequest.setStatus(PullRequestStatus.REJECTED);
    when(pullRequestService.getAll(NAMESPACE, NAME)).thenReturn(singletonList(pullRequest));

    when(branchProvider.getCreatedOrModified()).thenReturn(singletonList("source"));

    hook.checkForObstacles(event);

    verify(branchProvider, never()).getCreatedOrModified();
    verify(mergeDetectionProvider, never()).branchesMerged(any(), any());
  }

  private static class TestObstacle implements MergeObstacle {
    @Override
    public String getMessage() {
      return null;
    }

    @Override
    public String getKey() {
      return null;
    }
  }
}
