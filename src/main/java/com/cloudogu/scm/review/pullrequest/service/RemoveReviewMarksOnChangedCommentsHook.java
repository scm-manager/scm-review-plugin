package com.cloudogu.scm.review.pullrequest.service;

import com.cloudogu.scm.review.comment.service.CommentEvent;
import com.github.legman.Subscribe;
import sonia.scm.EagerSingleton;
import sonia.scm.HandlerEventType;
import sonia.scm.plugin.Extension;

import javax.inject.Inject;
import java.util.Collection;
import java.util.stream.Collectors;

@EagerSingleton
@Extension
public class RemoveReviewMarksOnChangedCommentsHook {

  private final PullRequestService service;

  @Inject
  public RemoveReviewMarksOnChangedCommentsHook(PullRequestService service) {
    this.service = service;
  }

  @Subscribe
  public void handleCommentEvents(CommentEvent event) {
    if (event.getEventType() == HandlerEventType.DELETE || event.getItem().getLocation() == null) {
      return;
    }

    Collection<ReviewMark> reviewMarksToBeRemoved = event.getPullRequest()
      .getReviewMarks()
      .stream()
      .filter(mark -> mark.getFile().equals(event.getItem().getLocation().getFile()))
      .collect(Collectors.toList());

    service.removeReviewMarks(event.getRepository(), event.getPullRequest().getId(), reviewMarksToBeRemoved);
  }
}
