/*
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
import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.UnauthorizedException;
import sonia.scm.config.ConfigurationPermissions;
import sonia.scm.repository.NamespacePermissions;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryPermissions;

import java.util.Objects;

public final class PermissionCheck {

  public static final String CREATE_PULL_REQUEST = "createPullRequest";
  public static final String MODIFY_PULL_REQUEST = "modifyPullRequest";
  public static final String READ_PULL_REQUEST = "readPullRequest";
  public static final String COMMENT_PULL_REQUEST = "commentPullRequest";
  public static final String MERGE_PULL_REQUEST = "mergePullRequest";
  public static final String PERFORM_EMERGENCY_MERGE = "performEmergencyMerge";
  public static final String CONFIGURE_PULL_REQUEST = "configurePullRequest";
  public static final String CONFIGURE_PERMISSION = "pullRequest";
  public static final String WORKFLOW_CONFIG = "workflowConfig";
  public static final String READ_WORKFLOW_CONFIG = "readWorkflowConfig";
  public static final String WRITE_WORKFLOW_CONFIG = "writeWorkflowConfig";

  private PermissionCheck() {
  }

  public static boolean mayCreate(Repository repository) {
    return RepositoryPermissions.custom(CREATE_PULL_REQUEST, repository).isPermitted();
  }

  public static void checkCreate(Repository repository) {
    RepositoryPermissions.custom(CREATE_PULL_REQUEST, repository).check();
  }

  public static boolean mayRead(Repository repository) {
    return RepositoryPermissions.custom(READ_PULL_REQUEST, repository).isPermitted() ||
      RepositoryPermissions.custom(CREATE_PULL_REQUEST, repository).isPermitted() ||
      RepositoryPermissions.custom(MODIFY_PULL_REQUEST, repository).isPermitted();
  }

  public static void checkRead(Repository repository) {
    if (!mayRead(repository)) {
      String msg = "User is not permitted to read pull requests";
      throw new UnauthorizedException(msg);
    }
  }

  public static boolean mayComment(Repository repository) {
    return RepositoryPermissions.custom(COMMENT_PULL_REQUEST, repository).isPermitted();
  }

  public static void checkComment(Repository repository) {
    RepositoryPermissions.custom(COMMENT_PULL_REQUEST, repository).check();
  }

  public static boolean mayMerge(Repository repository) {
    return RepositoryPermissions.custom(MERGE_PULL_REQUEST, repository).isPermitted();
  }

  public static void checkMerge(Repository repository) {
    RepositoryPermissions.custom(MERGE_PULL_REQUEST, repository).check();
  }

  public static void checkEmergencyMerge(Repository repository) {
    RepositoryPermissions.custom(PERFORM_EMERGENCY_MERGE, repository).check();
  }

  public static boolean mayPerformEmergencyMerge(Repository repository) {
    return RepositoryPermissions.custom(PERFORM_EMERGENCY_MERGE, repository).isPermitted();
  }

  /**
   * A User can modify a comment if he is the author or he has a modify permission
   *
   * @return true if the user can update/delete a comment
   */
  public static boolean mayModifyComment(Repository repository, BasicComment requestComment) {
    return currentUserIsAuthor(requestComment.getAuthor()) || mayModify(repository);
  }

  public static void checkModifyComment(Repository repository, BasicComment requestComment) {
    if (currentUserIsAuthor(requestComment.getAuthor())) {
      return;
    }
    checkModify(repository);
  }

  /**
   * A User can modify a pull request if he is the author or he has a modify permission
   *
   * @return true if the user can update/delete a comment
   */
  public static boolean mayModifyPullRequest(Repository repository, PullRequest request) {

    return currentUserIsAuthor(request.getAuthor()) || mayModify(repository);
  }

  private static boolean currentUserIsAuthor(String author) {
    if (Objects.isNull(SecurityUtils.getSubject().getPrincipals().getPrimaryPrincipal())) {
      return false;
    }
    return author.equals(SecurityUtils.getSubject().getPrincipals().getPrimaryPrincipal().toString());
  }

  private static boolean mayModify(Repository repository) {
    return RepositoryPermissions.custom(MODIFY_PULL_REQUEST, repository).isPermitted();
  }

  private static void checkModify(Repository repository) {
    RepositoryPermissions.custom(MODIFY_PULL_REQUEST, repository).check();
  }

  public static boolean mayConfigure(Repository repository) {
    return RepositoryPermissions.custom(CONFIGURE_PULL_REQUEST, repository).isPermitted();
  }

  /**
   * A User can reject a pull request if he is the author or he has a reject permission
   */
  public static boolean mayRejectPullRequest(PullRequest request) {
    return currentUserIsAuthor(request.getAuthor());
  }

  public static void checkReject(Repository repository, PullRequest request) {
    if (!mayRejectPullRequest(request)) {
      checkMerge(repository);
    }
  }

  public static boolean mayConfigure(String namespace) {
    return NamespacePermissions.custom(CONFIGURE_PULL_REQUEST, namespace).isPermitted();
  }

  public static void checkModify(String namespace) {
    NamespacePermissions.custom(CONFIGURE_PULL_REQUEST, namespace).check();
  }

  public static boolean mayReadGlobalConfig() {
    return ConfigurationPermissions.read(CONFIGURE_PERMISSION).isPermitted();
  }

  public static boolean mayWriteGlobalConfig() {
    return ConfigurationPermissions.write(CONFIGURE_PERMISSION).isPermitted();
  }

  public static void checkConfigure(Repository repository) {
    RepositoryPermissions.custom(CONFIGURE_PULL_REQUEST, repository).check();
  }

  public static void checkReadGlobalConfig() {
    ConfigurationPermissions.read(CONFIGURE_PERMISSION).check();
  }

  public static void checkWriteGlobalConfig() {
    ConfigurationPermissions.write(CONFIGURE_PERMISSION).check();
  }

  public static void checkReadWorkflowConfig(Repository repository) {
    RepositoryPermissions.custom(READ_WORKFLOW_CONFIG, repository).check();
  }

  public static void checkWriteWorkflowConfig(Repository repository) {
    RepositoryPermissions.custom(WRITE_WORKFLOW_CONFIG, repository).check();
  }

  public static boolean mayReadWorkflowConfig(Repository repository) {
    return RepositoryPermissions.custom(READ_WORKFLOW_CONFIG, repository).isPermitted();
  }

  public static boolean mayConfigureWorkflowConfig(Repository repository) {
    return RepositoryPermissions.custom(WRITE_WORKFLOW_CONFIG, repository).isPermitted();
  }

  public static boolean mayReadGlobalWorkflowConfig() {
    return ConfigurationPermissions.read(WORKFLOW_CONFIG).isPermitted();
  }

  public static boolean mayConfigureGlobalWorkflowConfig() {
    return ConfigurationPermissions.write(WORKFLOW_CONFIG).isPermitted();
  }

  public static void checkReadWorkflowEngineGlobalConfig() {
    ConfigurationPermissions.read(WORKFLOW_CONFIG).check();
  }

  public static void checkWriteWorkflowEngineGlobalConfig() {
    ConfigurationPermissions.write(WORKFLOW_CONFIG).check();
  }
}
