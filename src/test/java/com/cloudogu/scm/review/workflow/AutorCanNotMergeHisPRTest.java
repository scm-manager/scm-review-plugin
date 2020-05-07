package com.cloudogu.scm.review.workflow;

import com.cloudogu.scm.review.CurrentUserResolver;
import com.cloudogu.scm.review.TestData;
import com.cloudogu.scm.review.pullrequest.service.MergeObstacle;
import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import org.apache.commons.lang.Validate;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.session.Session;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.repository.NamespaceAndName;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryManager;
import sonia.scm.repository.RepositoryTestData;
import sonia.scm.user.User;

import java.util.Collection;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.synchronizedSortedMap;
import static junit.framework.TestCase.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AutorCanNotMergeHisPRTest {

  @Mock
  Engine engine;

  @InjectMocks
  WorkflowMergeGuard guard;

  @Mock
  private RepositoryManager repositoryManager;


  @InjectMocks
  private CurrentUserResolver currentUserResolver;


  private static final Repository REPOSITORY = new Repository("1", "git", "space", "X");
  private static final PullRequest PULL_REQUEST = new PullRequest("1-1", "feature", "develop");
  //private static final CurrentUserResolver CURRENT_USER_RESOLVER  = new CurrentUserResolver();
  private AutorCanNotMergeHisPR rule = new AutorCanNotMergeHisPR();

  @BeforeEach
  void bindSubject() {
    ThreadContext.bind(mock(Subject.class));
  }

  @AfterEach
  void unbindSubject() {
    ThreadContext.unbindSubject();
  }

  @Test
  void shouldReturnSuccessForOwnerasAuthor() {
    PullRequest pullRequest = TestData.createPullRequest();
pullRequest.setAuthor("SCM Administrator");

    assertEquals(pullRequest.getAuthor(),"SCM Administrator");
  }


  @Test
  void shouldReturnSuccessForOwnerasAuthorwithValidate() {
    PullRequest pullRequest = TestData.createPullRequest();
    pullRequest.setAuthor("SCM Administrator");
    Validate.isTrue(pullRequest.getAuthor().equals("SCM Administrator"));
  }







}
