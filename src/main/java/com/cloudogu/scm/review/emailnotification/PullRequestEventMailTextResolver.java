/**
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

import com.cloudogu.scm.review.pullrequest.service.PullRequestEvent;
import lombok.extern.slf4j.Slf4j;
import sonia.scm.HandlerEventType;

import java.util.Locale;
import java.util.Map;

@Slf4j
public class PullRequestEventMailTextResolver extends BasicPRMailTextResolver<PullRequestEvent> implements MailTextResolver {

  private final PullRequestEvent pullRequestEvent;
  private PullRequestEventType pullRequestEventType;

  public PullRequestEventMailTextResolver(PullRequestEvent pullRequestEvent) {
    this.pullRequestEvent = pullRequestEvent;
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
  public Map<String, Object> getContentTemplateModel(String basePath, boolean isReviewer) {
    Map<String, Object> model = super.getTemplateModel(basePath, pullRequestEvent, isReviewer);
    if (pullRequestEventType == PullRequestEventType.MODIFY) {
      model.put("oldPullRequest", pullRequestEvent.getOldItem());
    }
    return model;
  }

  private enum PullRequestEventType {

    CREATE("created_pull_request.mustache", "prCreated", HandlerEventType.CREATE),
    MODIFY("modified_pull_request.mustache", "prChanged", HandlerEventType.MODIFY);

    protected static final String PATH_BASE = "com/cloudogu/scm/email/template/";

    private String template;
    private String displayEventNameKey;
    private HandlerEventType type;


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
