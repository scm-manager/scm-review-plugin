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

import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import com.cloudogu.scm.review.pullrequest.service.PullRequestService;
import com.cloudogu.scm.review.pullrequest.service.PullRequestStatus;
import com.github.sdorra.shiro.ShiroRule;
import com.github.sdorra.shiro.SubjectAware;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import sonia.scm.config.ScmConfiguration;
import sonia.scm.repository.Branch;
import sonia.scm.repository.Branches;
import sonia.scm.repository.PostReceiveRepositoryHookEvent;
import sonia.scm.repository.Repository;
import sonia.scm.repository.api.BranchesCommandBuilder;
import sonia.scm.repository.api.Command;
import sonia.scm.repository.api.HookBranchProvider;
import sonia.scm.repository.api.HookContext;
import sonia.scm.repository.api.HookMessageProvider;
import sonia.scm.repository.api.RepositoryService;
import sonia.scm.repository.api.RepositoryServiceFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static sonia.scm.repository.api.HookFeature.MESSAGE_PROVIDER;

@SubjectAware(
  configuration = "classpath:com/cloudogu/scm/review/shiro.ini",
  password = "secret"
)
@RunWith(MockitoJUnitRunner.class)
public class PullRequestInformationHookTest {

  private static final Repository REPOSITORY = new Repository("1", "git", "space", "X");
  private static final PullRequest OPEN_PULL_REQUEST = createPullRequest("branch_X", PullRequestStatus.OPEN);
  private static final PullRequest DRAFT_PULL_REQUEST = createPullRequest("branch_Y", PullRequestStatus.DRAFT);
  private static final PullRequest MERGED_PULL_REQUEST = createPullRequest("branch_1", PullRequestStatus.MERGED);

  @Rule
  public ShiroRule shiroRule = new ShiroRule();

  @Mock
  private PullRequestService pullRequestService;
  @Mock
  private RepositoryServiceFactory serviceFactory;
  @Mock
  private BranchesCommandBuilder branchesCommand;
  @Mock
  private RepositoryService service;
  @Mock
  private ScmConfiguration configuration;
  @Mock
  private PostReceiveRepositoryHookEvent event;
  @Mock
  private HookContext context;
  @Mock
  private HookBranchProvider branchProvider;
  @Mock
  private HookMessageProvider messageProvider;

  @Captor
  private ArgumentCaptor<String> messageCaptor;

  @InjectMocks
  private MessageSenderFactory messageSenderFactory;

  private PullRequestInformationHook hook;

  @Before
  public void init() throws IOException {
    hook = new PullRequestInformationHook(pullRequestService, serviceFactory, messageSenderFactory);
    when(event.getContext()).thenReturn(context);
    when(event.getRepository()).thenReturn(REPOSITORY);
    when(context.getBranchProvider()).thenReturn(branchProvider);
    when(context.getMessageProvider()).thenReturn(messageProvider);
    when(context.isFeatureSupported(MESSAGE_PROVIDER)).thenReturn(true);
    when(branchProvider.getCreatedOrModified()).thenReturn(Collections.emptyList());
    when(serviceFactory.create(REPOSITORY)).thenReturn(service);
    when(service.isSupported(Command.MERGE)).thenReturn(true);
    when(configuration.getBaseUrl()).thenReturn("http://example.com");
    when(pullRequestService.getAll("space", "X")).thenReturn(asList(OPEN_PULL_REQUEST, MERGED_PULL_REQUEST, DRAFT_PULL_REQUEST));
    doNothing().when(messageProvider).sendMessage(messageCaptor.capture());
    when(service.getBranchesCommand()).thenReturn(branchesCommand);
    Branches branches = new Branches(Branch.defaultBranch("main", "", 0L), Branch.normalBranch("x", "", 0L));
    when(branchesCommand.getBranches()).thenReturn(branches);
  }

