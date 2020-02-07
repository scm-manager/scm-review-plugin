package com.cloudogu.scm.review.pullrequest.service;

import com.cloudogu.scm.review.comment.service.Comment;
import com.cloudogu.scm.review.comment.service.CommentEvent;
import com.cloudogu.scm.review.comment.service.Location;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.HandlerEventType;
import sonia.scm.repository.Repository;

import java.util.Arrays;
import java.util.Collections;

import static com.google.common.collect.ImmutableSet.of;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RemoveReviewMarksOnChangedCommentsHookTest {

  private Repository repository = new Repository("1", "git", "space", "X");
  private PullRequest pullRequest = new PullRequest();
  private Comment comment = new Comment();

  {
    pullRequest.setId("123");
  }

  @Mock
  PullRequestService service;
  @InjectMocks
  RemoveReviewMarksOnChangedCommentsHook hook;

  @Test
  void shouldIgnoreCommentsWithoutLocation() {
    CommentEvent event = new CommentEvent(repository, pullRequest, comment, null, HandlerEventType.CREATE);

    hook.handleCommentEvents(event);

    verify(service, never()).removeReviewMarks(any(), any(), any());
  }

  @Test
  void shouldIgnoreCommentDeletion() {
    CommentEvent event = new CommentEvent(repository, pullRequest, comment, null, HandlerEventType.DELETE);

    hook.handleCommentEvents(event);

    verify(service, never()).removeReviewMarks(any(), any(), any());
  }

  @Test
  void shouldRemoveMarksOnNewCommentWithSameLocation() {
    comment.setLocation(new Location("some/file"));
    pullRequest.setReviewMarks(of(new ReviewMark("some/file", "dent")));
    CommentEvent event = new CommentEvent(repository, pullRequest, comment, null, HandlerEventType.CREATE);

    hook.handleCommentEvents(event);

    verify(service).removeReviewMarks(repository, pullRequest.getId(), asList(new ReviewMark("some/file", "dent")));
  }

  @Test
  void shouldNotRemoveMarksOnNewCommentWithDifferentLocation() {
    comment.setLocation(new Location("some/other/file"));
    pullRequest.setReviewMarks(of(new ReviewMark("some/file", "dent")));
    CommentEvent event = new CommentEvent(repository, pullRequest, comment, null, HandlerEventType.CREATE);

    hook.handleCommentEvents(event);

    verify(service, atMost(1)).removeReviewMarks(repository, pullRequest.getId(), emptyList());
  }
}
