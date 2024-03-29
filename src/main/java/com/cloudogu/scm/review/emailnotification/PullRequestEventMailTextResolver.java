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

import com.cloudogu.scm.review.pullrequest.service.PullRequestEvent;
import lombok.Getter;
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
