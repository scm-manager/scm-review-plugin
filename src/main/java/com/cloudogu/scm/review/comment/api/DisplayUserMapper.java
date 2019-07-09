package com.cloudogu.scm.review.comment.api;

import com.cloudogu.scm.review.pullrequest.dto.DisplayedUserDto;
import sonia.scm.user.DisplayUser;
import sonia.scm.user.UserDisplayManager;

class DisplayUserMapper {

  private final UserDisplayManager userDisplayManager;

  DisplayUserMapper(UserDisplayManager userDisplayManager) {
    this.userDisplayManager = userDisplayManager;
  }

  DisplayedUserDto map(String userId) {
    return userDisplayManager.get(userId).map(this::createDisplayedUserDto).orElse(new DisplayedUserDto(userId, userId));
  }

  private DisplayedUserDto createDisplayedUserDto(DisplayUser user) {
    return new DisplayedUserDto(user.getId(), user.getDisplayName());
  }
}
