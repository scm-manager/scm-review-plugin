package com.cloudogu.scm.review;

import com.cloudogu.scm.review.comment.service.Comment;
import com.cloudogu.scm.review.comment.service.CommentService;
import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import com.github.legman.Subscribe;
import com.google.common.base.Strings;
import com.google.inject.Inject;
import sonia.scm.EagerSingleton;
import sonia.scm.plugin.Extension;
import sonia.scm.repository.Changeset;
import sonia.scm.repository.PostReceiveRepositoryHookEvent;
import sonia.scm.repository.Repository;

import java.io.IOException;
import java.util.List;
import java.util.Set;

@Extension
@EagerSingleton
public class FlagCommentsAsOutdatedHook {

  private final PullRequestCollector pullRequestCollector;
  private final CommentCollector commentCollector;
  private final CommentService commentService;
  private final ModificationCollector modificationCollector;

  @Inject
  public FlagCommentsAsOutdatedHook(PullRequestCollector pullRequestCollector, CommentCollector commentCollector, CommentService commentService, ModificationCollector modificationCollector) {
    this.pullRequestCollector = pullRequestCollector;
    this.commentCollector = commentCollector;
    this.commentService = commentService;
    this.modificationCollector = modificationCollector;
  }

  @Subscribe
  void flagAffectedComments(PostReceiveRepositoryHookEvent event) throws IOException {
    Repository repository = event.getRepository();
    List<String> affectedBranches = event.getContext().getBranchProvider().getCreatedOrModified();
    List<PullRequest> pullRequests = pullRequestCollector.collectAffectedPullRequests(repository, affectedBranches);
    Iterable<Changeset> changesets = event.getContext().getChangesetProvider().getChangesets();
    Set<String> modifications = modificationCollector.collect(repository, changesets);
    for (PullRequest pullRequest : pullRequests) {
      commentCollector.collectNonOutdated(repository, pullRequest)
        .stream()
        .filter(comment -> isGlobalComment(comment) || isAffectedFileComment(modifications, comment))
        .forEach(comment -> flagAsOutdated(repository, pullRequest, comment));
    }
  }

  private boolean isAffectedFileComment(Set<String> modifications, Comment pullRequestRootComment) {
    return pullRequestRootComment.getLocation() != null &&
      !Strings.isNullOrEmpty(pullRequestRootComment.getLocation().getFile()) &&
      modifications.contains(pullRequestRootComment.getLocation().getFile());
  }

  private void flagAsOutdated(Repository repository, PullRequest pr, Comment comment) {
    comment.setOutdated(true);
    commentService.modifyComment(repository.getNamespace(), repository.getName(), pr.getId(), comment.getId(), comment);
  }

  private boolean isGlobalComment(Comment comment) {
    return comment.getLocation() == null || Strings.isNullOrEmpty(comment.getLocation().getFile());
  }
}
