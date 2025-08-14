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
    when(commentPathBuilder.createDeleteCommentUri(any(), any(), any(), any(), any())).thenAnswer(
      invocation -> prefix + "/pull-requests/" + invocation.getArgument(0) + "/" + invocation.getArgument(1) + "/" + invocation.getArgument(2) + "/comments/" + invocation.getArgument(3)
    );
    when(commentPathBuilder.createUpdateCommentUri(any(), any(), any(), any(), any())).thenAnswer(
      invocation -> prefix + "/pull-requests/" + invocation.getArgument(0) + "/" + invocation.getArgument(1) + "/" + invocation.getArgument(2) + "/comments/" + invocation.getArgument(3)
    );
    when(commentPathBuilder.createUpdateCommentWithImageUri(any(), any(), any(), any(), any())).thenAnswer(
      invocation -> prefix + "/pull-requests/" + invocation.getArgument(0) + "/" + invocation.getArgument(1) + "/" + invocation.getArgument(2) + "/comments/" + invocation.getArgument(3) + "/images"
    );
    when(commentPathBuilder.createReplyCommentUri(any(), any(), any(), any(), any())).thenAnswer(
      invocation -> prefix + "/pull-requests/" + invocation.getArgument(0) + "/" + invocation.getArgument(1) + "/" + invocation.getArgument(2) + "/comments/" + invocation.getArgument(3) + "/reply"
    );
    when(commentPathBuilder.createReplyCommentWithImageUri(any(), any(), any(), any(), any())).thenAnswer(
      invocation -> prefix + "/pull-requests/" + invocation.getArgument(0) + "/" + invocation.getArgument(1) + "/" + invocation.getArgument(2) + "/comments/" + invocation.getArgument(3) + "/reply/images"
    );
    when(commentPathBuilder.createDeleteReplyUri(any(), any(), any(), any(), any(), any())).thenAnswer(
      invocation -> prefix + "/pull-requests/" + invocation.getArgument(0) + "/" + invocation.getArgument(1) + "/" + invocation.getArgument(2) + "/comments/" + invocation.getArgument(3) + "/replies/" + invocation.getArgument(4)
    );
    when(commentPathBuilder.createUpdateReplyUri(any(), any(), any(), any(), any(), any())).thenAnswer(
      invocation -> prefix + "/pull-requests/" + invocation.getArgument(0) + "/" + invocation.getArgument(1) + "/" + invocation.getArgument(2) + "/comments/" + invocation.getArgument(3) + "/replies/" + invocation.getArgument(4)
    );
    when(commentPathBuilder.createUpdateReplyWithImageUri(any(), any(), any(), any(), any(), any())).thenAnswer(
      invocation -> prefix + "/pull-requests/" + invocation.getArgument(0) + "/" + invocation.getArgument(1) + "/" + invocation.getArgument(2) + "/comments/" + invocation.getArgument(3) + "/replies/" + invocation.getArgument(4) + "/images"
    );
    when(commentPathBuilder.createPossibleTransitionUri(any(), any(), any(), any())).thenAnswer(
      invocation -> prefix + "/pull-requests/" + invocation.getArgument(0) + "/" + invocation.getArgument(1) + "/" + invocation.getArgument(2) + "/comments/" + invocation.getArgument(3) + "/transitions"
    );
    when(commentPathBuilder.createExecutedTransitionUri(any(), any(), any(), any(), any())).thenAnswer(
      invocation -> prefix + "/pull-requests/" + invocation.getArgument(0) + "/" + invocation.getArgument(1) + "/" + invocation.getArgument(2) + "/comments/" + invocation.getArgument(3) + "/transitions/" + invocation.getArgument(4)
    );
    return commentPathBuilder;
  }
}
