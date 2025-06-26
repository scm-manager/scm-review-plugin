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
public class PullRequestEventMailTextResolver extends BasicPRMailTextResolver<PullRequestEvent> implements MailTextResolver {

  private final PullRequestEvent pullRequestEvent;
  private final boolean reviewer;
  private PullRequestEventType pullRequestEventType;

  public PullRequestEventMailTextResolver(PullRequestEvent pullRequestEvent, boolean reviewer) {
    this.pullRequestEvent = pullRequestEvent;
    this.reviewer = reviewer;
    try {
      pullRequestEventType = PullRequestEventType.valueOf(pullRequestEvent.getEventType().name());
    } catch (Exception e) {
      log.warn("the event " + pullRequestEvent.getEventType() + " is not supported for the Mail Renderer ");
    }
  }

  @Override
  public String getMailSubject(Locale locale) {

    return getMailSubject(pullRequestEvent, pullRequestEventType.getDisplayEventNameKey(), locale);
  }

  @Override
  public String getContentTemplatePath() {
    return pullRequestEventType.getTemplatePath();
  }

  @Override
  public Map<String, Object> getContentTemplateModel(String basePath) {
    Map<String, Object> model = super.getTemplateModel(basePath, pullRequestEvent);
    if (pullRequestEventType == PullRequestEventType.MODIFY) {
      model.put("oldPullRequest", pullRequestEvent.getOldItem());
      model.put("isReviewer", reviewer);
    }
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

  public boolean isCreated() {
    return this.pullRequestEventType == PullRequestEventType.CREATE;
  }

  private enum PullRequestEventType {

    CREATE("created_pull_request.mustache", "prCreated", HandlerEventType.CREATE),
    MODIFY("modified_pull_request.mustache", "prChanged", HandlerEventType.MODIFY);

    protected static final String PATH_BASE = "com/cloudogu/scm/email/template/";

    private final String template;
    private final String displayEventNameKey;
    private final HandlerEventType type;


    PullRequestEventType(String template, String displayEventNameKey, HandlerEventType type) {
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
