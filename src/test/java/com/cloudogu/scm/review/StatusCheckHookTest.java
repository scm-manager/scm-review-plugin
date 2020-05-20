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
import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import com.cloudogu.scm.review.pullrequest.service.PullRequestRejectedEvent;
import com.cloudogu.scm.review.pullrequest.service.PullRequestStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import sonia.scm.config.ScmConfiguration;
import sonia.scm.repository.Changeset;
import sonia.scm.repository.ChangesetPagingResult;
import sonia.scm.repository.PostReceiveRepositoryHookEvent;
import sonia.scm.repository.Repository;
import sonia.scm.repository.api.Command;
import sonia.scm.repository.api.HookBranchProvider;
import sonia.scm.repository.api.HookContext;
import sonia.scm.repository.api.HookMessageProvider;
import sonia.scm.repository.api.LogCommandBuilder;
import sonia.scm.repository.api.RepositoryService;
import sonia.scm.repository.api.RepositoryServiceFactory;

import java.io.IOException;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static sonia.scm.repository.api.HookFeature.MESSAGE_PROVIDER;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StatusCheckHookTest {

  private static final String NAMESPACE = "space";
  private static final String NAME = "name";
  private static final Repository REPOSITORY = new Repository("id", "git", NAMESPACE, NAME);

  @Mock
  private DefaultPullRequestService service;

  @Mock
  private RepositoryServiceFactory repositoryServiceFactory;

  @Mock
  private RepositoryService repositoryService;
  @Mock(answer = Answers.RETURNS_SELF)
  private LogCommandBuilder logCommandBuilder;

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
  private HookMessageProvider messageProvider;
  @Mock
  private HookBranchProvider branchProvider;
  @Captor
  private ArgumentCaptor<String> messageCaptor;

  @BeforeEach
  void initRepositoryServiceFactory() {
    hook = new StatusCheckHook(service, repositoryServiceFactory, messageSenderFactory);
    when(repositoryServiceFactory.create(REPOSITORY)).thenReturn(repositoryService);
    when(repositoryService.getRepository()).thenReturn(REPOSITORY);
    when(repositoryService.getLogCommand()).thenReturn(logCommandBuilder);
    when(service.supportsPullRequests(REPOSITORY)).thenReturn(true);
    when(configuration.getBaseUrl()).thenReturn("http://example.com/");
    when(event.getContext()).thenReturn(hookContext);
    when(hookContext.isFeatureSupported(MESSAGE_PROVIDER)).thenReturn(true);
    when(hookContext.getMessageProvider()).thenReturn(messageProvider);
    doNothing().when(messageProvider).sendMessage(messageCaptor.capture());
  }

  @BeforeEach
  void initEvent() {
    when(event.getContext()).thenReturn(hookContext);
    when(hookContext.getBranchProvider()).thenReturn(branchProvider);
    when(branchProvider.getCreatedOrModified()).thenReturn(singletonList("source"));
  }

  @BeforeEach
  void initRepository() {
    when(event.getRepository()).thenReturn(REPOSITORY);
  }

  @Test
  void shouldSetMergedPullRequestToMerged() throws IOException {
    PullRequest pullRequest = openPullRequest();
    when(service.getAll(NAMESPACE, NAME)).thenReturn(singletonList(pullRequest));
    when(logCommandBuilder.getChangesets()).thenReturn(new ChangesetPagingResult(0, emptyList()));

    hook.checkStatus(event);

    verify(service).setMerged(REPOSITORY, pullRequest.getId(), null);
  }

  @Test
  void shouldLeaveUnmergedPullRequestOpen() throws IOException {
    PullRequest pullRequest = openPullRequest();
    when(service.getAll(NAMESPACE, NAME)).thenReturn(singletonList(pullRequest));
    when(logCommandBuilder.getChangesets()).thenReturn(new ChangesetPagingResult(1, singletonList(new Changeset())));

    hook.checkStatus(event);

    verify(service, never()).setMerged(any(), anyString(), anyString());
  }

  @Test
  void shouldNotifyServiceThatPrWasUpdated() throws IOException {
    PullRequest pullRequest = openPullRequest();
    when(service.getAll(NAMESPACE, NAME)).thenReturn(singletonList(pullRequest));
    when(logCommandBuilder.getChangesets()).thenReturn(new ChangesetPagingResult(1, singletonList(new Changeset())));

    hook.checkStatus(event);

    verify(service).updated(REPOSITORY, pullRequest.getId());
  }

  @Test
  void shouldLeaveRejectedPullRequestRejected() throws IOException {
    PullRequest pullRequest = rejectedPullRequest();
    when(service.getAll(NAMESPACE, NAME)).thenReturn(singletonList(pullRequest));

    hook.checkStatus(event);

    verify(logCommandBuilder, never()).getChangesets();
    verify(service, never()).reject(eq(REPOSITORY), eq(pullRequest.getId()), any());
  }

  @Test
  void shouldNotProcessEventsForRepositoriesWithoutMergeCapability() throws IOException {
    when(service.supportsPullRequests(REPOSITORY)).thenReturn(false);

    hook.checkStatus(event);

    verify(logCommandBuilder, never()).getChangesets();
    verify(service, never()).getAll(anyString(), anyString());
    verify(service, never()).setMerged(any(), anyString(), anyString());
  }

  @Test
  void shouldNotCheckNotEffectedPullRequests() throws IOException {
    PullRequest pullRequest = openPullRequest();
    pullRequest.setSource("other");
    when(service.getAll(NAMESPACE, NAME)).thenReturn(singletonList(pullRequest));

    hook.checkStatus(event);

    verify(logCommandBuilder, never()).getChangesets();
  }

  @Test
  void shouldSetPullRequestsWithDeletedSourceToRejected() {
    PullRequest pullRequest = openPullRequest();
    when(service.getAll(NAMESPACE, NAME)).thenReturn(singletonList(pullRequest));

    when(branchProvider.getCreatedOrModified()).thenReturn(emptyList());
    when(branchProvider.getDeletedOrClosed()).thenReturn(singletonList("source"));

    hook.checkStatus(event);

    verify(service).setRejected(REPOSITORY, pullRequest.getId(), PullRequestRejectedEvent.RejectionCause.BRANCH_DELETED);
  }

  private PullRequest openPullRequest() {
    PullRequest pullRequest = new PullRequest();
    pullRequest.setStatus(PullRequestStatus.OPEN);
    pullRequest.setSource("source");
    pullRequest.setTarget("target");
    return pullRequest;
  }

  private PullRequest rejectedPullRequest() {
    PullRequest pullRequest = new PullRequest();
    pullRequest.setStatus(PullRequestStatus.REJECTED);
    return pullRequest;
  }
}
