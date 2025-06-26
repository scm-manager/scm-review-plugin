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

import com.cloudogu.scm.review.comment.service.BasicComment;
import com.cloudogu.scm.review.comment.service.Comment;
import com.cloudogu.scm.review.comment.service.Location;
import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import com.github.sdorra.shiro.ShiroRule;
import com.github.sdorra.shiro.SubjectAware;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Rule;
import org.junit.Test;
import sonia.scm.repository.Repository;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@SubjectAware(configuration = "classpath:com/cloudogu/scm/review/shiro.ini")
public class PermissionCheckTest {

  @Rule
  public ShiroRule shiroRule = new ShiroRule();

  @Test
  @SubjectAware(username = "trillian", password = "secret")
  public void shouldAllowModificationsForAuthor() {
    BasicComment comment = createComment("trillian");

    boolean modificationsAllowed = PermissionCheck.mayModifyComment(createRepository(), comment);

    assertTrue(modificationsAllowed);
  }

  @Test
  @SubjectAware(username = "trillian", password = "secret")
  public void shouldNotFailOnModificationsForAuthor() {
    BasicComment comment = createComment("trillian");

    PermissionCheck.checkModifyComment(createRepository(), comment);
  }

  @Test
  @SubjectAware(username = "slarti", password = "secret")
  public void shouldAllowModificationsForPushPermission() {
    BasicComment comment = createComment("author");

    boolean modificationsAllowed = PermissionCheck.mayModifyComment(createRepository(), comment);

    assertTrue(modificationsAllowed);
  }

  @Test
  @SubjectAware(username = "slarti", password = "secret")
  public void shouldNotFailOnModificationsForPushPermission() {
    BasicComment comment = createComment("trillian");

    PermissionCheck.checkModifyComment(createRepository(), comment);
  }

  @Test
  @SubjectAware(username = "trillian", password = "secret")
  public void shouldDisallowModificationsForNonAuthorWithoutPermission() {
    BasicComment comment = createComment("other");

    boolean modificationsAllowed = PermissionCheck.mayModifyComment(createRepository(), comment);

    assertFalse(modificationsAllowed);
  }

  @Test(expected = UnauthorizedException.class)
  @SubjectAware(username = "trillian", password = "secret")
  public void shouldFailOnModificationsForNonAuthorWithoutPermission() {
    BasicComment comment = createComment("other");

    PermissionCheck.checkModifyComment(createRepository(), comment);
  }

  private Comment createComment(String author) {
    return Comment.createComment( "1", "1. comment", author, new Location());
  }

  private Repository createRepository() {
    return new Repository("repo_ID", "git", "space", "X");
  }

  @Test
  @SubjectAware(username = "trillian", password = "secret")
  public void shouldAllowRejectForAuthor() {
    PullRequest pullRequest = TestData.createPullRequest();
    pullRequest.setAuthor("trillian");

    boolean modificationsAllowed = PermissionCheck.mayRejectPullRequest(pullRequest);

    assertTrue(modificationsAllowed);
  }

  @Test
  @SubjectAware(username = "trillian", password = "secret")
  public void shouldNotAllowRejectUserWithoutPermission() {
    PullRequest pullRequest = TestData.createPullRequest();

    boolean modificationsAllowed = PermissionCheck.mayRejectPullRequest(pullRequest);

    assertFalse(modificationsAllowed);
  }

  @Test
  @SubjectAware(username = "dent", password = "secret")
  public void shouldAllowRejectForUserWithAdminPermission() {
    PullRequest pullRequest = TestData.createPullRequest();

    boolean modificationsAllowed = PermissionCheck.mayRejectPullRequest(pullRequest);

    assertTrue(modificationsAllowed);
  }
}
