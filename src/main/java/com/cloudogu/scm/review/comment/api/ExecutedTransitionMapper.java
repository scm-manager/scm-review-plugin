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
import com.cloudogu.scm.review.comment.service.ExecutedTransition;
import com.cloudogu.scm.review.comment.service.Transition;
import com.cloudogu.scm.review.pullrequest.dto.DisplayedUserDto;
import de.otto.edison.hal.Links;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ObjectFactory;
import sonia.scm.api.v2.resources.InstantAttributeMapper;
import sonia.scm.repository.NamespaceAndName;
import sonia.scm.user.UserDisplayManager;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import static de.otto.edison.hal.Links.linkingTo;
import static java.util.stream.Collectors.toList;

@Mapper
public abstract class ExecutedTransitionMapper implements InstantAttributeMapper {

  @Inject
  private UserDisplayManager userDisplayManager;
  @Inject
  private CommentPathBuilder commentPathBuilder;

  @Mapping(target = "attributes", ignore = true)
  @Mapping(target = "user", source = "user", qualifiedByName = "mapUser")
  abstract ExecutedTransitionDto map(ExecutedTransition transition, @Context NamespaceAndName namespaceAndName, @Context String pullRequestId, @Context BasicComment comment);

  @Named("mapUser")
  DisplayedUserDto mapUser(String userId) {
    return new DisplayUserMapper(userDisplayManager).map(userId);
  }

  @ObjectFactory
  ExecutedTransitionDto createDto(ExecutedTransition transition, @Context NamespaceAndName namespaceAndName, @Context String pullRequestId, @Context BasicComment comment) {
    Links.Builder linksBuilder = linkingTo()
      .self(commentPathBuilder.createExecutedTransitionUri(namespaceAndName.getNamespace(), namespaceAndName.getName(), pullRequestId, comment.getId(), transition.getId()));

    return new ExecutedTransitionDto(linksBuilder.build());
  }

  String map(Transition transition) {
    return transition == null? null: transition.name();
  }

  void appendTransitions(BasicCommentDto target, BasicComment source, NamespaceAndName namespaceAndName, String pullRequestId) {
    target.withEmbedded(
      "transitions",
      source
        .getExecutedTransitions()
        .stream()
        .map(t -> this.map(t, namespaceAndName, pullRequestId, source))
        .collect(toList())
    );
  }
}
