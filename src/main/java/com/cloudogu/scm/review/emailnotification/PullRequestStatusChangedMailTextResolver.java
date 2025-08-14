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

import com.cloudogu.scm.review.pullrequest.service.PullRequestEvent;
import lombok.extern.slf4j.Slf4j;
import sonia.scm.HandlerEventType;
import sonia.scm.mail.api.Topic;

import java.util.Locale;
import java.util.Map;

@Slf4j
public class PullRequestStatusChangedMailTextResolver extends BasicPRMailTextResolver<PullRequestEvent> implements MailTextResolver {
  private final PullRequestEvent pullRequestEvent;
  private final boolean reviewer;
  private final PullRequestStatusType pullRequestStatusType;

  public PullRequestStatusChangedMailTextResolver(PullRequestEvent pullRequestEvent, PullRequestStatusType pullRequestStatusType, boolean reviewer) {
    this.pullRequestEvent = pullRequestEvent;
    this.reviewer = reviewer;
    this.pullRequestStatusType = pullRequestStatusType;
  }

  @Override
  public String getMailSubject(Locale locale) {

    return getMailSubject(pullRequestEvent, pullRequestStatusType.getDisplayEventNameKey(), locale);
  }

  @Override
  public String getContentTemplatePath() {
    return pullRequestStatusType.getTemplatePath();
  }

  @Override
  public Map<String, Object> getContentTemplateModel(String basePath) {
    Map<String, Object> model = super.getTemplateModel(basePath, pullRequestEvent);
    model.put("oldPullRequest", pullRequestEvent.getOldItem());
    model.put("isReviewer", reviewer);
    return model;
  }

  @Override
  public Topic getTopic() {
    return TOPIC_PR_CHANGED;
  }

  @Override
  public String getPullRequestId() {
    return this.pullRequestEvent.getPullRequest().getId();
  }

  public enum PullRequestStatusType {
    TO_OPEN("status_to_open_pull_request.mustache", "prStatusToOpen", HandlerEventType.CREATE),
    TO_DRAFT("status_to_draft_pull_request.mustache", "prStatusToDraft", HandlerEventType.MODIFY);

    private final String template;
    private final String displayEventNameKey;
    private final HandlerEventType type;

    static final String PATH_BASE = "com/cloudogu/scm/email/template/";

    PullRequestStatusType(String template, String displayEventNameKey, HandlerEventType type) {
      this.template = template;
      this.displayEventNameKey = displayEventNameKey;
      this.type = type;
    }

    public String getTemplatePath() {
      return PATH_BASE + template;
    }

    public String getDisplayEventNameKey() {
      return displayEventNameKey;
    }

    public HandlerEventType getType() {
      return type;
    }
  }


}
