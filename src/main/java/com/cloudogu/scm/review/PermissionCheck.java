package com.cloudogu.scm.review;

import com.cloudogu.scm.review.comment.service.BasicComment;
import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.UnauthorizedException;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryPermissions;

public final class PermissionCheck {

  public static final String CREATE_PULL_REQUEST = "createPullRequest";
  public static final String MODIFY_PULL_REQUEST = "modifyPullRequest";
  public static final String READ_PULL_REQUEST = "readPullRequest";
  public static final String COMMENT_PULL_REQUEST = "commentPullRequest";
  public static final String MERGE_PULL_REQUEST = "mergePullRequest";
  public static final String CONFIGURE_PULL_REQUEST = "configurePullRequest";

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

  /**
   * A User can modify a comment if he is the author or he has a modify permission
   *
   *  @return true if the user can update/delete a comment
   */
  public static boolean mayModifyComment(Repository repository, BasicComment requestComment ) {
    return currentUserIsAuthor(requestComment.getAuthor()) || mayModify(repository);
  }

  public static void checkModifyComment(Repository repository, BasicComment requestComment ) {
    if (currentUserIsAuthor(requestComment.getAuthor())) {
      return;
    }
    checkModify(repository);
  }

  /**
   * A User can modify a pull request if he is the author or he has a modify permission
   *
   *  @return true if the user can update/delete a comment
   */
  public static boolean mayModifyPullRequest(Repository repository, PullRequest request) {

    return currentUserIsAuthor(request.getAuthor()) || mayModify(repository);
  }

  private static boolean currentUserIsAuthor(String author) {
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

  public static void checkConfigure(Repository repository) {
    RepositoryPermissions.custom(CONFIGURE_PULL_REQUEST, repository).check();
  }
}
