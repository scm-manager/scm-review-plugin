package com.cloudogu.scm.review.emailnotification;

import com.cloudogu.scm.review.pullrequest.service.PullRequestMergedEvent;
import lombok.extern.slf4j.Slf4j;
import sonia.scm.template.Template;
import sonia.scm.template.TemplateEngine;
import sonia.scm.template.TemplateEngineFactory;

import java.io.IOException;

@Slf4j
public class PullRequestMergedEmailRenderer extends BasicPREmailRenderer<PullRequestMergedEvent> implements EmailRenderer {

  public static final String EVENT_DISPLAY_NAME = "PR merged";
  private final PullRequestMergedEvent pullRequestMergedEvent;
  protected static final String TEMPLATE_PATH = "com/cloudogu/scm/email/template/merged_pull_request.mustache";

  public PullRequestMergedEmailRenderer(PullRequestMergedEvent pullRequestMergedEvent) {
    this.pullRequestMergedEvent = pullRequestMergedEvent;
  }


  @Override
  public String getMailSubject() {
    return getMailSubject(pullRequestMergedEvent, EVENT_DISPLAY_NAME);
  }

  @Override
  public String getMailContent(String basePath, TemplateEngineFactory templateEngineFactory) throws IOException {
    TemplateEngine templateEngine = templateEngineFactory.getEngineByExtension(TEMPLATE_PATH);
    Template template = templateEngine.getTemplate(TEMPLATE_PATH);

    return getMailContent(basePath, pullRequestMergedEvent, template);
  }

}
