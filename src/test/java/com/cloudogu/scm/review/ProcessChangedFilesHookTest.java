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
