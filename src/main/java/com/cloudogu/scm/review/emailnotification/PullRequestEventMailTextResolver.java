package com.cloudogu.scm.review.emailnotification;

import com.cloudogu.scm.review.pullrequest.service.PullRequestEvent;
import lombok.extern.slf4j.Slf4j;
import sonia.scm.HandlerEventType;

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
  public String getMailSubject() {
    return getMailSubject(pullRequestEvent, pullRequestEventType.getDisplayEventName());
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

    CREATE("created_pull_request.mustache", "PR created", HandlerEventType.CREATE),
    MODIFY("modified_pull_request.mustache", "PR changed", HandlerEventType.MODIFY);

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
