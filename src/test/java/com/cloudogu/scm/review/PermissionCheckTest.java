/**
 * MIT License
 *
 * Copyright (c) 2020-present Cloudogu GmbH and Contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.cloudogu.scm.review;

import com.cloudogu.scm.review.comment.service.BasicComment;
import com.cloudogu.scm.review.comment.service.Comment;
import com.cloudogu.scm.review.comment.service.Location;
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
}
