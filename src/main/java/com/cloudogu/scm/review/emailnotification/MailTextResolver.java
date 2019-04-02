package com.cloudogu.scm.review.emailnotification;

import java.util.Map;

public interface MailTextResolver {

  String getMailSubject();

  String getContentTemplatePath();

  Map<String, Object> getContentTemplateModel(String basePath, boolean isReviewer);
}