  @Test
  @SubjectAware(username = "rr")
  public void shouldDoNothingWhenMergeIsNotSupported() {
    when(service.isSupported(Command.MERGE)).thenReturn(false);

    hook.checkForInformation(event);

    verify(branchProvider, never()).getCreatedOrModified();
    verify(messageProvider, never()).sendMessage(anyString());
  }

  @Test
  @SubjectAware(username = "trillian")
  public void shouldDoNothingWhenUserHasNoPermissions() {
    hook.checkForInformation(event);

    verify(serviceFactory, never()).create(any(Repository.class));
  }

  @Test
  @SubjectAware(username = "dent")
  public void shouldSendMessageWithoutCreateLinks() throws Exception {
    when(branchProvider.getCreatedOrModified()).thenReturn(singletonList("branch_1"));
    when(service.getBranchesCommand()).thenReturn(branchesCommand);
    Branches branches = new Branches(Branch.defaultBranch("main", "", 0L));
    when(branchesCommand.getBranches()).thenReturn(branches);

    hook.checkForInformation(event);

    List<String> sentMessages = messageCaptor.getAllValues();
    assertThat(sentMessages)
      .filteredOn(s -> s.length() > 0)
      .isEmpty();
  }

  @Test
  @SubjectAware(username = "dent")
  public void shouldSendMessageWithCreateLinks() {
    when(branchProvider.getCreatedOrModified()).thenReturn(asList("branch_1", "branch_2"));

    hook.checkForInformation(event);

    List<String> sentMessages = messageCaptor.getAllValues();
    assertThat(sentMessages)
      .filteredOn(s -> s.length() > 0)
      .hasSize(4)
      .anyMatch(s -> s.matches(".*new pull request.*branch_1.*"))
      .anyMatch(s -> s.matches(".*new pull request.*branch_2.*"))
      .anyMatch(s -> s.matches("http://example.com/.*branch_1.*"))
      .anyMatch(s -> s.matches("http://example.com/.*branch_2.*"));
  }

  @Test
  @SubjectAware(username = "rr")
  public void shouldNotSendMessageWithCreateLinksWhenUserHasNoPermissions() {
    when(branchProvider.getCreatedOrModified()).thenReturn(asList("branch_1", "branch_2"));

    hook.checkForInformation(event);

    List<String> sentMessages = messageCaptor.getAllValues();
    assertThat(sentMessages).isEmpty();
  }

  @Test
  @SubjectAware(username = "rr")
  public void shouldSendMessageWithLinksForExistingPR() {
    when(branchProvider.getCreatedOrModified()).thenReturn(singletonList("branch_X"));

    hook.checkForInformation(event);

    List<String> sentMessages = messageCaptor.getAllValues();
    assertThat(sentMessages)
      .filteredOn(s -> s.length() > 0)
      .hasSize(2)
      .anyMatch(s -> s.matches(".*pull request.*branch_X.*target.*"))
      .anyMatch(s -> s.matches("http://example.com/.*pr_id.*"));
  }

  @Test
  @SubjectAware(username = "rr")
  public void shouldSendMessageWithLinksForExistingPRWithStatusDraft() {
    when(branchProvider.getCreatedOrModified()).thenReturn(singletonList("branch_Y"));

    hook.checkForInformation(event);

    List<String> sentMessages = messageCaptor.getAllValues();
    assertThat(sentMessages)
      .filteredOn(s -> s.length() > 0)
      .hasSize(2)
      .anyMatch(s -> s.matches(".*pull request.*branch_Y.*target.*"))
      .anyMatch(s -> s.matches("http://example.com/.*pr_id.*"));
  }

  private static PullRequest createPullRequest(String name, PullRequestStatus status) {
    PullRequest existingPullRequest = new PullRequest();
    existingPullRequest.setId("pr_id");
    existingPullRequest.setSource(name);
    existingPullRequest.setTarget("target");
    existingPullRequest.setStatus(status);
    return existingPullRequest;
  }
}
