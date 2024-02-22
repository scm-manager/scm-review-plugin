/*
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
