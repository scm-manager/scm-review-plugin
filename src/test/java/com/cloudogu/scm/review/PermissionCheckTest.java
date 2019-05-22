package com.cloudogu.scm.review;

import com.cloudogu.scm.review.comment.service.Location;
import com.cloudogu.scm.review.comment.service.PullRequestComment;
import com.github.sdorra.shiro.ShiroRule;
import com.github.sdorra.shiro.SubjectAware;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Rule;
import org.junit.Test;
import sonia.scm.repository.Repository;

import java.time.Instant;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@SubjectAware(configuration = "classpath:com/cloudogu/scm/review/shiro.ini")
public class PermissionCheckTest {

  @Rule
  public ShiroRule shiroRule = new ShiroRule();

  @Test
  @SubjectAware(username = "trillian", password = "secret")
  public void shouldAllowModificationsForAuthor() {
    PullRequestComment comment = createComment("trillian");

    boolean modificationsAllowed = PermissionCheck.mayModifyComment(createRepository(), comment);

    assertTrue(modificationsAllowed);
  }

  @Test
  @SubjectAware(username = "trillian", password = "secret")
  public void shouldNotFailOnModificationsForAuthor() {
    PullRequestComment comment = createComment("trillian");

    PermissionCheck.checkModifyComment(createRepository(), comment);
  }

  @Test
  @SubjectAware(username = "slarti", password = "secret")
  public void shouldAllowModificationsForPushPermission() {
    PullRequestComment comment = createComment("author");

    boolean modificationsAllowed = PermissionCheck.mayModifyComment(createRepository(), comment);

    assertTrue(modificationsAllowed);
  }

  @Test
  @SubjectAware(username = "slarti", password = "secret")
  public void shouldNotFailOnModificationsForPushPermission() {
    PullRequestComment comment = createComment("trillian");

    PermissionCheck.checkModifyComment(createRepository(), comment);
  }

  @Test
  @SubjectAware(username = "trillian", password = "secret")
  public void shouldDisallowModificationsForNonAuthorWithoutPermission() {
    PullRequestComment comment = createComment("other");

    boolean modificationsAllowed = PermissionCheck.mayModifyComment(createRepository(), comment);

    assertFalse(modificationsAllowed);
  }

  @Test(expected = UnauthorizedException.class)
  @SubjectAware(username = "trillian", password = "secret")
  public void shouldFailOnModificationsForNonAuthorWithoutPermission() {
    PullRequestComment comment = createComment("other");

    PermissionCheck.checkModifyComment(createRepository(), comment);
  }

  private PullRequestComment createComment(String author) {
    return new PullRequestComment("123", "1", "1. comment", author, new Location(), Instant.now(), false, false);
  }

  private Repository createRepository() {
    return new Repository("repo_ID", "git", "space", "X");
  }
}
