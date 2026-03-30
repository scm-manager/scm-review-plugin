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
import com.cloudogu.scm.review.comment.service.CommentType;
import com.cloudogu.scm.review.comment.service.ContextLine;
import com.cloudogu.scm.review.comment.service.InlineContext;
import com.cloudogu.scm.review.comment.service.Location;
import com.cloudogu.scm.review.comment.service.Reply;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static com.cloudogu.scm.review.comment.service.Comment.createComment;
import static com.cloudogu.scm.review.comment.service.Reply.createReply;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReadCommentsToolTest {

  private static final Comment GLOBAL_COMMENT = createComment("hg42",
    "Does this really have to be so complicated?",
    "Marvin",
    null
  );
  private static final Comment GLOBAL_TASK = createComment("6times7", "What is the question?", "Deep Thought", null);
  private static final Comment FILE_COMMENT = createComment("e1337",
    "Nice planet you have.\n\nWould be a pity when someone would want to build an intergalactic highway here!",
    "Prostetnic Vogon Jeltz",
    new Location("earth.md")
  );
  private static final Comment LINE_COMMENT = createComment("1in3",
    "Nice planet you have.\n\nWould be a pity when someone would want to build an intergalactic highway here!",
    "Prostetnic Vogon Jeltz",
    new Location("earth.md")
  );
  private static final Reply REPLY_1 = createReply("irrelevant", "Want to hear a poem?\nI've got a lot of them at hand.", "Prostetnic Vogon Jeltz");
  private static final Reply REPLY_2 = createReply("irrelevant", "Dare you!", "Marvin");

  static {
    GLOBAL_COMMENT.setDate(Instant.parse("1984-02-10T07:30:00.0Z"));
    GLOBAL_COMMENT.setReplies(List.of(REPLY_1, REPLY_2));
    GLOBAL_TASK.setDate(Instant.parse("1984-02-10T07:30:00.0Z"));
    FILE_COMMENT.setDate(Instant.parse("1982-12-10T07:30:00.0Z"));
    GLOBAL_TASK.setType(CommentType.TASK_TODO);
    LINE_COMMENT.setDate(Instant.parse("1982-12-10T07:30:00.0Z"));
    LINE_COMMENT.setContext(
      new InlineContext(
        List.of(
          new ContextLine(1, 1, "# The Earth"),
          new ContextLine(2, 2, ""),
          new ContextLine(3, null, "The earth is a nice planet with perfectly crafted landscape."),
          new ContextLine(null, 3, "The earth has been a nondescript overrated planet.")
        )
      )
    );
    LINE_COMMENT.setLocation(new Location("earth.md", "@@ -0,3 +0,3 @@", null, 3));
    REPLY_1.setDate(Instant.parse("1984-02-11T10:00:00Z"));
    REPLY_2.setDate(Instant.parse("1984-02-11T10:00:01Z"));
  }

  @Mock
  private CommentService commentService;
  @InjectMocks
  private ReadCommentsTool tool;

  @Test
  void shouldRenderGlobalComment() {
    when(commentService.getAll("galaxy", "milky_way", "42")).thenReturn(List.of(GLOBAL_COMMENT));

    ToolResult result = tool.execute(new ReadCommentsToolInput("galaxy", "milky_way", "42"));

    assertThat(result.getContent().get(0)).isEqualTo("""
      STATUS: [SUCCESS] Comments have been read successfully.
      ---------------------------------------------------------
      # Global Comments
            
      **Type:** Simple | **Author:** Marvin
            
      <comment_body id='hg42' timestamp='1984-02-10T07:30:00Z'>
      Does this really have to be so complicated?
      </comment_body>
      
      > **Reply from:** Prostetnic Vogon Jeltz
      > <reply_body timestamp='1984-02-11T10:00:00Z'>
      > Want to hear a poem?
      > I've got a lot of them at hand.
      > </reply_body>
      >
      > **Reply from:** Marvin
      > <reply_body timestamp='1984-02-11T10:00:01Z'>
      > Dare you!
      > </reply_body>
      
      """);
  }

  @Test
  void shouldRenderGlobalTask() {
    when(commentService.getAll("galaxy", "milky_way", "42")).thenReturn(List.of(GLOBAL_TASK));

    ToolResult result = tool.execute(new ReadCommentsToolInput("galaxy", "milky_way", "42"));

    assertThat(result.getContent().get(0)).isEqualTo("""
      STATUS: [SUCCESS] Comments have been read successfully.
      ---------------------------------------------------------
      # Global Comments
            
      **Type:** Open Task | **Author:** Deep Thought
            
      <comment_body id='6times7' timestamp='1984-02-10T07:30:00Z'>
      What is the question?
      </comment_body>
      
      """);
  }

  @Test
  void shouldRenderFileComment() {
    when(commentService.getAll("galaxy", "milky_way", "42")).thenReturn(List.of(FILE_COMMENT));

    ToolResult result = tool.execute(new ReadCommentsToolInput("galaxy", "milky_way", "42"));

    assertThat(result.getContent().get(0)).isEqualTo("""
      STATUS: [SUCCESS] Comments have been read successfully.
      ---------------------------------------------------------
      # File Comments: `earth.md`
           
      **Type:** Simple | **Author:** Prostetnic Vogon Jeltz
           
      <comment_body id='e1337' timestamp='1982-12-10T07:30:00Z'>
      Nice planet you have.
          
      Would be a pity when someone would want to build an intergalactic highway here!
      </comment_body>
      
      """);
  }

  @Test
  void shouldRenderLineComment() {
    when(commentService.getAll("galaxy", "milky_way", "42")).thenReturn(List.of(LINE_COMMENT));

    ToolResult result = tool.execute(new ReadCommentsToolInput("galaxy", "milky_way", "42"));

    assertThat(result.getContent().get(0)).isEqualTo("""
      STATUS: [SUCCESS] Comments have been read successfully.
      ---------------------------------------------------------
      # File Comments: `earth.md`

      **Type:** Simple | **Author:** Prostetnic Vogon Jeltz

      *The comment refers to new line number 3 of the file.*
      The diff features explicit side-by-side line numbers (formatted as `[ old | new ]`)

      ```diff
      [    1 |    1 ]  # The Earth
      [    2 |    2 ] \s
      [    3 |    - ] -The earth is a nice planet with perfectly crafted landscape.
      [    - |    3 ] +The earth has been a nondescript overrated planet.
      ```

      <comment_body id='1in3' timestamp='1982-12-10T07:30:00Z'>
      Nice planet you have.

      Would be a pity when someone would want to build an intergalactic highway here!
      </comment_body>
      
      """);
  }
}
