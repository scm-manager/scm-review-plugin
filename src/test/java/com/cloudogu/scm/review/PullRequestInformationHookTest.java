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
import sonia.scm.repository.PostReceiveRepositoryHookEvent;
import sonia.scm.repository.Repository;
import sonia.scm.repository.api.Command;
import sonia.scm.repository.api.HookBranchProvider;
import sonia.scm.repository.api.HookContext;
import sonia.scm.repository.api.HookMessageProvider;
import sonia.scm.repository.api.RepositoryService;
import sonia.scm.repository.api.RepositoryServiceFactory;

import java.util.Collections;
import java.util.List;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SubjectAware(
  configuration = "classpath:com/cloudogu/scm/review/shiro.ini",
  password = "secret"
)
@RunWith(MockitoJUnitRunner.class)
public class PullRequestInformationHookTest {

  private static final Repository REPOSITORY = new Repository("1", "git", "space", "X");
  private static final PullRequest OPEN_PULL_REQUEST = createPullRequest("branch_X", PullRequestStatus.OPEN);
  private static final PullRequest MERGED_PULL_REQUEST = createPullRequest("branch_1", PullRequestStatus.MERGED);

  @Rule
  public ShiroRule shiroRule = new ShiroRule();

  @Mock
  private PullRequestService pullRequestService;
  @Mock
  private RepositoryServiceFactory serviceFactory;
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
  public void init() {
    hook = new PullRequestInformationHook(pullRequestService, serviceFactory, messageSenderFactory);
    when(event.getContext()).thenReturn(context);
    when(event.getRepository()).thenReturn(REPOSITORY);
    when(context.getBranchProvider()).thenReturn(branchProvider);
    when(context.getMessageProvider()).thenReturn(messageProvider);
    when(branchProvider.getCreatedOrModified()).thenReturn(Collections.emptyList());
    when(serviceFactory.create(REPOSITORY)).thenReturn(service);
    when(service.isSupported(Command.MERGE)).thenReturn(true);
    when(configuration.getBaseUrl()).thenReturn("http://example.com");
    when(pullRequestService.getAll("space", "X")).thenReturn(asList(OPEN_PULL_REQUEST, MERGED_PULL_REQUEST));
    doNothing().when(messageProvider).sendMessage(messageCaptor.capture());
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
    when(branchProvider.getCreatedOrModified()).thenReturn(asList("branch_X"));

    hook.checkForInformation(event);

    List<String> sentMessages = messageCaptor.getAllValues();
    assertThat(sentMessages)
      .filteredOn(s -> s.length() > 0)
      .hasSize(2)
      .anyMatch(s -> s.matches(".*pull request.*branch_X.*target.*"))
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
