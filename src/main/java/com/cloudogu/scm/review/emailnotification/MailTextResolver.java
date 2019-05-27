package com.cloudogu.scm.review.emailnotification;

import java.util.Locale;
import java.util.Map;

public interface MailTextResolver {

  String getMailSubject(Locale locale);

  String getContentTemplatePath();

  Map<String, Object> getContentTemplateModel(String basePath, boolean isReviewer);
}
