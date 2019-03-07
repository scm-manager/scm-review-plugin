package com.cloudogu.scm.review.comment.service;

import com.cloudogu.scm.review.RepositoryResolver;
import com.google.common.collect.Lists;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.subject.support.SubjectThreadState;
import org.apache.shiro.util.ThreadContext;
import org.apache.shiro.util.ThreadState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import sonia.scm.NotFoundException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CommentServiceTest {


  @Mock
  private RepositoryResolver repositoryResolver;
  @Mock
  private CommentStoreFactory storeFactory;

  @InjectMocks
  private CommentService commentService;

  @Mock
  private CommentStore store;


  private final Subject subject = mock(Subject.class);
  private final ThreadState subjectThreadState = new SubjectThreadState(subject);

  @BeforeEach
  void init() {
    subjectThreadState.bind();
    ThreadContext.bind(subject);
    when(storeFactory.create(any())).thenReturn(store);
  }

  @Test
  void shouldAddComment() {
    PullRequestComment comment = new PullRequestComment("1", "1. comment", "author", new Location() , Instant.now());
    String pullRequestId = "pr_id";
    commentService.add("ns", "name", pullRequestId, comment);
    verify(store).add(pullRequestId, comment);
  }


  @Test
  void shouldDeleteComment() {
    String pullRequestId = "pr_id";
    String commentId = "1";
    commentService.delete("ns", "name", pullRequestId, commentId);
    verify(store).delete(pullRequestId, commentId);
  }

  @Test
  void shouldGetAllComments() {
    ArrayList<PullRequestComment> list = Lists.newArrayList(
      new PullRequestComment("1", "1. comment", "author", new Location(), Instant.now()),
      new PullRequestComment("2", "2. comment", "author", new Location(), Instant.now()),
      new PullRequestComment("3", "3. comment", "author", new Location(), Instant.now()));

    String pullRequestId = "id";
    PullRequestComments pullRequestComments = new PullRequestComments();
    pullRequestComments.setComments(list);
    when(store.get(pullRequestId)).thenReturn(pullRequestComments);

    List<PullRequestComment> all = commentService.getAll("ns", "name", pullRequestId);

    assertThat(all).extracting("id").containsExactly("1", "2", "3");

  }

  @Test
  void shouldGetComment() {
    ArrayList<PullRequestComment> list = Lists.newArrayList(
      new PullRequestComment("1", "1. comment", "author", new Location(), Instant.now()),
      new PullRequestComment("2", "2. comment", "author", new Location(), Instant.now()),
      new PullRequestComment("3", "3. comment", "author", new Location(), Instant.now()));

    String pullRequestId = "id";
    PullRequestComments pullRequestComments = new PullRequestComments();
    pullRequestComments.setComments(list);
    when(store.get(pullRequestId)).thenReturn(pullRequestComments);

    PullRequestComment comment_1 = commentService.get("ns", "name", pullRequestId, "1");
    PullRequestComment comment_2 = commentService.get("ns", "name", pullRequestId, "2");
    PullRequestComment comment_3 = commentService.get("ns", "name", pullRequestId, "3");

    assertThat(comment_1).extracting("id").containsExactly("1");
    assertThat(comment_2).extracting("id").containsExactly("2");
    assertThat(comment_3).extracting("id").containsExactly("3");
  }

  @Test
  void shouldThrowNotFoundOnGettingMissedPR() {
    String pullRequestId = "id";
    when(store.get(pullRequestId)).thenThrow(NotFoundException.class);

    assertThrows(NotFoundException.class, () -> commentService.get("ns", "name", pullRequestId, "1"));
  }

  @Test
  void shouldThrowNotFoundOnGettingMissedComment() {
    ArrayList<PullRequestComment> list = Lists.newArrayList(
      new PullRequestComment("1", "1. comment", "author", new Location(), Instant.now()),
      new PullRequestComment("2", "2. comment", "author", new Location(), Instant.now()),
      new PullRequestComment("3", "3. comment", "author", new Location(), Instant.now()));
    String pullRequestId = "id";
    PullRequestComments pullRequestComments = new PullRequestComments();
    pullRequestComments.setComments(list);
    when(store.get(pullRequestId)).thenReturn(pullRequestComments);

    assertThrows(NotFoundException.class, () -> commentService.get("ns", "name", pullRequestId, "4"));
  }

}
