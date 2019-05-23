package com.cloudogu.scm.review.emailnotification;

import com.cloudogu.scm.review.pullrequest.service.PullRequestMergedEvent;
import lombok.extern.slf4j.Slf4j;

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
  public Map<String, Object> getContentTemplateModel(String basePath, boolean isReviewer) {
    return getTemplateModel(basePath, pullRequestMergedEvent, isReviewer);
  }

}
