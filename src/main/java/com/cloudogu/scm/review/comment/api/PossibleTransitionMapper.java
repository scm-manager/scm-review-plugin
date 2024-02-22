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

import com.cloudogu.scm.review.comment.service.CommentTransition;
import de.otto.edison.hal.Link;
import de.otto.edison.hal.Links;

import jakarta.inject.Inject;
import java.util.Collection;

import static java.util.stream.Collectors.toList;

public class PossibleTransitionMapper {

  private final CommentPathBuilder commentPathBuilder;

  @Inject
  public PossibleTransitionMapper(CommentPathBuilder commentPathBuilder) {
    this.commentPathBuilder = commentPathBuilder;
  }

  TransitionDto map(CommentTransition transition, String namespace, String name, String pullRequestId, String commentId) {
    Links links = Links.linkingTo().single(Link.link("transform", commentPathBuilder.createPossibleTransitionUri(namespace, name, pullRequestId, commentId))).build();
    return new TransitionDto(links, transition.name());
  }

  void appendTransitions(BasicCommentDto target, Collection<CommentTransition> transitions, String namespace, String name, String pullRequestId, String commentId) {
    target.withEmbedded(
      "possibleTransitions",
      transitions
        .stream()
        .map(t -> this.map(t, namespace, name, pullRequestId, commentId))
        .collect(toList())
    );
  }
}
