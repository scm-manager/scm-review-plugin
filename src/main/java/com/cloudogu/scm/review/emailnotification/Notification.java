package com.cloudogu.scm.review.emailnotification;


/**
 * The Notification type of the address to be sent
 *
 * @author Mohamed Karray
 */
public enum Notification {
  MODIFIED_PULL_REQUEST ("modified_pull_request.mustache"),
  DELETED_COMMENT ("deleted_comment.mustache"),
  CREATED_COMMENT ("created_comment.mustache"),
  MODIFIED_COMMENT ("modified_comment.mustache"),
  MERGED_PULL_REQUEST ("merged_pull_request.mustache"),
  REJECTED_PULL_REQUEST ("rejected_pull_request.mustache");

  private String template;

  Notification(String template) {
    this.template = template;
  }

  public String getTemplate() {
    return template;
  }
}
