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
package com.cloudogu.scm.review.emailnotification;

import com.cloudogu.scm.review.comment.service.CommentEvent;
import com.cloudogu.scm.review.comment.service.MentionEvent;
import com.cloudogu.scm.review.comment.service.Reply;
import com.cloudogu.scm.review.comment.service.ReplyEvent;
import com.cloudogu.scm.review.emailnotification.PullRequestStatusChangedMailTextResolver.PullRequestStatusType;
import com.cloudogu.scm.review.pullrequest.service.BasicPullRequestEvent;
import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import com.cloudogu.scm.review.pullrequest.service.PullRequestApprovalEvent;
import com.cloudogu.scm.review.pullrequest.service.PullRequestEvent;
import com.cloudogu.scm.review.pullrequest.service.PullRequestMergedEvent;
import com.cloudogu.scm.review.pullrequest.service.PullRequestRejectedEvent;
import com.cloudogu.scm.review.pullrequest.service.PullRequestReopenedEvent;
import com.cloudogu.scm.review.pullrequest.service.PullRequestStatus;
import com.cloudogu.scm.review.pullrequest.service.PullRequestUpdatedMailEvent;
import com.github.legman.Subscribe;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.SecurityUtils;
import sonia.scm.EagerSingleton;
import sonia.scm.HandlerEventType;
import sonia.scm.mail.api.MailSendBatchException;
import sonia.scm.plugin.Extension;
import sonia.scm.repository.Repository;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static java.util.stream.Collectors.toSet;

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
    PullRequest pullRequest = event.getPullRequest();
    EMailRecipientHelper eMailRecipientHelper = new EMailRecipientHelper(pullRequest);
    Set<String> subscriberWithoutReviewers = eMailRecipientHelper.getSubscriberWithoutReviewers();
    Set<String> reviewers = eMailRecipientHelper.getSubscribingReviewers();

    if (event.getEventType().equals(HandlerEventType.CREATE)) {
      handleEventIfNotDraft(event, new PullRequestEventMailTextResolver(event, false), pullRequest, subscriberWithoutReviewers);
      handleEventIfNotDraft(event, new PullRequestEventMailTextResolver(event, true), pullRequest, reviewers);
    } else if (isDraftToOpen(event)) {
      handleEvent(event, new PullRequestStatusChangedMailTextResolver(event, PullRequestStatusType.TO_OPEN, false), pullRequest, subscriberWithoutReviewers);
      handleEvent(event, new PullRequestStatusChangedMailTextResolver(event, PullRequestStatusType.TO_OPEN, true), pullRequest, reviewers);
    } else if (isOpenToDraft(event)) {
      handleEvent(event, new PullRequestStatusChangedMailTextResolver(event, PullRequestStatusType.TO_DRAFT, false), pullRequest, subscriberWithoutReviewers);
      handleEvent(event, new PullRequestStatusChangedMailTextResolver(event, PullRequestStatusType.TO_DRAFT, true), pullRequest, reviewers);
    } else if (isRelevantChangeForMail(event)) {
      handleEventIfNotDraft(event, new PullRequestEventMailTextResolver(event, false), pullRequest, subscriberWithoutReviewers);
      handleEventIfNotDraft(event, new PullRequestEventMailTextResolver(event, true), pullRequest, reviewers);
    }
  }

  private boolean isDraftToOpen(PullRequestEvent event) {
    return event.getPullRequest().isOpen() && event.getOldItem().isDraft();
  }

  private boolean isOpenToDraft(PullRequestEvent event) {
    return event.getPullRequest().isDraft() && event.getOldItem().isOpen();
  }

  private boolean isRelevantChangeForMail(PullRequestEvent event) {
    return !event.getPullRequest().getTitle().equals(event.getOldItem().getTitle()) ||
      !event.getPullRequest().getDescription().equals(event.getOldItem().getDescription()) ||
      !event.getPullRequest().getTarget().equals(event.getOldItem().getTarget());
  }

  @Subscribe
  public void handleCommentEvents(CommentEvent event) {
    if (!isSystemComment(event)) {
      PullRequest pullRequest = event.getPullRequest();
      handleEventIfNotDraft(event, new CommentEventMailTextResolver(event), pullRequest, getSubscribersWithoutCurrentUser(pullRequest));
    }
  }

  @Subscribe
  public void handleReplyEvents(ReplyEvent event) {
    PullRequest pullRequest = event.getPullRequest();
    Set<String> authorsInThread = getAuthorsInThread(event);
    Set<String> subscribers = getSubscribersWithoutCurrentUser(pullRequest);
    subscribers.removeAll(authorsInThread);
    handleEventIfNotDraft(event, new CommentEventMailTextResolver(event), pullRequest, subscribers);
    handleEventIfNotDraft(event, new ReplyEventMailTextResolver(event), pullRequest, authorsInThread);
  }

  private Set<String> getAuthorsInThread(ReplyEvent replyEvent) {
    Set<String> involvedUsers = new HashSet<>();
    involvedUsers.add(replyEvent.getRootComment().getAuthor());
    replyEvent.getRootComment().getReplies().stream().map(Reply::getAuthor).forEach(involvedUsers::add);
    return filterCurrentUser(involvedUsers);
  }

  @Subscribe
  public void handleMentionEvents(MentionEvent event) throws MailSendBatchException {
    Set<String> newMentions;
    if (event.getOldItem() != null) {
      newMentions = new HashSet<>(event.getItem().getMentionUserIds());
      newMentions.removeAll(event.getOldItem().getMentionUserIds());
    } else {
      newMentions = event.getItem().getMentionUserIds();
    }
    MailTextResolver mailTextResolver = new MentionEventMailTextResolver(event);
    service.sendEmail(newMentions, mailTextResolver);
    service.sendEmail(Collections.emptySet(), mailTextResolver);
  }

  private boolean isSystemComment(CommentEvent event) {
    if (event.getEventType() == HandlerEventType.DELETE) {
      return event.getOldItem().isSystemComment();
    } else {
      return event.getItem().isSystemComment();
    }
  }

  @Subscribe
  public void handleUpdatedPullRequest(PullRequestUpdatedMailEvent event) {
    PullRequest pullRequest = event.getPullRequest();
    handleEventIfNotDraft(event, new PullRequestUpdatedMailTextResolver(event), pullRequest, getSubscribersWithoutCurrentUser(pullRequest));
  }

  @Subscribe
  public void handleMergedPullRequest(PullRequestMergedEvent event) {
    PullRequest pullRequest = event.getPullRequest();
    handleEventIfNotDraft(event, new PullRequestMergedMailTextResolver(event), pullRequest, getSubscribersWithoutCurrentUser(pullRequest));
  }

  @Subscribe
  public void handleRejectedPullRequest(PullRequestRejectedEvent event) {
    PullRequest pullRequest = event.getPullRequest();
    handleEventIfNotDraft(event, new PullRequestRejectedMailTextResolver(event), pullRequest, getSubscribersWithoutCurrentUser(pullRequest));
  }

  @Subscribe
  public void handleReopenedPullRequest(PullRequestReopenedEvent event) {
    PullRequest pullRequest = event.getPullRequest();
    handleEvent(event, new PullRequestReopenedMailTextResolver(event), pullRequest, getSubscribersWithoutCurrentUser(pullRequest));
  }

  @Subscribe
  public void handlePullRequestApproval(PullRequestApprovalEvent event) {
    PullRequest pullRequest = event.getPullRequest();
    handleEventIfNotDraft(event, new PullRequestApprovalMailTextResolver(event), pullRequest, getSubscribersWithoutCurrentUser(pullRequest));
  }

  private void handleEventIfNotDraft(BasicPullRequestEvent event, MailTextResolver mailTextResolver, PullRequest pullRequest, Set<String> recipients) {
    if (event.getPullRequest() != null && event.getPullRequest().getStatus() == PullRequestStatus.DRAFT) {
      // Do not send updates for draft pull requests
      return;
    }

    handleEvent(event, mailTextResolver, pullRequest, recipients);
  }

  private void handleEvent(BasicPullRequestEvent event, MailTextResolver mailTextResolver, PullRequest pullRequest, Set<String> recipients) {
    Repository repository = event.getRepository();
    if (pullRequest == null || repository == null) {
      log.warn("Repository or Pull Request not found in the event {}", event);
      return;
    }
    try {
      service.sendEmail(recipients, mailTextResolver);
    } catch (Exception e) {
      log.warn("Error on sending Email", e);
    }
  }

  private Set<String> getSubscribersWithoutCurrentUser(PullRequest pullRequest) {
    return filterCurrentUser(pullRequest.getSubscriber());
  }

  private Set<String> filterCurrentUser(Set<String> users) {
    String currentUser = SecurityUtils.getSubject().getPrincipal().toString();
    if (users.contains(currentUser)) {
      return users.stream().filter(s -> !s.equals(currentUser)).collect(toSet());
    }
    return users;
  }
}
