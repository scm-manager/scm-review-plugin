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
import com.cloudogu.scm.review.pullrequest.service.BasicPullRequestEvent;
import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import com.cloudogu.scm.review.pullrequest.service.PullRequestApprovalEvent;
import com.cloudogu.scm.review.pullrequest.service.PullRequestEvent;
import com.cloudogu.scm.review.pullrequest.service.PullRequestMergedEvent;
import com.cloudogu.scm.review.pullrequest.service.PullRequestRejectedEvent;
import com.github.legman.Subscribe;
import lombok.extern.slf4j.Slf4j;
import sonia.scm.EagerSingleton;
import sonia.scm.HandlerEventType;
import sonia.scm.mail.api.MailSendBatchException;
import sonia.scm.plugin.Extension;
import sonia.scm.repository.Repository;

import javax.inject.Inject;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

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
    handleEvent(event, new PullRequestEventMailTextResolver(event));
  }

  @Subscribe
  public void handleCommentEvents(CommentEvent event) {
    if (!isSystemComment(event)) {
      handleEvent(event, new CommentEventMailTextResolver(event));
    }
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
    service.sendEmails(new MentionEventMailTextResolver(event), newMentions, Collections.emptySet());
  }

  private boolean isSystemComment(CommentEvent event) {
    if (event.getEventType() == HandlerEventType.DELETE) {
      return event.getOldItem().isSystemComment();
    } else {
      return event.getItem().isSystemComment();
    }
  }

  @Subscribe
  public void handleMergedPullRequest(PullRequestMergedEvent event) {
    handleEvent(event, new PullRequestMergedMailTextResolver(event));
  }

  @Subscribe
  public void handleRejectedPullRequest(PullRequestRejectedEvent event) {
    handleEvent(event, new PullRequestRejectedMailTextResolver(event));
  }

  @Subscribe
  public void handlePullRequestApproval(PullRequestApprovalEvent event) {
    handleEvent(event, new PullRequestApprovalMailTextResolver(event));
  }

  private void handleEvent(BasicPullRequestEvent event, MailTextResolver mailTextResolver) {
    PullRequest pullRequest = event.getPullRequest();
    Repository repository = event.getRepository();
    if (pullRequest == null || repository == null) {
      log.error("Repository or Pull Request not found in the event {}", event);
      return;
    }
    try {
      service.sendEmails(mailTextResolver, pullRequest.getSubscriber(), pullRequest.getReviewer().keySet());
    } catch (Exception e) {
      log.warn("Error on sending Email", e);
    }
  }
}
