package com.cloudogu.scm.review.emailnotification;

import org.apache.shiro.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sonia.scm.config.ScmConfiguration;
import sonia.scm.mail.api.MailSendBatchException;
import sonia.scm.mail.api.MailService;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class EmailNotificationService {

  private static final Logger LOG = LoggerFactory.getLogger(EmailNotificationService.class);

  private final MailService mailService;
  private final ScmConfiguration configuration;

  @Inject
  public EmailNotificationService(MailService mailService, ScmConfiguration configuration) {
    this.mailService = mailService;
    this.configuration = configuration;
  }

  public void sendEmails(MailTextResolver mailTextResolver, Set<String> subscriber, Set<String> reviewer) throws IOException, MailSendBatchException {
    if (!mailService.isConfigured()) {
      LOG.warn("cannot send Email because the mail server is not configured");
      return;
    }

    String emailSubject = mailTextResolver.getMailSubject();
    Object principal = SecurityUtils.getSubject().getPrincipal();

    Set<String> subscriberWithoutReviewers = subscriber.stream()
      .filter(recipient -> !reviewer.contains(recipient))
      .filter(recipient -> !recipient.equals(principal))
      .collect(Collectors.toSet());

    Set<String> subscribingReviewers = subscriber.stream()
      .filter(reviewer::contains)
      .filter(recipient -> !recipient.equals(principal))
      .collect(Collectors.toSet());

    String path = mailTextResolver.getContentTemplatePath();
    if (!subscriberWithoutReviewers.isEmpty()) {
      Map<String, Object> templateModel = mailTextResolver.getContentTemplateModel(configuration.getBaseUrl(), false);
      sendEmails(subscriberWithoutReviewers, emailSubject, path, templateModel);
    }

    if (!subscribingReviewers.isEmpty()){
      Map<String, Object> reviewerTemplateModel = mailTextResolver.getContentTemplateModel(configuration.getBaseUrl(), true);
      sendEmails(subscribingReviewers, emailSubject, path, reviewerTemplateModel);
    }
  }

  private void sendEmails(Set<String> recipients, String emailSubject, String templatePath, Map<String, Object> templateModel) throws IOException, MailSendBatchException {
    MailService.EnvelopeBuilder envelopeBuilder = mailService.emailTemplateBuilder()
      .fromCurrentUser();
    recipients.forEach(envelopeBuilder::toUser);
    envelopeBuilder.withSubject(emailSubject)
      .withTemplate(templatePath)
      .andModel(templateModel)
      .send();
  }

}
