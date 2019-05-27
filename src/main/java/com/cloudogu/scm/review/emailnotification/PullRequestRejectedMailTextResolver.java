package com.cloudogu.scm.review.emailnotification;

import com.cloudogu.scm.review.pullrequest.service.PullRequestRejectedEvent;
import lombok.extern.slf4j.Slf4j;

import java.util.Locale;
import java.util.Map;

@Slf4j
public class PullRequestRejectedMailTextResolver extends BasicPRMailTextResolver<PullRequestRejectedEvent> implements MailTextResolver {

  public static final String EVENT_DISPLAY_NAME = "prRejected";
  private final PullRequestRejectedEvent pullRequestRejectedEvent;
  protected static final String TEMPLATE_PATH = "com/cloudogu/scm/email/template/rejected_pull_request.mustache";

  public PullRequestRejectedMailTextResolver(PullRequestRejectedEvent pullRequestRejectedEvent) {
    this.pullRequestRejectedEvent = pullRequestRejectedEvent;
  }

  @Override
  public String getMailSubject(Locale locale) {
    return getMailSubject(pullRequestRejectedEvent, EVENT_DISPLAY_NAME, locale);
  }

  @Override
  public String getContentTemplatePath() {
    return TEMPLATE_PATH;
  }

  @Override
  public Map<String, Object> getContentTemplateModel(String basePath, boolean isReviewer) {
    return getTemplateModel(basePath, pullRequestRejectedEvent, isReviewer);
  }


}
