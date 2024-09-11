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

package com.cloudogu.scm.review.pullrequest.service;

import com.cloudogu.scm.review.comment.service.BasicComment;
import com.cloudogu.scm.review.comment.service.BasicCommentEvent;
import com.cloudogu.scm.review.comment.service.Comment;
import com.cloudogu.scm.review.comment.service.CommentEvent;
import com.cloudogu.scm.review.comment.service.CommentService;
import com.cloudogu.scm.review.comment.service.Location;
import com.cloudogu.scm.review.comment.service.Reply;
import com.cloudogu.scm.review.comment.service.ReplyEvent;
import com.github.legman.Subscribe;
import sonia.scm.EagerSingleton;
import sonia.scm.HandlerEventType;
import sonia.scm.plugin.Extension;
import sonia.scm.repository.Repository;

import jakarta.inject.Inject;
import java.util.Collection;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@EagerSingleton
@Extension
public class RemoveReviewMarksOnChangedCommentsHook {

  private final PullRequestService pullRequestService;
  private final CommentService commentService;

  @Inject
  public RemoveReviewMarksOnChangedCommentsHook(PullRequestService pullRequestService, CommentService commentService) {
    this.pullRequestService = pullRequestService;
    this.commentService = commentService;
  }

  @Subscribe
  public void handleCommentEvents(CommentEvent event) {
    checkForLocation(event, Comment::getLocation);
  }

  @Subscribe
  public void handleReplyEvents(ReplyEvent event) {
    checkForLocation(event, reply -> getLocationForReply(reply, event.getPullRequest(), event.getRepository()));
  }

  private Location getLocationForReply(Reply reply, PullRequest pullRequest, Repository repository) {
    return commentService.getAll(repository.getNamespace(), repository.getName(), pullRequest.getId())
      .stream()
      .filter(comment -> comment.getReplies().stream().map(Reply::getId).anyMatch(id -> id.equals(reply.getId())))
      .map(Comment::getLocation)
      .filter(Objects::nonNull)
      .findAny()
      .orElse(null);
  }

  private <C extends BasicComment, T extends BasicCommentEvent<C>> void checkForLocation(T event, Function<C, Location> locationExtractor) {
    if (event.getEventType() == HandlerEventType.DELETE) {
      return;
    }
    Location location = locationExtractor.apply(event.getItem());
    if (location == null) {
      return;
    }

    Collection<ReviewMark> reviewMarksToBeRemoved = event.getPullRequest()
      .getReviewMarks()
      .stream()
      .filter(mark -> mark.getFile().equals(location.getFile()))
      .collect(Collectors.toList());

    pullRequestService.removeReviewMarks(event.getRepository(), event.getPullRequest().getId(), reviewMarksToBeRemoved);
  }
}
