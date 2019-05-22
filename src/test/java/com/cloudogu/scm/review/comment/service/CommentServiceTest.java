package com.cloudogu.scm.review.comment.service;

import com.cloudogu.scm.review.RepositoryResolver;
import com.cloudogu.scm.review.pullrequest.service.PullRequestStore;
import com.cloudogu.scm.review.pullrequest.service.PullRequestStoreFactory;
import com.google.common.collect.Lists;
import org.apache.shiro.subject.PrincipalCollection;
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
import sonia.scm.repository.Repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
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

  @Mock
  private PullRequestStoreFactory pullRequestStoreFactory;

  @InjectMocks
  private CommentService commentService;

  @Mock
  private CommentStore store;

  @Mock
  private PullRequestStore prStore;

  @Mock
  private Repository repository;

  private final Subject subject = mock(Subject.class);
  private final ThreadState subjectThreadState = new SubjectThreadState(subject);

  @BeforeEach
  void init() {
    subjectThreadState.bind();
    ThreadContext.bind(subject);
    when(pullRequestStoreFactory.create(any())).thenReturn(prStore);
    when(storeFactory.create(any())).thenReturn(store);
  }

  @Test
  void shouldAddComment() {
    PullRequestComment comment = new PullRequestComment("123", "1", "1. comment", "author", new Location(), Instant.now(), false, false);
    String pullRequestId = "pr_id";
    commentService.add(repository, pullRequestId, comment);
    verify(store).add(repository, pullRequestId, comment);
  }

 @Test
  void shouldAddChangedStatusComment() {
   PrincipalCollection p = mock(PrincipalCollection.class);
   when(subject.getPrincipals()).thenReturn(p);
   when(p.getPrimaryPrincipal()).thenReturn("scm user");
    commentService.addStatusChangedComment(repository, "pr_1", SystemCommentType.MERGED);
    verify(store).add(eq(repository),eq("pr_1"), argThat(t -> {
      assertThat(t.getComment()).isEqualTo("merged");
      assertThat(t.getAuthor()).isEqualTo("scm user");
      assertThat(t.getDate()).isNotNull();
      assertThat(t.isSystemComment()).isTrue();
      return true;
    }));
  }


  @Test
  void shouldDeleteComment() {
    String pullRequestId = "pr_id";
    String commentId = "1";
    commentService.delete(repository, pullRequestId, commentId);
    verify(store).delete(repository, pullRequestId, commentId);
  }

  @Test
  void shouldGetAllComments() {
    ArrayList<PullRequestComment> list = Lists.newArrayList(
      new PullRequestComment("123", "1", "1. comment", "author", new Location(), Instant.now(), false, false),
      new PullRequestComment("123", "2", "2. comment", "author", new Location(), Instant.now(), false, false),
      new PullRequestComment("123", "3", "3. comment", "author", new Location(), Instant.now(), false, false));

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
      new PullRequestComment("123", "1", "1. comment", "author", new Location(), Instant.now(), false, false),
      new PullRequestComment("123", "2", "2. comment", "author", new Location(), Instant.now(), false, false),
      new PullRequestComment("123", "3", "3. comment", "author", new Location(), Instant.now(), false, false));

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
      new PullRequestComment("123", "1", "1. comment", "author", new Location(), Instant.now(), false, false),
      new PullRequestComment("123", "2", "2. comment", "author", new Location(), Instant.now(), false, false),
      new PullRequestComment("123", "3", "3. comment", "author", new Location(), Instant.now(), false, false));
    String pullRequestId = "id";
    PullRequestComments pullRequestComments = new PullRequestComments();
    pullRequestComments.setComments(list);
    when(store.get(pullRequestId)).thenReturn(pullRequestComments);

    assertThrows(NotFoundException.class, () -> commentService.get("ns", "name", pullRequestId, "4"));
  }

}
