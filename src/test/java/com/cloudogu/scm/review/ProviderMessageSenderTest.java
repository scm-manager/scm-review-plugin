package com.cloudogu.scm.review;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.config.ScmConfiguration;
import sonia.scm.repository.RepositoryHookEvent;
import sonia.scm.repository.api.HookMessageProvider;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProviderMessageSenderTest {

  @Mock
  private ScmConfiguration scmConfiguration;
  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private RepositoryHookEvent event;
  @Mock
  private HookMessageProvider messageProvider;

  @InjectMocks
  private ProviderMessageSender sender;

  @BeforeEach
  void setUpMessageProvider() {
    when(event.getContext().getMessageProvider())
      .thenReturn(messageProvider);
  }

  @BeforeEach
  void mockBaseUrl() {
    when(scmConfiguration.getBaseUrl()).thenReturn("https://hog/scm");
  }

  @Test
  void shouldEncodeBranchInUrl() {
    sender.sendCreatePullRequestMessage("nice+branch");

    verify(messageProvider).sendMessage("https://hog/scm/repo/null/null/pull-requests/add/changesets/?source=nice%2Bbranch");
  }
}
