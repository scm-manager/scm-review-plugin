package com.cloudogu.scm.review.comment.service;

import com.cloudogu.scm.review.TestData;
import com.cloudogu.scm.review.pullrequest.service.PullRequestStore;
import com.cloudogu.scm.review.pullrequest.service.PullRequestStoreFactory;
import com.google.common.collect.Maps;
import org.apache.shiro.authz.AuthorizationException;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import sonia.scm.NotFoundException;
import sonia.scm.event.ScmEventBus;
import sonia.scm.repository.Repository;
import sonia.scm.security.KeyGenerator;
import sonia.scm.security.UUIDKeyGenerator;
import sonia.scm.store.DataStore;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CommentStoreTest {

  private final Map<String, PullRequestComments> backingMap = Maps.newHashMap();
  @Mock
  private DataStore<PullRequestComments> dataStore;

  private CommentStore store;
  private KeyGenerator keyGenerator = new UUIDKeyGenerator();
  @Mock
  private PullRequestStoreFactory pullRequestStoreFactory;

  @Mock
  private ScmEventBus eventBus;

  @Mock
  private Repository repository;

  @BeforeEach
  void init() {
    PullRequestStore prStore = mock(PullRequestStore.class);
    when(prStore.get(any())).thenReturn(TestData.createPullRequest());
    when(pullRequestStoreFactory.create(repository)).thenReturn(prStore);
    store = new CommentStore(dataStore, pullRequestStoreFactory, eventBus, keyGenerator);
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
    PullRequestComment pullRequestComment = new PullRequestComment("1", "my Comment", "author", new Location(), Instant.now(), false);
    store.add(repository, pullRequestId, pullRequestComment);
    assertThat(backingMap)
      .isNotEmpty()
      .hasSize(1)
      .containsKeys(pullRequestId);
  }

  @Test
  void shouldAddCommentToExistingCommentList() {
    String pullRequestId = "1";
    PullRequestComment oldPRComment = new PullRequestComment("1", "my comment", "author", new Location(), Instant.now(), false);
    PullRequestComments pullRequestComments = new PullRequestComments();
    PullRequestComment newPullRequestComment = new PullRequestComment("2", "my new comment", "author", new Location(), Instant.now(), false);
    pullRequestComments.setComments(Lists.newArrayList(oldPRComment));

    when(dataStore.get(pullRequestId)).thenReturn(pullRequestComments);
    store.add(repository, pullRequestId, newPullRequestComment);
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
    pullRequestComments.getComments().add(new PullRequestComment("1", "1. comment", "author", new Location(), Instant.now(), false));
    pullRequestComments.getComments().add(new PullRequestComment("2", "2. comment", "author", new Location(), Instant.now(), false));
    pullRequestComments.getComments().add(new PullRequestComment("3", "3. comment", "author", new Location(), Instant.now(), false));
    String pullRequestId = "id";
    when(dataStore.get(pullRequestId)).thenReturn(pullRequestComments);

    store.delete(repository, pullRequestId, "2");

    PullRequestComments comments = store.get(pullRequestId);
    assertThat(comments.getComments())
      .extracting("id")
      .containsExactly("1", "3");

    // delete a removed comment has no effect
    store.delete(repository, pullRequestId, "2");

    comments = store.get(pullRequestId);
    assertThat(comments.getComments())
      .extracting("id")
      .containsExactly("1", "3");

  }

  @Test
  void shouldUpdateAnExistingComment() {
    PullRequestComments pullRequestComments = new PullRequestComments();
    pullRequestComments.getComments().add(new PullRequestComment("1", "1. comment", "author", new Location(), Instant.now(), false));
    pullRequestComments.getComments().add(new PullRequestComment("2", "2. comment", "author", new Location(), Instant.now(), false));
    pullRequestComments.getComments().add(new PullRequestComment("3", "3. comment", "author", new Location(), Instant.now(), false));
    String pullRequestId = "id";
    when(dataStore.get(pullRequestId)).thenReturn(pullRequestComments);

    store.update(repository ,pullRequestId, "2", "new text");

    PullRequestComments comments = store.get(pullRequestId);
    assertThat(comments.getComments().stream().filter(c -> "2".equals(c.getId())))
      .extracting("comment")
      .containsExactly("new text");

    // delete a removed comment has no effect
    store.delete(repository, pullRequestId, "2");

    comments = store.get(pullRequestId);
    assertThat(comments.getComments())
      .extracting("id")
      .containsExactly("1", "3");

  }

  @Test
  void shouldGetPullRequestComments() {
    PullRequestComments pullRequestComments = new PullRequestComments();
    pullRequestComments.getComments().add(new PullRequestComment("1", "1. comment", "author", new Location(), Instant.now(), false));
    pullRequestComments.getComments().add(new PullRequestComment("2", "2. comment", "author", new Location(), Instant.now(), false));
    pullRequestComments.getComments().add(new PullRequestComment("3", "3. comment", "author", new Location(), Instant.now(), false));
    String pullRequestId = "id";
    when(dataStore.get(pullRequestId)).thenReturn(pullRequestComments);

    PullRequestComments comments = store.get(pullRequestId);
    assertThat(comments.getComments())
      .extracting("id")
      .containsExactly("1", "2", "3");
  }

  @Test
  void shouldThrowNotFoundException() {
    assertThrows(NotFoundException.class, () -> store.get("iDontExist"));
  }

  @Test
  void shouldThrowExceptionWhenDeletingSystemComment() {
    PullRequestComments pullRequestComments = new PullRequestComments();
    pullRequestComments.getComments().add(new PullRequestComment("1", "1. comment", "author", new Location(), Instant.now(), true));

    String pullRequestId = "id";
    when(dataStore.get(pullRequestId)).thenReturn(pullRequestComments);

    assertThrows(AuthorizationException.class,
      () -> store.delete(repository, pullRequestId, "1"));
  }

  @Test
  void shouldThrowExceptionWhenUpdatingSystemComment() {
    PullRequestComments pullRequestComments = new PullRequestComments();
    pullRequestComments.getComments().add(new PullRequestComment("1", "1. comment", "author", new Location(), Instant.now(), true));

    String pullRequestId = "id";
    when(dataStore.get(pullRequestId)).thenReturn(pullRequestComments);

    assertThrows(AuthorizationException.class,
      () -> store.update(repository, pullRequestId, "1", "new comment" ));
  }
}
