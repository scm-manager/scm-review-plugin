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

import com.cloudogu.scm.review.pullrequest.service.PullRequestUpdatedMailEvent;
import sonia.scm.mail.api.Topic;

import java.util.Locale;
import java.util.Map;

public class PullRequestUpdatedMailTextResolver extends BasicPRMailTextResolver<PullRequestUpdatedMailEvent> implements MailTextResolver {

  public static final String EVENT_DISPLAY_NAME = "prUpdated";
  private final PullRequestUpdatedMailEvent pullRequestUpdatedMailEvent;
  protected static final String TEMPLATE_PATH = "com/cloudogu/scm/email/template/updated_pull_request.mustache";

  public PullRequestUpdatedMailTextResolver(PullRequestUpdatedMailEvent pullRequestUpdatedMailEvent) {
    this.pullRequestUpdatedMailEvent = pullRequestUpdatedMailEvent;
  }

  @Override
  public String getMailSubject(Locale locale) {
    return getMailSubject(pullRequestUpdatedMailEvent, EVENT_DISPLAY_NAME, locale);
  }

  @Override
  public String getContentTemplatePath() {
    return TEMPLATE_PATH;
  }

  @Override
  public Map<String, Object> getContentTemplateModel(String basePath) {
    return getTemplateModel(basePath, pullRequestUpdatedMailEvent);
  }

  @Override
  public Topic getTopic() {
    return TOPIC_PR_UPDATED;
  }
}
