package com.cloudogu.scm.review;

import com.cloudogu.scm.review.comment.service.Comment;
import com.cloudogu.scm.review.comment.service.CommentCollector;
import com.cloudogu.scm.review.comment.service.CommentService;
import com.cloudogu.scm.review.comment.service.Location;
import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import com.cloudogu.scm.review.pullrequest.service.PullRequestCollector;
import com.cloudogu.scm.review.pullrequest.service.ReviewMark;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.repository.Changeset;
import sonia.scm.repository.PostReceiveRepositoryHookEvent;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryTestData;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FlagCommentsAsOutdatedHookTest {

  @Mock
  private PullRequestCollector pullRequestCollector;

  @Mock
  private CommentCollector commentCollector;

  @Mock
  private CommentService commentService;

  @Mock
  private ModificationCollector modificationCollector;

  @InjectMocks
  private FlagCommentsAsOutdatedHook hook;

  private Repository repository;

  private PullRequest pullRequest;

  @BeforeEach
  void setUpHookContext() {
    repository = RepositoryTestData.createHeartOfGold();
    pullRequest = TestData.createPullRequest();
  }

  @Test
  void shouldMarkAllGlobalCommentsAsOutdated() {
    Comment one = Comment.createSystemComment("awesome");
    Comment two = Comment.createSystemComment("with-location");
    two.setLocation(new Location("pom.xml", null, null, null));

    flagAffectedComments(one, two);

    assertThat(one.isOutdated()).isTrue();
    assertThat(two.isOutdated()).isFalse();

    verifyChanged(one);
  }

  @Test
  void shouldMarkAffectedFileCommentsAsOutdated() throws IOException {
    Comment one = Comment.createSystemComment("awesome");
    one.setLocation(new Location("some", null, null, null));
    Comment two = Comment.createSystemComment("with-location");
    two.setLocation(new Location("pom.xml", null, null, null));

    Set<String> modifications = ImmutableSet.of("pom.xml");

    when(modificationCollector.collect(eq(repository), any())).thenReturn(modifications);

    flagAffectedComments(one, two);

    assertThat(one.isOutdated()).isFalse();
    assertThat(two.isOutdated()).isTrue();

    verifyChanged(two);
  }

  @Test
  void shouldNotCollectModificationWithoutFileComments() throws IOException {
    Comment one = Comment.createSystemComment("awesome");

    flagAffectedComments(one);

    verify(modificationCollector, never()).collect(any(), any());
  }

  @Test
  void shouldRemoveReviewMarksForAffectedFiles() throws IOException {
    Set<String> modifications = ImmutableSet.of("pom.xml");
    when(modificationCollector.collect(eq(repository), any())).thenReturn(modifications);

    pullRequest.getReviewMarks().add(new ReviewMark("pom.xml", "dent"));

    flagAffectedComments();

    assertThat(pullRequest.getReviewMarks()).isEmpty();
  }

  @Test
  void shouldNotTouchReviewMarksForUnaffectedFiles() throws IOException {
    Set<String> modifications = ImmutableSet.of("pom.xml");
    when(modificationCollector.collect(eq(repository), any())).thenReturn(modifications);

    ReviewMark reviewMark = new ReviewMark("other.txt", "dent");
    pullRequest.getReviewMarks().add(reviewMark);

    flagAffectedComments();

    assertThat(pullRequest.getReviewMarks()).contains(reviewMark);
  }

  private void flagAffectedComments(Comment... comments) {
    PostReceiveRepositoryHookEvent event = prepareEvent(comments);

    hook.flagAffectedComments(event);
  }

  private void verifyChanged(Comment one) {
    verify(commentService).markAsOutdated(repository.getNamespace(), repository.getName(), pullRequest.getId(), one.getId());
  }

  private PostReceiveRepositoryHookEvent prepareEvent(Comment... comments) {
    when(commentCollector.collectNonOutdated(repository, pullRequest)).thenReturn(Stream.of(comments));
    List<String> branches = ImmutableList.of(pullRequest.getSource(), pullRequest.getTarget());
    PostReceiveRepositoryHookEvent event = createRepositoryHookEvent(branches);
    when(pullRequestCollector.collectAffectedPullRequests(repository, branches)).thenReturn(ImmutableList.of(pullRequest));
    List<Changeset> changesets = ImmutableList.of(
      new Changeset(),
      new Changeset()
    );
    when(event.getContext().getChangesetProvider().getChangesets()).thenReturn(changesets);
    return event;
  }

  private PostReceiveRepositoryHookEvent createRepositoryHookEvent(List<String> branches) {
    PostReceiveRepositoryHookEvent event = mock(PostReceiveRepositoryHookEvent.class, Answers.RETURNS_DEEP_STUBS);
    when(event.getRepository()).thenReturn(repository);
    when(event.getContext().getBranchProvider().getCreatedOrModified()).thenReturn(branches);
    return event;
  }


}
