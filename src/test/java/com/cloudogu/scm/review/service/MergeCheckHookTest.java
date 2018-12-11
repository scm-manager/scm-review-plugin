package com.cloudogu.scm.review.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import sonia.scm.repository.Changeset;
import sonia.scm.repository.ChangesetPagingResult;
import sonia.scm.repository.PostReceiveRepositoryHookEvent;
import sonia.scm.repository.Repository;
import sonia.scm.repository.api.LogCommandBuilder;
import sonia.scm.repository.api.RepositoryService;
import sonia.scm.repository.api.RepositoryServiceFactory;

import java.io.IOException;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
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
  @InjectMocks
  private MergeCheckHook hook;

  @Mock
  private PostReceiveRepositoryHookEvent event;

  @BeforeEach
  void initRepositoryServiceFactory() {
    when(repositoryServiceFactory.create(REPOSITORY)).thenReturn(repositoryService);
    when(repositoryService.getLogCommand()).thenReturn(logCommandBuilder);
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
