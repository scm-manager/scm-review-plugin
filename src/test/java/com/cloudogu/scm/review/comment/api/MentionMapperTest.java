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

import com.cloudogu.scm.review.comment.service.BasicComment;
import com.cloudogu.scm.review.comment.service.Comment;
import com.cloudogu.scm.review.comment.service.Location;
import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.user.DisplayUser;
import sonia.scm.user.User;
import sonia.scm.user.UserDisplayManager;

import java.util.Optional;
import java.util.Set;

import static com.cloudogu.scm.review.TestData.createComment;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
class MentionMapperTest {

  @Mock
  private UserDisplayManager displayManager;

  @InjectMocks
  private MentionMapper mentionMapper;

  private Comment comment;

  @BeforeEach
  void initComment() {
    comment = createComment();
    comment.setComment("@[dent] Did you test this? Please report to @[_anonymous] @[trillian] ");
  }

  @Test
  void shouldAppendMentionForMentionedUser() {
    DisplayUser user = setupExistingUserMock("dent", "Arthur Dent", "dent@hitchhiker.org");

    Set<String> userIds = ImmutableSet.of("dent");
    Set<DisplayUser> mentions = mentionMapper.mapMentions(userIds);

    assertThat(mentions.iterator().next()).isEqualTo(user);
  }


  @Test
  void shouldAppendMultipleMentionsForMentionedUser() {
    DisplayUser user1 = setupExistingUserMock("dent", "Arthur Dent", "dent@hitchhiker.org");
    DisplayUser user2 = setupExistingUserMock("_anonymous", "SCM Anonymous", "anonymous@hitchhiker.org");

    Set<String> userIds = ImmutableSet.of("dent", "_anonymous");
    Set<DisplayUser> mentions = mentionMapper.mapMentions(userIds);

    assertThat(mentions).hasSize(2);
    assertThat(mentions).contains(user1);
    assertThat(mentions).contains(user2);
  }

  @Test
  void shouldNotAppendMentionIfUserDoesNotExist() {
    doReturn(Optional.empty()).when(displayManager).get("trillian");

    Set<String> userIds = ImmutableSet.of("trillian");
    Set<DisplayUser> mentions = mentionMapper.mapMentions(userIds);

    assertThat(mentions).hasSize(0);
  }

  @Test
  void shouldExtractMentionsFromComment() {
    setupExistingUserMock("dent", "Arthur Dent", "dent@hitchhiker.org");
    setupExistingUserMock("_anonymous", "SCM Anonymous", "anonymous@hitchhiker.org");

    Set<String> mentions = mentionMapper.extractMentionsFromComment(comment.getComment());
    assertThat(mentions).hasSize(2);
    assertThat(mentions).contains("dent");
    assertThat(mentions).contains("_anonymous");
  }

  @Test
  void shouldParseUserIdsToDisplayNames() {
    setupExistingUserMock("dent", "Arthur Dent", "dent@hitchhiker.org");
    setupExistingUserMock("_anonymous", "SCM Anonymous", "anonymous@hitchhiker.org");

    BasicComment unparsedComment = Comment.createComment("1", "new comment @[dent] @[_anonymous]", "author", new Location());
    unparsedComment.setMentionUserIds(ImmutableSet.of("dent", "_anonymous"));

    BasicComment parsedComment = mentionMapper.parseMentionsUserIdsToDisplayNames(unparsedComment);

    assertThat(parsedComment.getComment()).isEqualTo("new comment @Arthur Dent @SCM Anonymous");
  }

  @Test
  void shouldNotExtractMentionsFromCommentIfUserDoesNotExist() {
    Set<String> mentions = mentionMapper.extractMentionsFromComment(comment.getComment());
    assertThat(mentions).hasSize(0);
  }


  private DisplayUser setupExistingUserMock(String name, String displayName, String email) {
    Optional<DisplayUser> user = Optional.of(DisplayUser.from(new User(name, displayName, email)));
    doReturn(user).when(displayManager).get(name);
    return user.get();
  }

}
