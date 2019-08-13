package com.cloudogu.scm.review;

import com.cloudogu.scm.review.comment.service.Comment;
import com.cloudogu.scm.review.comment.service.CommentCollector;
import com.cloudogu.scm.review.comment.service.CommentService;
import com.cloudogu.scm.review.comment.service.Location;
import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import com.cloudogu.scm.review.pullrequest.service.PullRequestCollector;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
  void shouldMarkAllGlobalCommentsAsOutdated() throws IOException {
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

    List<Changeset> changesets = ImmutableList.of(
      new Changeset(),
      new Changeset()
    );

    PostReceiveRepositoryHookEvent event = prepareEvent(one, two);
    when(event.getContext().getChangesetProvider().getChangesets()).thenReturn(changesets);

    Set<String> modifications = ImmutableSet.of("pom.xml");

    when(modificationCollector.collect(any(), any())).thenReturn(modifications);

    flagAffectedComments(one, two);

    assertThat(one.isOutdated()).isFalse();
    assertThat(two.isOutdated()).isTrue();

    verifyChanged(two);
  }

  @Test
  void shouldNotCollectModificationWithoutFileComments() throws IOException {
    Comment one = Comment.createSystemComment("awesome");

    List<Changeset> changesets = ImmutableList.of(
      new Changeset(),
      new Changeset()
    );

    PostReceiveRepositoryHookEvent event = prepareEvent(one);
    when(event.getContext().getChangesetProvider().getChangesets()).thenReturn(changesets);

    hook.flagAffectedComments(event);

    verify(modificationCollector, never()).collect(any(), any());
  }

  private void flagAffectedComments(Comment one, Comment two) throws IOException {
    PostReceiveRepositoryHookEvent event = prepareEvent(one, two);

    hook.flagAffectedComments(event);
  }

  private void verifyChanged(Comment one) {
    verify(commentService).modifyComment(repository.getNamespace(), repository.getName(), pullRequest.getId(), one.getId(), one);
  }

  private PostReceiveRepositoryHookEvent prepareEvent(Comment... comments) {
    List<String> branches = ImmutableList.of(pullRequest.getSource(), pullRequest.getTarget());
    PostReceiveRepositoryHookEvent event = createRepositoryHookEvent(branches);
    when(pullRequestCollector.collectAffectedPullRequests(repository, branches)).thenReturn(ImmutableList.of(pullRequest));
    when(commentCollector.collectNonOutdated(repository, pullRequest)).thenReturn(ImmutableList.copyOf(comments));
    return event;
  }

  private PostReceiveRepositoryHookEvent createRepositoryHookEvent(List<String> branches) {
    PostReceiveRepositoryHookEvent event = mock(PostReceiveRepositoryHookEvent.class, Answers.RETURNS_DEEP_STUBS);
    when(event.getRepository()).thenReturn(repository);
    when(event.getContext().getBranchProvider().getCreatedOrModified()).thenReturn(branches);
    return event;
  }


}
