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
