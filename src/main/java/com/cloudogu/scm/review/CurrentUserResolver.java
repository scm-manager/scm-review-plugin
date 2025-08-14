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
