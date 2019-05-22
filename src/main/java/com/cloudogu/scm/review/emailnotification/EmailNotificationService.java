package com.cloudogu.scm.review.emailnotification;

import org.apache.shiro.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sonia.scm.config.ScmConfiguration;
import sonia.scm.mail.api.MailSendBatchException;
import sonia.scm.mail.api.MailService;
import sonia.scm.mail.api.MailTemplateType;

import javax.inject.Inject;
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

  public void sendEmails(MailTextResolver mailTextResolver, Set<String> subscriber, Set<String> reviewer) throws MailSendBatchException {
    if (!mailService.isConfigured()) {
      LOG.warn("cannot send Email because the mail server is not configured");
      return;
    }

    Object principal = SecurityUtils.getSubject().getPrincipal();

    Set<String> subscriberWithoutReviewers = subscriber.stream()
      .filter(recipient -> !reviewer.contains(recipient))
      .filter(recipient -> !recipient.equals(principal))
      .collect(Collectors.toSet());

    Set<String> subscribingReviewers = subscriber.stream()
      .filter(reviewer::contains)
      .filter(recipient -> !recipient.equals(principal))
      .collect(Collectors.toSet());


    if (!subscriberWithoutReviewers.isEmpty()) {
      sendEmails(mailTextResolver, subscriberWithoutReviewers, false);
    }

    if (!subscribingReviewers.isEmpty()){
      sendEmails(mailTextResolver, subscribingReviewers, true);
    }
  }

  private void sendEmails(MailTextResolver mailTextResolver, Set<String> recipients, boolean reviewer) throws MailSendBatchException {
    MailService.EnvelopeBuilder envelopeBuilder = mailService.emailTemplateBuilder()
      .fromCurrentUser();
    recipients.forEach(envelopeBuilder::toUser);
    envelopeBuilder.withSubject(mailTextResolver.getMailSubject())
      .withTemplate(mailTextResolver.getContentTemplatePath(), MailTemplateType.MARKDOWN_HTML)
      .andModel(mailTextResolver.getContentTemplateModel(configuration.getBaseUrl(), reviewer))
      .send();
  }

}
