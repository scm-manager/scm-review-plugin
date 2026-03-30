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

package com.cloudogu.scm.review.mcp;

import com.cloudogu.mcp.ToolResult;
import com.cloudogu.scm.review.comment.service.Comment;
import com.cloudogu.scm.review.comment.service.CommentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReplyToCommentToolTest {

  @Mock
  private CommentService commentService;
  @InjectMocks
  private ReplyToCommentTool tool;

  @Test
  void shouldCreateReply() {
    when(commentService.get("hitchhiker", "hog", "42", "1337"))
      .thenReturn(new Comment());

    ReplyToCommentToolInput input = new ReplyToCommentToolInput("hitchhiker", "hog", "42", "1337", "Reply");

    ToolResult result = tool.execute(input);

    assertThat(result.getContent().get(0))
      .isEqualTo("STATUS: [SUCCESS] The reply has been submitted.\n");
    verify(commentService)
      .reply(
        eq("hitchhiker"),
        eq("hog"),
        eq("42"),
        eq("1337"),
        argThat(
          reply -> {
            assertThat(reply.getComment())
              .isEqualTo("Reply");
            return true;
          }
        )
      );
  }
}
