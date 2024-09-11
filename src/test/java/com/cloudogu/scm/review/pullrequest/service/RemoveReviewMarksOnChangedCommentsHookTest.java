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

import com.cloudogu.scm.review.comment.service.Comment;
import com.cloudogu.scm.review.comment.service.CommentEvent;
import com.cloudogu.scm.review.comment.service.CommentService;
import com.cloudogu.scm.review.comment.service.Location;
import com.cloudogu.scm.review.comment.service.Reply;
import com.cloudogu.scm.review.comment.service.ReplyEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.HandlerEventType;
import sonia.scm.repository.Repository;

import static com.google.common.collect.ImmutableSet.of;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RemoveReviewMarksOnChangedCommentsHookTest {

  private Repository repository = new Repository("1", "git", "space", "X");
  private PullRequest pullRequest = new PullRequest();
  private Comment comment = new Comment();

  {
    pullRequest.setId("123");
  }

  @Mock
  PullRequestService pullRequestService;
  @Mock
  CommentService commentService;
  @InjectMocks
  RemoveReviewMarksOnChangedCommentsHook hook;

  @Test
  void shouldIgnoreCommentsWithoutLocation() {
    CommentEvent event = new CommentEvent(repository, pullRequest, comment, null, HandlerEventType.CREATE);

    hook.handleCommentEvents(event);

    verify(pullRequestService, never()).removeReviewMarks(any(), any(), any());
  }

  @Test
  void shouldIgnoreCommentDeletion() {
    CommentEvent event = new CommentEvent(repository, pullRequest, null, comment, HandlerEventType.DELETE);

    hook.handleCommentEvents(event);

    verify(pullRequestService, never()).removeReviewMarks(any(), any(), any());
  }

  @Test
  void shouldRemoveMarksOnNewCommentWithSameLocation() {
    comment.setLocation(new Location("some/file"));
    pullRequest.setReviewMarks(of(new ReviewMark("some/file", "dent")));
    CommentEvent event = new CommentEvent(repository, pullRequest, comment, null, HandlerEventType.CREATE);

    hook.handleCommentEvents(event);

    verify(pullRequestService).removeReviewMarks(repository, pullRequest.getId(), singletonList(new ReviewMark("some/file", "dent")));
  }

  @Test
  void shouldNotRemoveMarksOnNewCommentWithDifferentLocation() {
    comment.setLocation(new Location("some/other/file"));
    pullRequest.setReviewMarks(of(new ReviewMark("some/file", "dent")));
    CommentEvent event = new CommentEvent(repository, pullRequest, comment, null, HandlerEventType.CREATE);

    hook.handleCommentEvents(event);

    verify(pullRequestService, atMost(1)).removeReviewMarks(repository, pullRequest.getId(), emptyList());
  }

  @Test
  void shouldRemoveMarksOnNewReplyOnCommentWithSameLocation() {
    Reply reply = Reply.createReply("321", "reply", "trillian");

    comment.setLocation(new Location("some/file"));
    comment.setReplies(singletonList(reply));

    pullRequest.setReviewMarks(of(new ReviewMark("some/file", "dent")));

    when(commentService.getAll(repository.getNamespace(), repository.getName(), pullRequest.getId()))
      .thenReturn(singletonList(comment));

    ReplyEvent event = new ReplyEvent(repository, pullRequest, reply, null, comment, HandlerEventType.CREATE);

    hook.handleReplyEvents(event);

    verify(pullRequestService).removeReviewMarks(repository, pullRequest.getId(), singletonList(new ReviewMark("some/file", "dent")));
  }

  @Test
  void shouldIgnoreReplyOnCommentWithoutLocation() {
    Reply reply = Reply.createReply("321", "reply", "trillian");

    comment.setReplies(singletonList(reply));

    pullRequest.setReviewMarks(of(new ReviewMark("some/file", "dent")));

    when(commentService.getAll(repository.getNamespace(), repository.getName(), pullRequest.getId()))
      .thenReturn(singletonList(comment));

    ReplyEvent event = new ReplyEvent(repository, pullRequest, reply, null, comment, HandlerEventType.CREATE);

    hook.handleReplyEvents(event);

    verify(pullRequestService, never()).removeReviewMarks(any(), any(), any());
  }

  @Test
  void shouldIgnoreDeletedReply() {
    Reply reply = Reply.createReply("321", "reply", "trillian");

    ReplyEvent event = new ReplyEvent(repository, pullRequest, null, reply, comment, HandlerEventType.DELETE);

    hook.handleReplyEvents(event);

    verify(pullRequestService, never()).removeReviewMarks(any(), any(), any());
  }
}
