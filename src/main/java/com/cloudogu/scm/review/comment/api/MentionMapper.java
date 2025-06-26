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

package com.cloudogu.scm.review.comment.api;

import com.cloudogu.scm.review.comment.service.BasicComment;
import jakarta.inject.Inject;
import sonia.scm.user.DisplayUser;
import sonia.scm.user.UserDisplayManager;

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
