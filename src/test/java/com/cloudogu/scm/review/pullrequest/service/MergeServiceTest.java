package com.cloudogu.scm.review.pullrequest.service;

import com.cloudogu.scm.review.MergeStrategyNotSupportedException;
import com.github.sdorra.shiro.ShiroRule;
import com.github.sdorra.shiro.SubjectAware;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.junit.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryTestData;
import sonia.scm.repository.api.CommandNotSupportedException;
import sonia.scm.repository.api.MergeCommandBuilder;
import sonia.scm.repository.api.MergeCommandResult;
import sonia.scm.repository.api.MergeStrategy;
import sonia.scm.repository.api.RepositoryService;
import sonia.scm.repository.api.RepositoryServiceFactory;
import sonia.scm.user.User;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SubjectAware(
  configuration = "classpath:com/cloudogu/scm/review/shiro.ini",
  password = "secret"
)
@ExtendWith(MockitoExtension.class)
class MergeServiceTest {

  @Rule
  public ShiroRule shiroRule = new ShiroRule();
  private final Subject subject = mock(Subject.class);

  private final Repository REPOSITORY = RepositoryTestData.createHeartOfGold();

  @Mock
  private RepositoryServiceFactory serviceFactory;
  @Mock
  private RepositoryService repositoryService;
  @Mock
  private MergeCommandBuilder mergeCommandBuilder;

  private MergeService service;


  @BeforeEach
  void initService() {
    service = new MergeService(serviceFactory);
  }

  @BeforeEach
  void initMocks() {
    mockPrincipal();

    when(serviceFactory.create(REPOSITORY.getNamespaceAndName())).thenReturn(repositoryService);
    when(repositoryService.getRepository()).thenReturn(REPOSITORY);
    when(repositoryService.getMergeCommand()).thenReturn(mergeCommandBuilder);
  }

  @Test
  @SubjectAware(username = "trillian", password = "secret")
  void shouldMergeSuccessfully() {
    when(mergeCommandBuilder.isSupported(MergeStrategy.SQUASH)).thenReturn(true);
    when(mergeCommandBuilder.executeMerge()).thenReturn(MergeCommandResult.success());

    PullRequest pullRequest = new PullRequest();
    pullRequest.setSource("squash");
    pullRequest.setTarget("master");
    pullRequest.setAuthor("Philip J Fry");

    MergeCommandResult result = service.merge(REPOSITORY.getNamespaceAndName(), pullRequest, MergeStrategy.SQUASH);

    assertThat(result.isSuccess()).isTrue();
  }

  @Test
  @SubjectAware(username = "trillian", password = "secret")
  void shouldThrowExceptionIfStrategyNotSupported() {
    when(mergeCommandBuilder.isSupported(MergeStrategy.SQUASH)).thenReturn(false);

    PullRequest pullRequest = new PullRequest();
    pullRequest.setSource("squash");
    pullRequest.setTarget("master");
    pullRequest.setAuthor("Philip J Fry");

    assertThrows(MergeStrategyNotSupportedException.class, () -> service.merge(REPOSITORY.getNamespaceAndName(), pullRequest, MergeStrategy.SQUASH));
  }

  private void mockPrincipal() {
    ThreadContext.bind(subject);
    PrincipalCollection principals = mock(PrincipalCollection.class);
    when(subject.getPrincipals()).thenReturn(principals);
    User user1 = new User();
    user1.setName("Philip J Fry");
    user1.setDisplayName("Philip");
  }
}
