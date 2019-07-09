package com.cloudogu.scm.review.comment.api;

import com.cloudogu.scm.review.comment.service.BasicComment;
import com.cloudogu.scm.review.comment.service.ExecutedTransition;
import com.cloudogu.scm.review.pullrequest.dto.DisplayedUserDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import sonia.scm.api.v2.resources.InstantAttributeMapper;
import sonia.scm.user.UserDisplayManager;

import javax.inject.Inject;
import javax.inject.Named;

import static java.util.stream.Collectors.toList;

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

  void appendTransitions(@MappingTarget BasicCommentDto target, BasicComment source) {
    target.withEmbedded(
      "transitions",
      source
        .getExecutedTransitions()
        .stream()
        .map(this::map)
        .collect(toList())
    );
  }
}
