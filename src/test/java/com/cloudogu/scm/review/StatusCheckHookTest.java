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

import com.cloudogu.scm.review.pullrequest.service.DefaultPullRequestService;
import com.cloudogu.scm.review.pullrequest.service.MergeObstacle;
import com.cloudogu.scm.review.pullrequest.service.MergeService;
import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import com.cloudogu.scm.review.pullrequest.service.PullRequestRejectedEvent;
import com.cloudogu.scm.review.pullrequest.service.PullRequestStatus;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import sonia.scm.config.ScmConfiguration;
import sonia.scm.repository.PostReceiveRepositoryHookEvent;
import sonia.scm.repository.Repository;
import sonia.scm.repository.api.HookBranchProvider;
import sonia.scm.repository.api.HookContext;
import sonia.scm.repository.api.HookMessageProvider;
import sonia.scm.repository.spi.HookMergeDetectionProvider;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static sonia.scm.repository.api.HookFeature.BRANCH_PROVIDER;
import static sonia.scm.repository.api.HookFeature.MERGE_DETECTION_PROVIDER;
import static sonia.scm.repository.api.HookFeature.MESSAGE_PROVIDER;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StatusCheckHookTest {

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

  private StatusCheckHook hook;

  @Mock
  private PostReceiveRepositoryHookEvent event;
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
    hook = new StatusCheckHook(pullRequestService, messageSenderFactory, mergeService, internalMergeSwitch);
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
    when(branchProvider.getCreatedOrModified()).thenReturn(singletonList("target"));
  }

  @BeforeEach
  void initRepository() {
    when(event.getRepository()).thenReturn(REPOSITORY);
  }

  @BeforeEach
  void bindSubject() {
    ThreadContext.bind(subject);
  }

  @AfterEach
  void unbindSubject() {
    ThreadContext.unbindSubject();
  }

  @Test
  void shouldSetMergedPullRequestToMerged() {
    PullRequest pullRequest = mockOpenPullRequest();
    when(mergeDetectionProvider.branchesMerged("target", "source")).thenReturn(true);

    hook.checkStatus(event);

    verify(pullRequestService).setMerged(REPOSITORY, pullRequest.getId());
  }

  @Test
  void shouldSetMergedPullRequestToMergedEvenWhenSourceBranchHasBeenChanged() {
    when(branchProvider.getCreatedOrModified()).thenReturn(singletonList("source"));

    PullRequest pullRequest = mockOpenPullRequest();
    when(mergeDetectionProvider.branchesMerged("target", "source")).thenReturn(true);

    hook.checkStatus(event);

    verify(pullRequestService).setMerged(REPOSITORY, pullRequest.getId());
  }

  @Test
  void shouldLeaveUnmergedPullRequestOpen() {
    mockOpenPullRequest();
    when(mergeDetectionProvider.branchesMerged("target", "source")).thenReturn(false);

    hook.checkStatus(event);

    verify(pullRequestService, never()).setMerged(any(), anyString());
  }

  @Test
  void shouldNotifyServiceThatPrWasUpdated() {
    PullRequest pullRequest = mockOpenPullRequest();
    when(mergeDetectionProvider.branchesMerged("target", "source")).thenReturn(false);

    hook.checkStatus(event);

    verify(pullRequestService).updated(REPOSITORY, pullRequest.getId());
  }

  @Test
  void shouldLeaveRejectedPullRequestRejected() {
    PullRequest pullRequest = mockRejectedPullRequest();

    hook.checkStatus(event);

    verify(mergeDetectionProvider, never()).branchesMerged(any(), any());
    verify(pullRequestService, never()).reject(eq(REPOSITORY), eq(pullRequest.getId()), any());
  }

  @Test
  void shouldNotProcessEventsForRepositoriesWithoutMergeCapability() {
    when(pullRequestService.supportsPullRequests(REPOSITORY)).thenReturn(false);

    hook.checkStatus(event);

    verify(mergeDetectionProvider, never()).branchesMerged(any(), any());
    verify(pullRequestService, never()).getAll(anyString(), anyString());
    verify(pullRequestService, never()).setMerged(any(), anyString());
  }

  @Test
  void shouldNotCheckNotEffectedPullRequests() {
    PullRequest pullRequest = mockOpenPullRequest();
    pullRequest.setTarget("other");

    hook.checkStatus(event);

    verify(mergeDetectionProvider, never()).branchesMerged(any(), any());
  }

  @Test
  void shouldSetPullRequestsWithDeletedSourceToRejected() {
    PullRequest pullRequest = mockOpenPullRequest();

    when(branchProvider.getCreatedOrModified()).thenReturn(emptyList());
    when(branchProvider.getDeletedOrClosed()).thenReturn(singletonList("source"));

    hook.checkStatus(event);

    verify(pullRequestService).setRejected(REPOSITORY, pullRequest.getId(), PullRequestRejectedEvent.RejectionCause.BRANCH_DELETED);
  }

  @Test
  void shouldSetMergedPullRequestToMergedWithEmergencyMergeAndObstacles() {
    PullRequest pullRequest = mockOpenPullRequest();
    when(mergeDetectionProvider.branchesMerged("target", "source")).thenReturn(true);
    when(subject.isPermitted("repository:performEmergencyMerge:id")).thenReturn(true);
    when(mergeService.verifyNoObstacles(true, REPOSITORY, pullRequest))
      .thenReturn(singletonList(new TestObstacle()));

    hook.checkStatus(event);

    verify(pullRequestService)
      .setEmergencyMerged(REPOSITORY, pullRequest.getId(), "merged by a direct push to the repository", singletonList("TEST_OBSTACLE"));
  }

  @Test
  void shouldIgnoreHookWhenInternalMerge() {
    when(internalMergeSwitch.internalMergeRunning()).thenReturn(true);
    when(mergeDetectionProvider.branchesMerged("target", "source")).thenReturn(true);

    hook.checkStatus(event);

    verify(pullRequestService, never()).setEmergencyMerged(any(), any(), any(), any());
    verify(pullRequestService, never()).setMerged(any(), any());
  }

  private PullRequest mockOpenPullRequest() {
    PullRequest pullRequest = new PullRequest();
    pullRequest.setStatus(PullRequestStatus.OPEN);
    pullRequest.setSource("source");
    pullRequest.setTarget("target");
    when(pullRequestService.getAll(NAMESPACE, NAME)).thenReturn(singletonList(pullRequest));
    return pullRequest;
  }

  private PullRequest mockRejectedPullRequest() {
    PullRequest pullRequest = new PullRequest();
    pullRequest.setStatus(PullRequestStatus.REJECTED);
    when(pullRequestService.getAll(NAMESPACE, NAME)).thenReturn(singletonList(pullRequest));
    return pullRequest;
  }

  private static class TestObstacle implements MergeObstacle {
    @Override
    public String getMessage() {
      return null;
    }

    @Override
    public String getKey() {
      return "TEST_OBSTACLE";
    }
  }
}
