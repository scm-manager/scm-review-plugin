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
  public Map<String, Object> getContentTemplateModel(String basePath) {
    return getTemplateModel(basePath, pullRequestApprovalEvent);
  }

  @Override
  public Topic getTopic() {
    return TOPIC_APPROVALS;
  }

  @Override
  public String getPullRequestId() {
    return this.pullRequestApprovalEvent.getPullRequest().getId();
  }

}
