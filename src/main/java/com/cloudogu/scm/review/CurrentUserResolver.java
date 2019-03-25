package com.cloudogu.scm.review;

import com.google.common.base.Strings;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.PrincipalCollection;
import sonia.scm.user.User;

public class CurrentUserResolver {

  private CurrentUserResolver() {
  }

  public static String getCurrentUserDisplayName() {
    PrincipalCollection principals = SecurityUtils.getSubject().getPrincipals();
    String displayName = principals.getPrimaryPrincipal().toString();
    User user = principals.oneByType(User.class);
    if (user != null) {
      displayName = user.getDisplayName();
      if (Strings.isNullOrEmpty(displayName)) {
        displayName = user.getName();
      }
    }
    return displayName;
  }

  public static User getCurrentUser() {
    PrincipalCollection principals = SecurityUtils.getSubject().getPrincipals();
    return principals.oneByType(User.class);
  }

}
