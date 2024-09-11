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

import com.cloudogu.scm.review.pullrequest.service.PullRequestMergedEvent;
import lombok.extern.slf4j.Slf4j;
import sonia.scm.mail.api.Topic;

import java.util.Locale;
import java.util.Map;

@Slf4j
public class PullRequestMergedMailTextResolver extends BasicPRMailTextResolver<PullRequestMergedEvent> implements MailTextResolver {

  public static final String EVENT_DISPLAY_NAME = "prMerged";
  private final PullRequestMergedEvent pullRequestMergedEvent;
  protected static final String TEMPLATE_PATH = "com/cloudogu/scm/email/template/merged_pull_request.mustache";

  public PullRequestMergedMailTextResolver(PullRequestMergedEvent pullRequestMergedEvent) {
    this.pullRequestMergedEvent = pullRequestMergedEvent;
  }

  @Override
  public String getMailSubject(Locale locale) {
    return getMailSubject(pullRequestMergedEvent, EVENT_DISPLAY_NAME, locale);
  }

  @Override
  public String getContentTemplatePath() {
    return TEMPLATE_PATH;
  }

  @Override
  public Map<String, Object> getContentTemplateModel(String basePath) {
    return getTemplateModel(basePath, pullRequestMergedEvent);
  }

  @Override
  public Topic getTopic() {
    return TOPIC_CLOSED;
  }

  @Override
  public String getPullRequestId() {
    return this.pullRequestMergedEvent.getPullRequest().getId();
  }
}
