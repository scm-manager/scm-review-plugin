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

import com.cloudogu.scm.review.pullrequest.service.PullRequestApprovalEvent;
import lombok.extern.slf4j.Slf4j;
import sonia.scm.mail.api.Topic;

import java.util.Locale;
import java.util.Map;

@Slf4j
public class PullRequestApprovalMailTextResolver extends BasicPRMailTextResolver<PullRequestApprovalEvent> implements MailTextResolver {

  public static final String APPROVED_EVENT_DISPLAY_NAME = "prApproved";
  public static final String APPROVAL_REMOVED_EVENT_DISPLAY_NAME = "prApprovalRemoved";
  private final PullRequestApprovalEvent pullRequestApprovalEvent;
  protected static final String APPROVED_TEMPLATE_PATH = "com/cloudogu/scm/email/template/pull_request_approved.mustache";
  protected static final String APPROVAL_REMOVED_TEMPLATE_PATH = "com/cloudogu/scm/email/template/pull_request_approval_removed.mustache";

  public PullRequestApprovalMailTextResolver(PullRequestApprovalEvent pullRequestApprovalEvent) {
    this.pullRequestApprovalEvent = pullRequestApprovalEvent;
  }

  @Override
  public String getMailSubject(Locale locale) {
    return getMailSubject(pullRequestApprovalEvent, getMailSubjectKey(), locale);
  }

  private String getMailSubjectKey() {
    switch (pullRequestApprovalEvent.getCause()) {
      case APPROVED:
        return APPROVED_EVENT_DISPLAY_NAME;
      case APPROVAL_REMOVED:
        return APPROVAL_REMOVED_EVENT_DISPLAY_NAME;
      default:
        throw new IllegalArgumentException("unknown cause: " + pullRequestApprovalEvent.getCause());
    }
  }

  @Override
  public String getContentTemplatePath() {
    switch (pullRequestApprovalEvent.getCause()) {
      case APPROVED:
        return APPROVED_TEMPLATE_PATH;
      case APPROVAL_REMOVED:
        return APPROVAL_REMOVED_TEMPLATE_PATH;
      default:
        throw new IllegalArgumentException("unknown cause: " + pullRequestApprovalEvent.getCause());
    }
  }

  @Override
  public Map<String, Object> getContentTemplateModel(String basePath, boolean isReviewer) {
    return getTemplateModel(basePath, pullRequestApprovalEvent, isReviewer);
  }

  @Override
  public Topic getTopic() {
    return TOPIC_APPROVALS;
  }
}
