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
package com.cloudogu.scm.review.comment.api;

import com.cloudogu.scm.review.comment.service.BasicComment;
import sonia.scm.user.DisplayUser;
import sonia.scm.user.UserDisplayManager;

import javax.inject.Inject;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MentionMapper {

  private UserDisplayManager userDisplayManager;

  private static final Pattern mentionPattern =
    Pattern.compile("@\\[([A-Za-z0-9\\.\\-_][A-Za-z0-9\\.\\-_@]*)]");

  @Inject
  public MentionMapper(UserDisplayManager userDisplayManager) {
    this.userDisplayManager = userDisplayManager;
  }

  Set<DisplayUser> mapMentions(Set<String> userIds) {
    Set<DisplayUser> mentions = new HashSet<>();
    if (userIds != null) {
      for (String id : userIds) {
        Optional<DisplayUser> displayUser = userDisplayManager.get(id);
        displayUser.ifPresent(mentions::add);
      }
    }
    return mentions;
  }

  public Set<String> extractMentionsFromComment(String comment) {
    Set<String> mentions = new HashSet<>();
    Matcher matcher = mentionPattern.matcher(comment);

    while (matcher.find()) {
      String matchingId = matcher.group(1);
      Optional<DisplayUser> displayUser = userDisplayManager.get(matchingId);
      displayUser.ifPresent(user -> mentions.add(user.getId()));
    }
    return mentions;
  }

  public BasicComment parseMentionsUserIdsToDisplayNames(BasicComment rootComment) {
    BasicComment comment = rootComment.clone();
    for (String mentionUserId : rootComment.getMentionUserIds()) {
      Optional<DisplayUser> user = userDisplayManager.get(mentionUserId);
      user.ifPresent(displayUser -> comment.setComment(comment.getComment().replaceAll("\\[" + mentionUserId + "]", displayUser.getDisplayName())));
    }
    return comment;
  }
}
