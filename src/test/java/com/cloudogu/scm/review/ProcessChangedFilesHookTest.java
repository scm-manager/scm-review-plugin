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

package com.cloudogu.scm.review;

import com.cloudogu.scm.review.comment.service.Comment;
import com.cloudogu.scm.review.comment.service.CommentCollector;
import com.cloudogu.scm.review.comment.service.CommentService;
import com.cloudogu.scm.review.comment.service.Location;
import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import com.cloudogu.scm.review.pullrequest.service.PullRequestCollector;
import com.cloudogu.scm.review.pullrequest.service.PullRequestService;
import com.cloudogu.scm.review.pullrequest.service.ReviewMark;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
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

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcessChangedFilesHookTest {

  @Mock
  private PullRequestCollector pullRequestCollector;

  @Mock
  private CommentCollector commentCollector;

  @Mock
  private CommentService commentService;
  @Mock
  private PullRequestService pullRequestService;

  @Mock
  private ModificationCollector modificationCollector;

  @InjectMocks
  private ProcessChangedFilesHook hook;

  private Repository repository = RepositoryTestData.createHeartOfGold();

  private PullRequest pullRequest = TestData.createPullRequest();

  @Nested
  class WithPullRequestSupport {
    @BeforeEach
    void setUpHookContext() {
      when(pullRequestService.supportsPullRequests(repository)).thenReturn(true);
    }

    @Test
    void shouldMarkAllGlobalCommentsAsOutdated() {
      Comment one = Comment.createComment("1", "awesome", null, null);
      Comment two = Comment.createComment("2", "with-location", null, null);
      two.setLocation(new Location("pom.xml", null, null, null));

      flagAffectedComments(one, two);

      verify(commentService).markAsOutdated(repository.getNamespace(), repository.getName(), pullRequest.getId(), one.getId());
      verify(commentService, never()).markAsOutdated(repository.getNamespace(), repository.getName(), pullRequest.getId(), two.getId());
    }

    @Test
    void shouldMarkAffectedFileCommentsAsOutdated() throws IOException {
      Comment one = Comment.createComment("1", "awesome", null, new Location("some"));
      Comment two = Comment.createComment("2", "with-location", null, new Location("pom.xml"));

      Set<String> modifications = ImmutableSet.of("pom.xml");

      when(modificationCollector.collect(eq(repository), any())).thenReturn(modifications);

      flagAffectedComments(one, two);

      verify(commentService, never()).markAsOutdated(repository.getNamespace(), repository.getName(), pullRequest.getId(), one.getId());
      verify(commentService).markAsOutdated(repository.getNamespace(), repository.getName(), pullRequest.getId(), two.getId());
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

      ReviewMark reviewMark = new ReviewMark("pom.xml", "dent");
      pullRequest.getReviewMarks().add(reviewMark);

      flagAffectedComments();

      verify(pullRequestService).removeReviewMarks(repository, "id", asList(reviewMark));
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
  }

  @Nested
  class WithoutPullRequestSupport {
    @BeforeEach
    void setUpHookContext() {
      repository = RepositoryTestData.createHeartOfGold();
      when(pullRequestService.supportsPullRequests(repository)).thenReturn(false);
    }

    @Test
    void shouldDoNothing() throws IOException {
      flagAffectedComments(Comment.createComment("2", "with-location", null, new Location("pom.xml")));
      verify(modificationCollector, never()).collect(any(), any());
    }
  }

  private void flagAffectedComments(Comment... comments) {
    PostReceiveRepositoryHookEvent event = prepareEvent(comments);

    hook.checkChangedFiles(event);
  }

  private PostReceiveRepositoryHookEvent prepareEvent(Comment... comments) {
    lenient().when(commentCollector.collectNonOutdated(repository, pullRequest)).thenReturn(Stream.of(comments));
    List<String> branches = ImmutableList.of(pullRequest.getSource(), pullRequest.getTarget());
    PostReceiveRepositoryHookEvent event = createRepositoryHookEvent(branches);
    lenient().when(pullRequestCollector.collectAffectedPullRequests(repository, branches)).thenReturn(ImmutableList.of(pullRequest));
    List<Changeset> changesets = ImmutableList.of(
      new Changeset(),
      new Changeset()
    );
    lenient().when(event.getContext().getChangesetProvider().getChangesets()).thenReturn(changesets);
    return event;
  }

  private PostReceiveRepositoryHookEvent createRepositoryHookEvent(List<String> branches) {
    PostReceiveRepositoryHookEvent event = mock(PostReceiveRepositoryHookEvent.class, Answers.RETURNS_DEEP_STUBS);
    when(event.getRepository()).thenReturn(repository);
    when(event.getContext().getBranchProvider().getCreatedOrModified()).thenReturn(branches);
    return event;
  }


}
