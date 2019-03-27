package com.cloudogu.scm.review.emailnotification;

import sonia.scm.template.TemplateEngineFactory;

import java.io.IOException;

public interface EmailRenderer {

  String getMailSubject();

  String getMailContent(String basePath, TemplateEngineFactory templateEngineFactory) throws IOException;
}
