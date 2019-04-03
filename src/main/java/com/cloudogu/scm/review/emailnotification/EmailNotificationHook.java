package com.cloudogu.scm.review.emailnotification;

import com.cloudogu.scm.review.comment.service.CommentEvent;
import com.cloudogu.scm.review.comment.service.PullRequestComment;
import com.cloudogu.scm.review.pullrequest.service.BasicPullRequestEvent;
import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import com.cloudogu.scm.review.pullrequest.service.PullRequestEvent;
import com.cloudogu.scm.review.pullrequest.service.PullRequestMergedEvent;
import com.cloudogu.scm.review.pullrequest.service.PullRequestRejectedEvent;
import com.github.legman.Subscribe;
import lombok.extern.slf4j.Slf4j;
import sonia.scm.EagerSingleton;
import sonia.scm.plugin.Extension;
import sonia.scm.repository.Repository;

import javax.inject.Inject;

@Slf4j
@EagerSingleton
@Extension
public class EmailNotificationHook {

  private final EmailNotificationService service;

  @Inject
  public EmailNotificationHook(EmailNotificationService service) {
    this.service = service;
  }

  @Subscribe
  public void handlePullRequestEvents(PullRequestEvent event) {
    handleEvent(event, new PullRequestEventEmailRenderer(event));
  }

  @Subscribe
  public void handleCommentEvents(CommentEvent event) {
    PullRequestComment comment = event.getItem();
    if (comment == null || !comment.isSystemComment()) {
      handleEvent(event, new CommentEventEmailRenderer(event));
    }
  }

  @Subscribe
  public void handleMergedPullRequest(PullRequestMergedEvent event) {
    handleEvent(event, new PullRequestMergedEmailRenderer(event));
  }

  @Subscribe
  public void handleRejectedPullRequest(PullRequestRejectedEvent event) {
    handleEvent(event, new PullRequestRejectedEmailRenderer(event));
  }

  private void handleEvent(BasicPullRequestEvent event, EmailRenderer emailRenderer) {
    PullRequest pullRequest = event.getPullRequest();
    Repository repository = event.getRepository();
    if (pullRequest == null || repository == null) {
      log.error("Repository or Pull Request not found in the event {}", event);
      return;
    }
    try {
      service.sendEmails(emailRenderer, pullRequest.getSubscriber(), pullRequest.getReviewer());
    } catch (Exception e) {
      log.warn("Error on sending Email", e);
    }
  }
}
