package com.cloudogu.scm.review;

import com.cloudogu.scm.review.pullrequest.service.DefaultPullRequestService;
import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import com.cloudogu.scm.review.pullrequest.service.PullRequestStatus;
import org.assertj.core.api.Assertions;
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
import sonia.scm.repository.api.HookContext;
import sonia.scm.repository.api.HookMessageProvider;
import sonia.scm.repository.api.LogCommandBuilder;
import sonia.scm.repository.api.RepositoryService;
import sonia.scm.repository.api.RepositoryServiceFactory;

import java.io.IOException;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MergeCheckHookTest {

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
  private MessageSender messageSender;

  private MergeCheckHook hook;

  @Mock
  private PostReceiveRepositoryHookEvent event;
  @Mock
  private HookContext context;
  @Mock
  private HookMessageProvider messageProvider;
  @Captor
  private ArgumentCaptor<String> messageCaptor;

  @BeforeEach
  void initRepositoryServiceFactory() {
    hook = new MergeCheckHook(service, repositoryServiceFactory, messageSender);
    when(repositoryServiceFactory.create(REPOSITORY)).thenReturn(repositoryService);
    when(repositoryService.getLogCommand()).thenReturn(logCommandBuilder);
    when(repositoryService.isSupported(Command.MERGE)).thenReturn(true);
    when(configuration.getBaseUrl()).thenReturn("http://example.com/");
    when(event.getContext()).thenReturn(context);
    when(context.getMessageProvider()).thenReturn(messageProvider);
    doNothing().when(messageProvider).sendMessage(messageCaptor.capture());
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

    hook.checkForMerges(event);

    verify(service).setStatus(REPOSITORY, pullRequest, PullRequestStatus.MERGED);
    assertThat(messageCaptor.getAllValues()).isNotEmpty();
  }

  @Test
  void shouldLeaveUnmergedPullRequestOpen() throws IOException {
    PullRequest pullRequest = openPullRequest();
    when(service.getAll(NAMESPACE, NAME)).thenReturn(singletonList(pullRequest));
    when(logCommandBuilder.getChangesets()).thenReturn(new ChangesetPagingResult(1, singletonList(new Changeset())));

    hook.checkForMerges(event);

    verify(service, never()).setStatus(REPOSITORY, pullRequest, PullRequestStatus.MERGED);
  }

  @Test
  void shouldLeaveRejectedPullRequestRejected() throws IOException {
    PullRequest pullRequest = rejectedPullRequest();
    when(service.getAll(NAMESPACE, NAME)).thenReturn(singletonList(pullRequest));

    hook.checkForMerges(event);

    verify(logCommandBuilder, never()).getChangesets();
    verify(service, never()).setStatus(REPOSITORY, pullRequest, PullRequestStatus.MERGED);
  }

  @Test
  void shouldNotProcessEventsForRepositoriesWithoutMergeCapability() throws IOException {
    when(repositoryService.isSupported(Command.MERGE)).thenReturn(false);
    PullRequest pullRequest = rejectedPullRequest();

    hook.checkForMerges(event);

    verify(logCommandBuilder, never()).getChangesets();
    verify(service, never()).getAll(NAMESPACE, NAME);
    verify(service, never()).setStatus(REPOSITORY, pullRequest, PullRequestStatus.MERGED);
  }

  private PullRequest openPullRequest() {
    PullRequest pullRequest = new PullRequest();
    pullRequest.setStatus(PullRequestStatus.OPEN);
    return pullRequest;
  }

  private PullRequest rejectedPullRequest() {
    PullRequest pullRequest = new PullRequest();
    pullRequest.setStatus(PullRequestStatus.REJECTED);
    return pullRequest;
  }
}
