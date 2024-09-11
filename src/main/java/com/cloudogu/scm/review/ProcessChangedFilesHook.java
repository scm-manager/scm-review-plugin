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
import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import com.cloudogu.scm.review.pullrequest.service.PullRequestCollector;
import com.cloudogu.scm.review.pullrequest.service.PullRequestService;
import com.cloudogu.scm.review.pullrequest.service.ReviewMark;
import com.github.legman.Subscribe;
import com.google.common.base.Strings;
import com.google.inject.Inject;
import sonia.scm.ContextEntry;
import sonia.scm.EagerSingleton;
import sonia.scm.plugin.Extension;
import sonia.scm.repository.Changeset;
import sonia.scm.repository.InternalRepositoryException;
import sonia.scm.repository.PostReceiveRepositoryHookEvent;
import sonia.scm.repository.Repository;

import java.io.IOException;
import java.util.List;
import java.util.Set;

@Extension
@EagerSingleton
public class ProcessChangedFilesHook {

  private final PullRequestCollector pullRequestCollector;
  private final CommentCollector commentCollector;
  private final ModificationCollector modificationCollector;
  private final CommentService commentService;
  private final PullRequestService pullRequestService;


  @Inject
  ProcessChangedFilesHook(PullRequestCollector pullRequestCollector, CommentCollector commentCollector, ModificationCollector modificationCollector, CommentService commentService, PullRequestService pullRequestService) {
    this.pullRequestCollector = pullRequestCollector;
    this.commentCollector = commentCollector;
    this.modificationCollector = modificationCollector;
    this.commentService = commentService;
    this.pullRequestService = pullRequestService;
  }

  @Subscribe
  public void checkChangedFiles(PostReceiveRepositoryHookEvent event) {
    if (!pullRequestService.supportsPullRequests(event.getRepository())) {
      return;
    }
    CachingModificationCollector collector = new CachingModificationCollector(event);
    FlagAffectedCommentsAsOutdated flagAffectedCommentsAsOutdated = new FlagAffectedCommentsAsOutdated(event.getRepository(), collector);
    RemoveReviewMarksForChangedFiles removeReviewMarksForChangedFiles = new RemoveReviewMarksForChangedFiles(event.getRepository(), collector);

    List<PullRequest> affectedPullRequests = getAffectedPullRequests(event);

    affectedPullRequests.forEach(flagAffectedCommentsAsOutdated::process);
    affectedPullRequests.forEach(removeReviewMarksForChangedFiles::process);
  }

  private List<PullRequest> getAffectedPullRequests(PostReceiveRepositoryHookEvent event) {
    List<String> affectedBranches = event.getContext().getBranchProvider().getCreatedOrModified();
    return pullRequestCollector.collectAffectedPullRequests(event.getRepository(), affectedBranches);
  }

  private class FlagAffectedCommentsAsOutdated {

    private final Repository repository;
    private final CachingModificationCollector collector;

    FlagAffectedCommentsAsOutdated(Repository repository, CachingModificationCollector collector) {
      this.repository = repository;
      this.collector = collector;
    }

    void process(PullRequest pullRequest) {
      commentCollector.collectNonOutdated(repository, pullRequest)
        .filter(comment -> isGlobalComment(comment) || isAffectedFileComment(comment))
        .forEach(comment -> flagAsOutdated(repository, pullRequest, comment));
    }

    private boolean isAffectedFileComment(Comment comment) {
      return comment.getLocation() != null &&
        !Strings.isNullOrEmpty(comment.getLocation().getFile()) &&
        collector.collect().contains(comment.getLocation().getFile());
    }

    private boolean isGlobalComment(Comment comment) {
      return comment.getLocation() == null || Strings.isNullOrEmpty(comment.getLocation().getFile());
    }

    private void flagAsOutdated(Repository repository, PullRequest pullRequest, Comment comment) {
      commentService.markAsOutdated(repository.getNamespace(), repository.getName(), pullRequest.getId(), comment.getId());
    }
  }

  private class RemoveReviewMarksForChangedFiles {

    private final Repository repository;
    private final CachingModificationCollector collector;

    public RemoveReviewMarksForChangedFiles(Repository repository, CachingModificationCollector collector) {
      this.repository = repository;
      this.collector = collector;
    }

    void process(PullRequest pullRequest) {
      List<ReviewMark> marksToBeRemoved =
        pullRequest.getReviewMarks()
          .stream()
          .filter(reviewMark -> collector.collect().contains(reviewMark.getFile()))
          .toList();
      if (!marksToBeRemoved.isEmpty()) {
        pullRequestService.removeReviewMarks(repository, pullRequest.getId(), marksToBeRemoved);
      }
    }
  }

  private class CachingModificationCollector {

    private final Repository repository;
    private final Iterable<Changeset> changesets;

    private Set<String> modifications;

    private CachingModificationCollector(PostReceiveRepositoryHookEvent event) {
      repository = event.getRepository();
      changesets = event.getContext().getChangesetProvider().getChangesets();
    }

    Set<String> collect() {
      if (modifications == null) {
        try {
          modifications = modificationCollector.collect(repository, changesets);
        } catch (IOException ex) {
          throw new InternalRepositoryException(
            ContextEntry.ContextBuilder.entity(repository).build(),
            "failed to collect modifications",
            ex
          );
        }
      }
      return modifications;
    }
  }
}
