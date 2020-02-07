package com.cloudogu.scm.review.comment.api;

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

  Set<Mention> mapMentions(Set<String> userIds) {
    Set<Mention> mentions = new HashSet<>();
    if (userIds != null) {
      for (String id : userIds) {
        Optional<DisplayUser> displayUser = userDisplayManager.get(id);
        displayUser.ifPresent(user -> mentions.add(new Mention(user.getId(), user.getDisplayName())));

      }
    }
    return mentions;
  }

  Set<String> extractMentionsFromComment(String comment) {
    Set<String> mentions = new HashSet<>();
    Matcher matcher = mentionPattern.matcher(comment);

    while (matcher.find()) {
      for (int j = 0; j <= matcher.groupCount(); j++) {
        String matchingId = matcher.group(1);
        Optional<DisplayUser> displayUser = userDisplayManager.get(matchingId);
        displayUser.ifPresent(user -> mentions.add(user.getId()));
      }
    }
    return mentions;
  }
}
