package com.cloudogu.scm.review.emailnotification;

import com.cloudogu.scm.review.pullrequest.service.PullRequestEvent;
import lombok.extern.slf4j.Slf4j;
import sonia.scm.HandlerEventType;
import sonia.scm.template.Template;
import sonia.scm.template.TemplateEngine;
import sonia.scm.template.TemplateEngineFactory;

import java.io.IOException;
import java.util.Map;

@Slf4j
public class PullRequestEventEmailRenderer extends BasicPREmailRenderer<PullRequestEvent> implements EmailRenderer {

  private final PullRequestEvent pullRequestEvent;
  private PullRequestEventType pullRequestEventType;

  public PullRequestEventEmailRenderer(PullRequestEvent pullRequestEvent) {
    this.pullRequestEvent = pullRequestEvent;
    try {
      pullRequestEventType = PullRequestEventType.valueOf(pullRequestEvent.getEventType().name());
    } catch (Exception e) {
      log.warn("the event " + pullRequestEvent.getEventType() + " is not supported for the Mail Renderer ");
    }
  }


  @Override
  public String getMailSubject() {
    return getMailSubject(pullRequestEvent, pullRequestEventType.getDisplayEventName());
  }

  @Override
  public String getMailContent(String basePath, TemplateEngineFactory templateEngineFactory) throws IOException {
    String path = pullRequestEventType.getTemplatePath();
    TemplateEngine templateEngine = templateEngineFactory.getEngineByExtension(path);
    Template template = templateEngine.getTemplate(path);

    return getMailContent(basePath, pullRequestEvent, template);
  }


  @Override
  public Map<String, Object> getTemplateModel(String basePath, PullRequestEvent event) {
    Map<String, Object> model = super.getTemplateModel(basePath, event);
    if (pullRequestEventType == PullRequestEventType.MODIFY) {
      model.put("oldPullRequest", event.getOldItem());
    }
    return model;
  }

  private enum PullRequestEventType {

    CREATE("created_pull_request.mustache", "Pull request created", HandlerEventType.CREATE),
    MODIFY("modified_pull_request.mustache", "Pull request modified", HandlerEventType.MODIFY);

    protected static final String PATH_BASE = "com/cloudogu/scm/email/template/";

    private String template;
    private String displayEventName;
    private HandlerEventType type;


    PullRequestEventType(String template, String displayEventName, HandlerEventType type) {
      this.template = template;
      this.displayEventName = displayEventName;
      this.type = type;
    }

    public String getTemplatePath() {
      return PATH_BASE + template;
    }

    public String getDisplayEventName() {
      return displayEventName;
    }

    public HandlerEventType getType() {
      return type;
    }
  }

}
