package com.cloudogu.scm.review.emailnotification;

import com.cloudogu.scm.review.pullrequest.service.PullRequestApprovalEvent;
import lombok.extern.slf4j.Slf4j;

import java.util.Locale;
import java.util.Map;

@Slf4j
public class PullRequestApprovalMailTextResolver extends BasicPRMailTextResolver<PullRequestApprovalEvent> implements MailTextResolver {

  public static final String APPROVED_EVENT_DISPLAY_NAME = "prApproved";
  public static final String APPROVAL_REMOVED_EVENT_DISPLAY_NAME = "prApprovalRemoved";
  private final PullRequestApprovalEvent pullRequestApprovalEvent;
  protected static final String APPROVED_TEMPLATE_PATH = "com/cloudogu/scm/email/template/pull_request_approved.mustache";
  protected static final String APPROVAL_REMOVED_TEMPLATE_PATH = "com/cloudogu/scm/email/template/pull_request_approval_removed.mustache";

  public PullRequestApprovalMailTextResolver(PullRequestApprovalEvent pullRequestApprovalEvent) {
    this.pullRequestApprovalEvent = pullRequestApprovalEvent;
  }

  @Override
  public String getMailSubject(Locale locale) {
    return getMailSubject(pullRequestApprovalEvent, getMailSubjectKey(), locale);
  }

  private String getMailSubjectKey() {
    switch (pullRequestApprovalEvent.getCause()) {
      case APPROVED:
        return APPROVED_EVENT_DISPLAY_NAME;
      case APPROVAL_REMOVED:
        return APPROVAL_REMOVED_EVENT_DISPLAY_NAME;
      default:
        throw new IllegalArgumentException("unknown cause: " + pullRequestApprovalEvent.getCause());
    }
  }

  @Override
  public String getContentTemplatePath() {
    switch (pullRequestApprovalEvent.getCause()) {
      case APPROVED:
        return APPROVED_TEMPLATE_PATH;
      case APPROVAL_REMOVED:
        return APPROVAL_REMOVED_TEMPLATE_PATH;
      default:
        throw new IllegalArgumentException("unknown cause: " + pullRequestApprovalEvent.getCause());
    }
  }

  @Override
  public Map<String, Object> getContentTemplateModel(String basePath, boolean isReviewer) {
    return getTemplateModel(basePath, pullRequestApprovalEvent, isReviewer);
  }
}
