package com.cloudogu.scm.review.comment.api;

import com.cloudogu.scm.review.comment.service.ExecutedTransition;
import com.cloudogu.scm.review.pullrequest.dto.DisplayedUserDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import sonia.scm.api.v2.resources.InstantAttributeMapper;
import sonia.scm.user.UserDisplayManager;

import javax.inject.Inject;
import javax.inject.Named;

@Mapper
public abstract class ExecutedTransitionMapper implements InstantAttributeMapper {

  @Inject
  private UserDisplayManager userDisplayManager;

  @Mapping(target = "user", source = "user", qualifiedByName = "mapUser")
  abstract ExecutedTransitionDto map(ExecutedTransition transition);

  @Named("mapUser")
  DisplayedUserDto mapUser(String userId) {
    return new DisplayUserMapper(userDisplayManager).map(userId);
  }
}
