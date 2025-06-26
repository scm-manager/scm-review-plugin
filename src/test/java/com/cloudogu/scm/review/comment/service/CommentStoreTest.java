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

package com.cloudogu.scm.review.comment.service;

import com.cloudogu.scm.review.TestData;
import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import com.cloudogu.scm.review.pullrequest.service.PullRequestStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import sonia.scm.security.KeyGenerator;
import sonia.scm.store.QueryableMutableStore;
import sonia.scm.store.QueryableStore;
import sonia.scm.store.QueryableStoreExtension;
import sonia.scm.store.QueryableStoreFactory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import static com.cloudogu.scm.review.comment.service.ContextLine.copy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, QueryableStoreExtension.class})
@MockitoSettings(strictness = Strictness.LENIENT)
@QueryableStoreExtension.QueryableTypes({PullRequest.class, Comment.class})
class CommentStoreTest {

  private int nextId = 1;

  private CommentStore store;
  private KeyGenerator keyGenerator = () -> String.valueOf(nextId++);

  @BeforeEach
  void init(QueryableStoreFactory storeFactory) {
    PullRequestStore prStore = mock(PullRequestStore.class);
    when(prStore.get(any())).thenReturn(TestData.createPullRequest());
    store = new CommentStore(pullRequestId -> storeFactory.getMutable(Comment.class, "hog", pullRequestId), keyGenerator);
  }

  @Test
  void shouldAddTheFirstComment(QueryableStoreFactory storeFactory) {
    String pullRequestId = "1";
    Comment pullRequestComment = createComment("1", "my Comment", "author", new Location());

    store.add(pullRequestId, pullRequestComment);

    try (QueryableStore<Comment> store = storeFactory.getReadOnly(Comment.class, "hog")) {
      List<QueryableStore.Result<Comment>> all = store.query().withIds().findAll();
      assertThat(all)
        .hasSize(1);
      assertThat(all.get(0).getParentId(PullRequest.class))
        .contains(pullRequestId);
    }
  }

  @Test
  void shouldAddCommentToExistingCommentList(CommentStoreFactory storeFactory) {
    String pullRequestId = "1";
    Comment oldPRComment = createComment("0", "my comment", "author", new Location());
    try (QueryableMutableStore<Comment> store = storeFactory.getMutable("hog", pullRequestId)) {
      store.put("0", oldPRComment);
    }

    Comment newPullRequestComment = createComment("0", "my new comment", "author", new Location());
    this.store.add(pullRequestId, newPullRequestComment);

    try (QueryableStore<Comment> store = storeFactory.get("hog", pullRequestId)) {
      List<Comment> comments = store.query().findAll();
      assertThat(comments.get(0))
        .usingRecursiveComparison()
        .isEqualTo(oldPRComment);
      assertThat(comments.get(1))
        .usingRecursiveComparison()
        .isEqualTo(newPullRequestComment);
    }
  }

  @Test
  void shouldDeleteAnExistingComment() {
    String pullRequestId = "id";
    store.add(pullRequestId, createComment("1", "1. comment", "author", new Location()));
    store.add(pullRequestId, createComment("2", "2. comment", "author", new Location()));
    store.add(pullRequestId, createComment("3", "3. comment", "author", new Location()));

    store.delete(pullRequestId, "2");

    List<Comment> comments = store.getAll(pullRequestId);
    assertThat(comments)
      .extracting("id")
      .containsExactly("1", "3");

    // delete a removed comment has no effect
    store.delete(pullRequestId, "2");

    comments = store.getAll(pullRequestId);
    assertThat(comments)
      .extracting("id")
      .containsExactly("1", "3");

  }

  @Test
  void shouldUpdateAnExistingComment() {
    String pullRequestId = "id";
    store.add(pullRequestId, createComment("1", "1. comment", "author", new Location()));
    Comment commentToChange = createComment("2", "2. comment", "author", new Location());
    store.add(pullRequestId, commentToChange);
    store.add(pullRequestId, createComment("3", "3. comment", "author", new Location()));

    Comment copy = commentToChange.clone();
    copy.setComment("new text");
    store.update(pullRequestId, copy);

    List<Comment> comments = store.getAll(pullRequestId);
    assertThat(comments.stream().filter(c -> "2".equals(c.getId())))
      .extracting("comment")
      .containsExactly("new text");

    // delete a removed comment has no effect
    store.delete(pullRequestId, "2");

    comments = store.getAll(pullRequestId);
    assertThat(comments)
      .extracting("id")
      .containsExactly("1", "3");

  }

  @Test
  void shouldGetPullRequestComments() {
    String pullRequestId = "id";
    store.add(pullRequestId, createComment("1", "1. comment", "author", new Location()));
    store.add(pullRequestId, createComment("2", "2. comment", "author", new Location()));
    store.add(pullRequestId, createComment("3", "3. comment", "author", new Location()));

    List<Comment> comments = store.getAll(pullRequestId);
    assertThat(comments)
      .extracting("id")
      .containsExactly("1", "2", "3");
  }

  @Test
  void shouldCreateNewStoreIfStoreIsMissing() {
    List<Comment> newStore = store.getAll("iDontExist");

    assertThat(newStore)
      .isNotNull()
      .isEmpty();
  }

  @Test
  void shouldCreateNewCommentWithInlineContext() {
    String pullRequestId = "1";
    Comment comment = createComment("1", "1. comment", "author", new Location("file.txt", "123", null, 3));
    comment.setContext(new InlineContext(List.of(
      copy(new MockedDiffLine.Builder().newLineNumber(1).get()),
      copy(new MockedDiffLine.Builder().newLineNumber(2).get()),
      copy(new MockedDiffLine.Builder().newLineNumber(3).get())
    )));

    store.add(pullRequestId, comment);

    Comment storedComment = store.getAll(pullRequestId).get(0);
    assertThat(storedComment).usingRecursiveComparison().isEqualTo(comment);
    assertThat(storedComment.getContext().getLines()).hasSize(3);
  }

  @Test
  void shouldStoreSystemCommentsWithParameters() {
    String pullRequestId = "1";
    Comment comment = Comment.createSystemComment("system", Map.of("ship", "Heart of Gold", "captain", "Zaphod Beeblebrox"));
    comment.setDate(Instant.now().truncatedTo(ChronoUnit.SECONDS)); // we have to truncate this because the date is serialized to xml and the xml adapter does not support nanoseconds

    String newId = store.add(pullRequestId, comment);
    comment.setId(newId);

    Comment storedComment = store.getAll(pullRequestId).get(0);
    assertThat(storedComment).usingRecursiveComparison().isEqualTo(comment);
  }

  Comment createComment(String id, String text, String author, Location location) {
    Comment comment = Comment.createComment(id, text, author, location);
    comment.setDate(Instant.now().truncatedTo(ChronoUnit.SECONDS)); // we have to truncate this because the date is serialized to xml and the xml adapter does not support nanoseconds
    return comment;
  }
}
