package com.cloudogu.scm.review.comment.service;

import com.google.common.collect.Maps;
import lombok.val;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import sonia.scm.NotFoundException;
import sonia.scm.store.DataStore;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CommentStoreTest {

  private final Map<String, PullRequestComments> backingMap = Maps.newHashMap();
  @Mock
  private DataStore<PullRequestComments> dataStore;

  private CommentStore store;

  @BeforeEach
  void init(){
    store = new CommentStore(dataStore);
    // delegate store methods to backing map
    when(dataStore.getAll()).thenReturn(backingMap);

    doAnswer(invocationOnMock -> {
      String id = invocationOnMock.getArgument(0);
      PullRequestComments pullRequestComments = invocationOnMock.getArgument(1);
      backingMap.put(id, pullRequestComments);
      return null;
    }).when(dataStore).put(anyString(), any(PullRequestComments.class));
  }

  @Test
   void shouldAddTheFirstComment() {
    val pullRequestId = "1";
    when(dataStore.get(pullRequestId)).thenReturn(null);
    val pullRequestComment = new PullRequestComment(1, "my Comment", "author", Instant.now());
    store.add(pullRequestId, pullRequestComment);
    assertThat(backingMap)
      .isNotEmpty()
      .hasSize(1)
      .containsKeys(pullRequestId);
  }

  @Test
  void shouldAddCommentToExistingCommentList() {
    val pullRequestId = "1";
    val oldPRComment  = new PullRequestComment(1, "my comment", "author", Instant.now());
    val pullRequestComments = new PullRequestComments();
    val newPullRequestComment   = new PullRequestComment(2, "my new comment", "author", Instant.now());
    pullRequestComments.setComments(Lists.newArrayList(oldPRComment));

    when(dataStore.get(pullRequestId)).thenReturn(pullRequestComments);
    store.add(pullRequestId, newPullRequestComment);
    assertThat(backingMap)
      .isNotEmpty()
      .hasSize(1)
      .containsKeys(pullRequestId);
    assertThat(pullRequestComments.getComments())
      .isNotEmpty()
      .containsExactly(oldPRComment, newPullRequestComment);
  }

  @Test
  void shouldCreateNextId(){
    // test the first id
    when(dataStore.get("id")).thenReturn(null);
    int id = store.createId("id");
    assertThat(id).isEqualTo(1);

    // test the next id
    PullRequestComments pullRequestComments = new PullRequestComments();
    pullRequestComments.getComments().add(new PullRequestComment(1, "my first comment", "author", Instant.now()));
    pullRequestComments.getComments().add(new PullRequestComment(2, "my second comment", "author", Instant.now()));

    when(dataStore.get("id")).thenReturn(pullRequestComments);
    id = store.createId("id");

    assertThat(id).isEqualTo(3);
  }

  @Test
  void shouldDeleteAnExistingComment(){
    val pullRequestComments = new PullRequestComments();
    pullRequestComments.getComments().add(new PullRequestComment(1, "1. comment", "author", Instant.now()));
    pullRequestComments.getComments().add(new PullRequestComment(2, "2. comment", "author", Instant.now()));
    pullRequestComments.getComments().add(new PullRequestComment(3, "3. comment", "author", Instant.now()));
    val pullRequestId = "id";
    when(dataStore.get(pullRequestId)).thenReturn(pullRequestComments);

    store.delete(pullRequestId, 2);

    PullRequestComments comments = store.get(pullRequestId);
    assertThat(comments.getComments())
      .extracting("id")
      .containsExactly(1,3);

    // delete a removed comment has no effect
    store.delete(pullRequestId, 2);

    comments = store.get(pullRequestId);
    assertThat(comments.getComments())
      .extracting("id")
      .containsExactly(1,3);

  }

  @Test
  void shouldGetPullRequestComments(){
    val pullRequestComments = new PullRequestComments();
    pullRequestComments.getComments().add(new PullRequestComment(1, "1. comment", "author", Instant.now()));
    pullRequestComments.getComments().add(new PullRequestComment(2, "2. comment", "author", Instant.now()));
    pullRequestComments.getComments().add(new PullRequestComment(3, "3. comment", "author", Instant.now()));
    val pullRequestId = "id";
    when(dataStore.get(pullRequestId)).thenReturn(pullRequestComments);

    PullRequestComments comments = store.get(pullRequestId);
    assertThat(comments.getComments())
      .extracting("id")
      .containsExactly(1,2,3);
  }

  @Test
  void shouldThrowNotFoundException() {
    Assertions.assertThrows(NotFoundException.class, () -> store.get( "iDontExist"));
  }

}
