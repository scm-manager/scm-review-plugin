package com.cloudogu.scm.review.comment.service;

import com.google.common.collect.Maps;
import lombok.val;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
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
public class PullRequestCommentStoreTest {

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
  public void shouldAddTheFirstComment() {
    val pullRequestId = "1";
    when(dataStore.get(pullRequestId)).thenReturn(null);
    PullRequestComment pullRequestComment = new PullRequestComment("id", "my Comment", "author", Instant.now());
    store.add(pullRequestId, pullRequestComment);
    assertThat(backingMap)
      .isNotEmpty()
      .hasSize(1)
      .containsKeys(pullRequestId);
  }

  @Test
  public void shouldAddCommentToExistingCommentList() {
    val pullRequestId = "1";
    val oldPRComment  = new PullRequestComment("id", "my comment", "author", Instant.now());
    val pullRequestComments = new PullRequestComments();
    val newPullRequestComment   = new PullRequestComment("id_1", "my new comment", "author", Instant.now());
    pullRequestComments.setPullRequestComments(Lists.newArrayList(oldPRComment));

    when(dataStore.get(pullRequestId)).thenReturn(pullRequestComments);
    store.add(pullRequestId, newPullRequestComment);
    assertThat(backingMap)
      .isNotEmpty()
      .hasSize(1)
      .containsKeys(pullRequestId);
    assertThat(pullRequestComments.getPullRequestComments())
      .isNotEmpty()
      .containsExactly(oldPRComment, newPullRequestComment);
  }

}
