package com.cloudogu.scm.review.pullrequest.service;

import com.cloudogu.scm.review.comment.service.CommentEvent;
import com.cloudogu.scm.review.comment.service.Location;
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
    Location location = event.getItem().getLocation();
    if (event.getEventType() == HandlerEventType.DELETE || location == null) {
      return;
    }

    Collection<ReviewMark> reviewMarksToBeRemoved = event.getPullRequest()
      .getReviewMarks()
      .stream()
      .filter(mark -> mark.getFile().equals(location.getFile()))
      .collect(Collectors.toList());

    service.removeReviewMarks(event.getRepository(), event.getPullRequest().getId(), reviewMarksToBeRemoved);
  }
}
