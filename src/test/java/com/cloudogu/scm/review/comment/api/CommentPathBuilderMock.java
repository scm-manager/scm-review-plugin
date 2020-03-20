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
    when(commentPathBuilder.createReplyCommentUri(any(), any(), any(), any(), any())).thenAnswer(
      invocation -> prefix + "/pull-requests/" + invocation.getArgument(0) + "/" + invocation.getArgument(1) + "/" + invocation.getArgument(2) + "/comments/" + invocation.getArgument(3) + "/reply"
    );
    when(commentPathBuilder.createDeleteReplyUri(any(), any(), any(), any(), any(), any())).thenAnswer(
      invocation -> prefix + "/pull-requests/" + invocation.getArgument(0) + "/" + invocation.getArgument(1) + "/" + invocation.getArgument(2) + "/comments/" + invocation.getArgument(3) + "/replies/" + invocation.getArgument(4)
    );
    when(commentPathBuilder.createUpdateReplyUri(any(), any(), any(), any(), any(), any())).thenAnswer(
      invocation -> prefix + "/pull-requests/" + invocation.getArgument(0) + "/" + invocation.getArgument(1) + "/" + invocation.getArgument(2) + "/comments/" + invocation.getArgument(3) + "/replies/" + invocation.getArgument(4)
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
