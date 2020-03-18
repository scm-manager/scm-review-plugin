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
