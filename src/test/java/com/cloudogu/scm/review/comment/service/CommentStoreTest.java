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
package com.cloudogu.scm.review.comment.service;

import com.cloudogu.scm.review.TestData;
import com.cloudogu.scm.review.pullrequest.service.PullRequestStore;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import sonia.scm.repository.Repository;
import sonia.scm.security.KeyGenerator;
import sonia.scm.security.UUIDKeyGenerator;
import sonia.scm.store.DataStore;

import java.util.Map;

import static com.cloudogu.scm.review.comment.service.Comment.createComment;
import static com.cloudogu.scm.review.comment.service.ContextLine.copy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CommentStoreTest {

  private final Map<String, PullRequestComments> backingMap = Maps.newHashMap();
  @Mock
  private DataStore<PullRequestComments> dataStore;

  private CommentStore store;
  private KeyGenerator keyGenerator = new UUIDKeyGenerator();

  @BeforeEach
  void init() {
    PullRequestStore prStore = mock(PullRequestStore.class);
    when(prStore.get(any())).thenReturn(TestData.createPullRequest());
    store = new CommentStore(dataStore, keyGenerator);
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
    String pullRequestId = "1";
    when(dataStore.get(pullRequestId)).thenReturn(null);
    Comment pullRequestComment = createComment("1", "my Comment", "author", new Location());
    store.add(pullRequestId, pullRequestComment);
    assertThat(backingMap)
      .isNotEmpty()
      .hasSize(1)
      .containsKeys(pullRequestId);
  }

  @Test
  void shouldAddCommentToExistingCommentList() {
    String pullRequestId = "1";
    Comment oldPRComment = createComment("1", "my comment", "author", new Location());
    PullRequestComments pullRequestComments = new PullRequestComments();
    Comment newPullRequestComment = createComment("2", "my new comment", "author", new Location());
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
  void shouldDeleteAnExistingComment() {
    PullRequestComments pullRequestComments = new PullRequestComments();
    pullRequestComments.getComments().add(createComment("1", "1. comment", "author", new Location()));
    pullRequestComments.getComments().add(createComment("2", "2. comment", "author", new Location()));
    pullRequestComments.getComments().add(createComment("3", "3. comment", "author", new Location()));
    String pullRequestId = "id";
    when(dataStore.get(pullRequestId)).thenReturn(pullRequestComments);

    store.delete(pullRequestId, "2");

    PullRequestComments comments = store.get(pullRequestId);
    assertThat(comments.getComments())
      .extracting("id")
      .containsExactly("1", "3");

    // delete a removed comment has no effect
    store.delete(pullRequestId, "2");

    comments = store.get(pullRequestId);
    assertThat(comments.getComments())
      .extracting("id")
      .containsExactly("1", "3");

  }

  @Test
  void shouldUpdateAnExistingComment() {
    PullRequestComments pullRequestComments = new PullRequestComments();
    pullRequestComments.getComments().add(createComment("1", "1. comment", "author", new Location()));
    Comment commentToChange = createComment("2", "2. comment", "author", new Location());
    pullRequestComments.getComments().add(commentToChange);
    pullRequestComments.getComments().add(createComment("3", "3. comment", "author", new Location()));
    String pullRequestId = "id";
    when(dataStore.get(pullRequestId)).thenReturn(pullRequestComments);

    Comment copy = commentToChange.clone();
    copy.setComment("new text");
    store.update(pullRequestId, copy);

    PullRequestComments comments = store.get(pullRequestId);
    assertThat(comments.getComments().stream().filter(c -> "2".equals(c.getId())))
      .extracting("comment")
      .containsExactly("new text");

    // delete a removed comment has no effect
    store.delete(pullRequestId, "2");

    comments = store.get(pullRequestId);
    assertThat(comments.getComments())
      .extracting("id")
      .containsExactly("1", "3");

  }

  @Test
  void shouldGetPullRequestComments() {
    PullRequestComments pullRequestComments = new PullRequestComments();
    pullRequestComments.getComments().add(createComment("1", "1. comment", "author", new Location()));
    pullRequestComments.getComments().add(createComment("2", "2. comment", "author", new Location()));
    pullRequestComments.getComments().add(createComment("3", "3. comment", "author", new Location()));
    String pullRequestId = "id";
    when(dataStore.get(pullRequestId)).thenReturn(pullRequestComments);

    PullRequestComments comments = store.get(pullRequestId);
    assertThat(comments.getComments())
      .extracting("id")
      .containsExactly("1", "2", "3");
  }

  @Test
  void shouldCreateNewStoreIfStoreIsMissing() {
    PullRequestComments newStore = store.get("iDontExist");

    assertThat(newStore).isNotNull();
    assertThat(newStore.getComments()).isEmpty();
  }

  @Test
  void shouldCreateNewCommentWithInlineContext() {
    String pullRequestId = "1";
    Comment comment = createComment("1", "1. comment", "author", new Location("file.txt", "123", null, 3));
    comment.setContext(new InlineContext(ImmutableList.of(
      copy(new MockedDiffLine.Builder().newLineNumber(1).get()),
      copy(new MockedDiffLine.Builder().newLineNumber(2).get()),
      copy(new MockedDiffLine.Builder().newLineNumber(3).get())
    )));

    store.add(pullRequestId, comment);

    Comment storedComment = backingMap.get(pullRequestId).getComments().iterator().next();
    assertThat(storedComment).isEqualTo(comment);
    assertThat(storedComment.getContext().getLines()).hasSize(3);
  }
}
