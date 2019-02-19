package com.cloudogu.scm.review;

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

  public static boolean mayModify(Repository repository) {
    return RepositoryPermissions.custom("modifyPullRequest", repository).isPermitted();
  }

  public static void checkModify(Repository repository) {
    RepositoryPermissions.custom("modifyPullRequest", repository).check();
  }

  public static boolean mayMerge(Repository repository) {
    return RepositoryPermissions.custom("mergePullRequest", repository).isPermitted();
  }

  public static void checkMerge(Repository repository) {
    RepositoryPermissions.custom("mergePullRequest", repository).check();
  }
}
