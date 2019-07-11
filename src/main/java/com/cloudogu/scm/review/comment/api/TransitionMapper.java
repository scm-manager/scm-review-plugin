package com.cloudogu.scm.review.comment.api;

import com.cloudogu.scm.review.comment.service.CommentTransition;
import de.otto.edison.hal.Link;
import de.otto.edison.hal.Links;

import javax.inject.Inject;
import java.util.Collection;

import static java.util.stream.Collectors.toList;

public class TransitionMapper {

  private final CommentPathBuilder commentPathBuilder;

  @Inject
  public TransitionMapper(CommentPathBuilder commentPathBuilder) {
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
