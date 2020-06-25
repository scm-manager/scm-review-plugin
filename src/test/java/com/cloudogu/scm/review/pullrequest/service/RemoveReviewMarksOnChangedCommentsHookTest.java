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
