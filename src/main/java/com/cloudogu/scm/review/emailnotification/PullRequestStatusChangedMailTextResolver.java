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
