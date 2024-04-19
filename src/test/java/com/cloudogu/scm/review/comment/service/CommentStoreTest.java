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
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import sonia.scm.security.KeyGenerator;
import sonia.scm.security.UUIDKeyGenerator;
import sonia.scm.store.DataStore;
import sonia.scm.store.DataStoreFactory;
import sonia.scm.store.InMemoryByteDataStoreFactory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import static com.cloudogu.scm.review.comment.service.ContextLine.copy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CommentStoreTest {

  private CommentStore store;
  private DataStoreFactory dataStoreFactory = new InMemoryByteDataStoreFactory();
  private DataStore<PullRequestComments> dataStore = dataStoreFactory.withType(PullRequestComments.class).withName("comments").build();
  private KeyGenerator keyGenerator = new UUIDKeyGenerator();

  @BeforeEach
  void init() {
    PullRequestStore prStore = mock(PullRequestStore.class);
    when(prStore.get(any())).thenReturn(TestData.createPullRequest());
    store = new CommentStore(dataStore, keyGenerator);
  }

  @Test
  void shouldAddTheFirstComment() {
    String pullRequestId = "1";
    Comment pullRequestComment = createComment("1", "my Comment", "author", new Location());

    store.add(pullRequestId, pullRequestComment);

    assertThat(dataStore.getAll())
      .hasSize(1)
      .containsKeys(pullRequestId);
  }

  @Test
  void shouldAddCommentToExistingCommentList() {
    String pullRequestId = "1";
    Comment oldPRComment = createComment("1", "my comment", "author", new Location());
    PullRequestComments pullRequestComments = new PullRequestComments();
    pullRequestComments.setComments(Lists.newArrayList(oldPRComment));
    dataStore.put(pullRequestId, pullRequestComments);

    Comment newPullRequestComment = createComment("2", "my new comment", "author", new Location());
    store.add(pullRequestId, newPullRequestComment);

    List<Comment> comments = dataStore.get(pullRequestId).getComments();
    assertThat(comments.get(0))
      .usingRecursiveComparison()
      .isEqualTo(oldPRComment);
    assertThat(comments.get(1))
      .usingRecursiveComparison()
      .isEqualTo(newPullRequestComment);
  }

  @Test
  void shouldDeleteAnExistingComment() {
    PullRequestComments pullRequestComments = new PullRequestComments();
    pullRequestComments.getComments().add(createComment("1", "1. comment", "author", new Location()));
    pullRequestComments.getComments().add(createComment("2", "2. comment", "author", new Location()));
    pullRequestComments.getComments().add(createComment("3", "3. comment", "author", new Location()));
    String pullRequestId = "id";
    dataStore.put(pullRequestId, pullRequestComments);

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
    dataStore.put(pullRequestId, pullRequestComments);

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
    dataStore.put(pullRequestId, pullRequestComments);

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

    Comment storedComment = dataStore.get(pullRequestId).getComments().get(0);
    assertThat(storedComment).usingRecursiveComparison().isEqualTo(comment);
    assertThat(storedComment.getContext().getLines()).hasSize(3);
  }

  @Test
  void shouldStoreSystemCommentsWithParameters() {
    String pullRequestId = "1";
    Comment comment = Comment.createSystemComment("system", Map.of("ship", "Heart of Gold", "captain", "Zaphod Beeblebrox"));
    comment.setDate(Instant.now().truncatedTo(ChronoUnit.SECONDS)); // we have to truncate this because the date is serialized to xml and the xml adapter does not support nanoseconds

    store.add(pullRequestId, comment);

    Comment storedComment = dataStore.get(pullRequestId).getComments().get(0);
    assertThat(storedComment).usingRecursiveComparison().isEqualTo(comment);
  }

  Comment createComment(String id, String text, String author, Location location) {
    Comment comment = Comment.createComment(id, text, author, location);
    comment.setDate(Instant.now().truncatedTo(ChronoUnit.SECONDS)); // we have to truncate this because the date is serialized to xml and the xml adapter does not support nanoseconds
    return comment;
  }
}
