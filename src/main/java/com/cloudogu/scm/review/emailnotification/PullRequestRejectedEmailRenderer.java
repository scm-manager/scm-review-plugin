package com.cloudogu.scm.review.emailnotification;

import com.cloudogu.scm.review.pullrequest.service.PullRequestRejectedEvent;
import lombok.extern.slf4j.Slf4j;
import sonia.scm.template.Template;
import sonia.scm.template.TemplateEngine;
import sonia.scm.template.TemplateEngineFactory;

import java.io.IOException;

@Slf4j
public class PullRequestRejectedEmailRenderer extends BasicPREmailRenderer<PullRequestRejectedEvent> implements EmailRenderer {

  public static final String EVENT_DISPLAY_NAME = "Pull request rejected";
  private final PullRequestRejectedEvent pullRequestRejectedEvent;
  protected static final String TEMPLATE_PATH = "com/cloudogu/scm/email/template/rejected_pull_request.mustache";

  public PullRequestRejectedEmailRenderer(PullRequestRejectedEvent pullRequestRejectedEvent) {
    this.pullRequestRejectedEvent = pullRequestRejectedEvent;
  }


  @Override
  public String getMailSubject() {
    return getMailSubject(pullRequestRejectedEvent, EVENT_DISPLAY_NAME);
  }

  @Override
  public String getMailContent(String basePath, TemplateEngineFactory templateEngineFactory) throws IOException {
    TemplateEngine templateEngine = templateEngineFactory.getEngineByExtension(TEMPLATE_PATH);
    Template template = templateEngine.getTemplate(TEMPLATE_PATH);

    return getMailContent(basePath, pullRequestRejectedEvent, template);
  }

}
