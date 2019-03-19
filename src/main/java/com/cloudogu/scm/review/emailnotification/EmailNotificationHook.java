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
import sonia.scm.HandlerEventType;
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
    Repository repository = event.getRepository();
    if (repository == null){
      log.error("Repository is not found in the pull request event {}", event.getEventType());
      return ;
    }
    EmailContext emailContext = new EmailContext();

    PullRequest pullRequest = event.getItem();
    PullRequest oldPullRequest = event.getOldItem();

    emailContext.setPullRequest(pullRequest);
    emailContext.setOldPullRequest(oldPullRequest);

    emailContext.setRepository(event.getRepository());

    if (event.getEventType() == HandlerEventType.MODIFY && pullRequest != null && oldPullRequest != null) {
      emailContext.setRecipients(pullRequest.getSubscriber());
      service.sendEmail(emailContext, Notification.MODIFIED_PULL_REQUEST);
    }
  }

  @Subscribe
  public void handleCommentEvents(CommentEvent event) {
    EmailContext emailContext = getEmailContext(event);
    if (emailContext == null) {
      return;
    }

    PullRequestComment comment = event.getItem();
    PullRequestComment oldComment = event.getOldItem();
    emailContext.setComment(comment);
    emailContext.setOldComment(oldComment);

    if (event.getEventType() == HandlerEventType.CREATE && comment != null) {
      emailContext.setComment(comment);
      service.sendEmail(emailContext, Notification.CREATED_COMMENT);
    } else if (event.getEventType() == HandlerEventType.MODIFY && comment != null && oldComment != null) {
      service.sendEmail(emailContext, Notification.MODIFIED_COMMENT);

    } else if (event.getEventType() == HandlerEventType.DELETE && oldComment != null) {
      service.sendEmail(emailContext, Notification.DELETED_COMMENT);
    }
  }

  @Subscribe
  public void handleMergedPullRequest(PullRequestMergedEvent event) {
    EmailContext emailContext = getEmailContext(event);
    if (emailContext == null){
      return;
    }
    service.sendEmail(emailContext, Notification.MERGED_PULL_REQUEST);
  }

  @Subscribe
  public void handleRejectedPullRequest(PullRequestRejectedEvent event) {
    EmailContext emailContext = getEmailContext(event);
    if (emailContext == null){
      return;
    }
    service.sendEmail(emailContext, Notification.REJECTED_PULL_REQUEST);
  }

  private EmailContext getEmailContext(BasicPullRequestEvent event) {
    PullRequest pullRequest = event.getPullRequest();
    Repository repository = event.getRepository();
    if (pullRequest == null || repository == null){
      log.error("Repository or Pull Request not found in the event");
      return null;
    }
    EmailContext emailContext = new EmailContext();
    emailContext.setRepository(repository);
    emailContext.setPullRequest(pullRequest);
    emailContext.setRecipients(pullRequest.getSubscriber());
    return emailContext;
  }
}
