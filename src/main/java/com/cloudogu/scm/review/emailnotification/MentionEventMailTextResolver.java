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

import com.cloudogu.scm.review.comment.service.MentionEvent;
import lombok.extern.slf4j.Slf4j;
import sonia.scm.mail.api.Topic;

import java.util.Locale;
import java.util.Map;

@Slf4j
public class MentionEventMailTextResolver extends BasicPRMailTextResolver<MentionEvent> implements MailTextResolver {

  private final MentionEvent mentionEvent;

  public static final String EVENT_DISPLAY_NAME = "newMention";
  protected static final String EVENT_TEMPLATE_PATH = "com/cloudogu/scm/email/template/new_mention.mustache";


  public MentionEventMailTextResolver(MentionEvent mentionEvent) {
    this.mentionEvent = mentionEvent;
  }

  @Override
  public String getMailSubject(Locale locale) {
    return getMailSubject(mentionEvent, EVENT_DISPLAY_NAME, locale);
  }

  @Override
  public String getContentTemplatePath() {
    return EVENT_TEMPLATE_PATH;
  }

  @Override
  public Map<String, Object> getContentTemplateModel(String basePath) {
    Map<String, Object> model = getTemplateModel(basePath, mentionEvent);
    model.put("comment", mentionEvent.getItem());
    return model;
  }

  @Override
  public Topic getTopic() {
    return TOPIC_MENTIONS;
  }

  @Override
  public String getPullRequestId() {
    return mentionEvent.getPullRequest().getId();
  }
}
