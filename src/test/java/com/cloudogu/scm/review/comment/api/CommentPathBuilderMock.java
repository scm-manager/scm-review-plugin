package com.cloudogu.scm.review.comment.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CommentPathBuilderMock {

  static CommentPathBuilder createMock() {
    return createMock("/v2");
  }
  static CommentPathBuilder createMock(String prefix) {
    CommentPathBuilder commentPathBuilder = mock(CommentPathBuilder.class);
    when(commentPathBuilder.createCommentSelfUri(any(), any(), any(), any())).thenAnswer(
      invocation -> prefix + "/pull-requests/" + invocation.getArgument(0) + "/" + invocation.getArgument(1) + "/" + invocation.getArgument(2) + "/comments/" + invocation.getArgument(3)
    );
    when(commentPathBuilder.createReplySelfUri(any(), any(), any(), any(), any())).thenAnswer(
      invocation -> prefix + "/pull-requests/" + invocation.getArgument(0) + "/" + invocation.getArgument(1) + "/" + invocation.getArgument(2) + "/comments/" + invocation.getArgument(3) + "/replies/" + invocation.getArgument(4)
    );
    when(commentPathBuilder.createDeleteCommentUri(any(), any(), any(), any())).thenAnswer(
      invocation -> prefix + "/pull-requests/" + invocation.getArgument(0) + "/" + invocation.getArgument(1) + "/" + invocation.getArgument(2) + "/comments/" + invocation.getArgument(3)
    );
    when(commentPathBuilder.createUpdateCommentUri(any(), any(), any(), any())).thenAnswer(
      invocation -> prefix + "/pull-requests/" + invocation.getArgument(0) + "/" + invocation.getArgument(1) + "/" + invocation.getArgument(2) + "/comments/" + invocation.getArgument(3)
    );
    when(commentPathBuilder.createReplyCommentUri(any(), any(), any(), any())).thenAnswer(
      invocation -> prefix + "/pull-requests/" + invocation.getArgument(0) + "/" + invocation.getArgument(1) + "/" + invocation.getArgument(2) + "/comments/" + invocation.getArgument(3) + "/reply"
    );
    when(commentPathBuilder.createDeleteReplyUri(any(), any(), any(), any(), any())).thenAnswer(
      invocation -> prefix + "/pull-requests/" + invocation.getArgument(0) + "/" + invocation.getArgument(1) + "/" + invocation.getArgument(2) + "/comments/" + invocation.getArgument(3) + "/replies/" + invocation.getArgument(4)
    );
    when(commentPathBuilder.createUpdateReplyUri(any(), any(), any(), any(), any())).thenAnswer(
      invocation -> prefix + "/pull-requests/" + invocation.getArgument(0) + "/" + invocation.getArgument(1) + "/" + invocation.getArgument(2) + "/comments/" + invocation.getArgument(3) + "/replies/" + invocation.getArgument(4)
    );
    when(commentPathBuilder.createTransitionUri(any(), any(), any(), any())).thenAnswer(
      invocation -> prefix + "/pull-requests/" + invocation.getArgument(0) + "/" + invocation.getArgument(1) + "/" + invocation.getArgument(2) + "/comments/" + invocation.getArgument(3) + "/transitions"
    );
    return commentPathBuilder;
  }
}
