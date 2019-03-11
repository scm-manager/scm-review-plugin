package com.cloudogu.scm.review;

import com.cloudogu.scm.review.comment.service.PullRequestComment;
import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.UnauthorizedException;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryPermissions;

public final class PermissionCheck {

  public static final String CREATE_PULL_REQUEST = "createPullRequest";
  public static final String MODIFY_PULL_REQUEST = "modifyPullRequest";
  public static final String READ_PULL_REQUEST = "readPullRequest";

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
    return RepositoryPermissions.custom("commentPullRequest", repository).isPermitted();
  }

  public static void checkComment(Repository repository) {
    RepositoryPermissions.custom("commentPullRequest", repository).check();
  }

  public static boolean mayMerge(Repository repository) {
    return RepositoryPermissions.custom("mergePullRequest", repository).isPermitted();
  }

  public static void checkMerge(Repository repository) {
    RepositoryPermissions.custom("mergePullRequest", repository).check();
  }

  /**
   * A User can modify a comment if he is the author or he has a modify permission
   *
   *  @return true if the user can update/delete a comment
   */
  public static boolean mayModifyComment(Repository repository, PullRequestComment requestComment ) {
    String currentUser = SecurityUtils.getSubject().getPrincipals().getPrimaryPrincipal().toString();

    return currentUser.equals(requestComment.getAuthor()) || mayModify(repository);
  }

  /**
   * A User can modify a pull request if he is the author or he has a modify permission
   *
   *  @return true if the user can update/delete a comment
   */
  public static boolean mayModifyPullRequest(Repository repository, PullRequest request) {
    String currentUser = SecurityUtils.getSubject().getPrincipals().getPrimaryPrincipal().toString();

    return currentUser.equals(request.getAuthor()) || mayModify(repository);
  }

  private static boolean mayModify(Repository repository) {
    return RepositoryPermissions.custom(MODIFY_PULL_REQUEST, repository).isPermitted();
  }
}
