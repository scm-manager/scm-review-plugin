package com.cloudogu.scm.review.workflow;

import com.cloudogu.scm.review.CurrentUserResolver;
import com.cloudogu.scm.review.TestData;
import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AutorCanNotMergeTest {

  static final String PRINCIPAL = "dent";

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  Subject subject;

  @BeforeEach
  void initSubject() {
    ThreadContext.bind(subject);
    when(subject.getPrincipals().getPrimaryPrincipal()).thenReturn(PRINCIPAL);
  }
  @Test
  void shouldReturnSuccessForOwnerasAuthor() {
    PullRequest pullRequest = TestData.createPullRequest();
    assertThat(pullRequest.getAuthor()).isEqualTo(CurrentUserResolver.getCurrentUserDisplayName());
  }
}
