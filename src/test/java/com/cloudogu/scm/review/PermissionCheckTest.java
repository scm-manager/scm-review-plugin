package com.cloudogu.scm.review;

import com.cloudogu.scm.review.comment.service.Location;
import com.cloudogu.scm.review.comment.service.PullRequestComment;
import com.google.common.collect.Lists;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.junit.Before;
import org.junit.Test;
import sonia.scm.repository.Repository;
import sonia.scm.user.User;

import java.time.Instant;
import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PermissionCheckTest {

  private final Subject subject = mock(Subject.class);

  @Before
  public void init() {
    ThreadContext.bind(subject);
  }

  @Test
  public void shouldAllowModificationsForAuthor() {
    PrincipalCollection principals = mock(PrincipalCollection.class);
    when(subject.getPrincipals()).thenReturn(principals);
    String currentUser = "author";
    when(principals.getPrimaryPrincipal()).thenReturn(currentUser);
    User user1 = new User();
    user1.setAdmin(false);
    List<Object> userList = Lists.newArrayList("user", user1);
    when(principals.asList()).thenReturn(userList);

    PullRequestComment comment = new PullRequestComment("1", "1. comment", "author", new Location(), Instant.now());
    boolean modificationsAllowed = PermissionCheck.mayModifyComment(new Repository(), comment);

    assertTrue(modificationsAllowed);
  }

  @Test
  public void shouldAllowModificationsForPushPermission() {
    PrincipalCollection principals = mock(PrincipalCollection.class);
    when(subject.getPrincipals()).thenReturn(principals);
    when(subject.isPermitted(anyString())).thenReturn(true);
    String currentUser = "author_1";
    when(principals.getPrimaryPrincipal()).thenReturn(currentUser);
    User user1 = new User();
    user1.setAdmin(false);
    List<Object> userList = Lists.newArrayList("user", user1);
    when(principals.asList()).thenReturn(userList);

    PullRequestComment comment = new PullRequestComment("1", "1. comment", "author", new Location(), Instant.now());
    boolean modificationsAllowed = PermissionCheck.mayModifyComment(new Repository("", "", "", ""), comment);

    assertTrue(modificationsAllowed);
  }
}
