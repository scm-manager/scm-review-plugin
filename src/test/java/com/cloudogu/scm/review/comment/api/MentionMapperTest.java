package com.cloudogu.scm.review.comment.api;

import com.cloudogu.scm.review.TestData;
import com.cloudogu.scm.review.comment.service.Comment;
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
    comment = TestData.createComment();
    comment.setComment("@[dent] Did you test this? Please report to @[_anonymous] @[trillian] ");
  }

  @Test
  void shouldAppendMentionForMentionedUser() {
    setupExistingUserMock("dent");

    Set<String> userIds = ImmutableSet.of("dent");
    Set<Mention> mentions = mentionMapper.mapMentions(userIds);

    assertThat(mentions).contains(new Mention("dent", "dent"));
  }


  @Test
  void shouldAppendMultipleMentionsForMentionedUser() {
    setupExistingUserMock("dent");
    setupExistingUserMock("_anonymous");

    Set<String> userIds = ImmutableSet.of("dent", "_anonymous");
    Set<Mention> mentions = mentionMapper.mapMentions(userIds);

    assertThat(mentions).hasSize(2);
    assertThat(mentions).contains(new Mention("dent", "dent"));
    assertThat(mentions).contains(new Mention("_anonymous", "_anonymous"));
  }

  @Test
  void shouldNotAppendMentionIfUserDoesNotExist() {
    doReturn(Optional.empty()).when(displayManager).get("trillian");

    Set<String> userIds = ImmutableSet.of("trillian");
    Set<Mention> mentions = mentionMapper.mapMentions(userIds);

    assertThat(mentions).hasSize(0);
  }

  @Test
  void shouldExtractMentionsFromComment() {
    setupExistingUserMock("dent");
    setupExistingUserMock("_anonymous");

    Set<String> mentions = mentionMapper.extractMentionsFromComment(comment.getComment());
    assertThat(mentions).hasSize(2);
    assertThat(mentions).contains("dent");
    assertThat(mentions).contains("_anonymous");
  }

  @Test
  void shouldNotExtractMentionsFromCommentIfUserDoesNotExist() {
    Set<String> mentions = mentionMapper.extractMentionsFromComment(comment.getComment());
    assertThat(mentions).hasSize(0);
  }


  private void setupExistingUserMock(String name) {
    Optional<DisplayUser> user = Optional.of(DisplayUser.from(new User(name)));
    doReturn(user).when(displayManager).get(name);
  }

}
