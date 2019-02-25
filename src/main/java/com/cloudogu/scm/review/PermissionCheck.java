package com.cloudogu.scm.review;

import com.cloudogu.scm.review.comment.service.PullRequestComment;
import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import org.apache.shiro.SecurityUtils;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryPermissions;

public final class PermissionCheck {
  private PermissionCheck() {
  }

  public static boolean mayCreate(Repository repository) {
    return RepositoryPermissions.custom("createPullRequest", repository).isPermitted();
  }

  public static void checkCreate(Repository repository) {
    RepositoryPermissions.custom("createPullRequest", repository).check();
  }

  public static boolean mayRead(Repository repository) {
    return RepositoryPermissions.custom("readPullRequest", repository).isPermitted();
  }

  public static void checkRead(Repository repository) {
    RepositoryPermissions.custom("readPullRequest", repository).check();
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
    return RepositoryPermissions.custom("modifyPullRequest", repository).isPermitted();
  }
}
