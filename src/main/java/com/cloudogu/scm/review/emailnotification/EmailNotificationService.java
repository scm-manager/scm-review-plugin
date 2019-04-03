package com.cloudogu.scm.review.emailnotification;

import com.cloudogu.scm.review.pullrequest.service.Recipient;
import lombok.extern.slf4j.Slf4j;
import org.codemonkey.simplejavamail.Email;
import sonia.scm.config.ScmConfiguration;
import sonia.scm.mail.api.MailContext;
import sonia.scm.mail.api.MailSendBatchException;
import sonia.scm.mail.api.MailService;
import sonia.scm.template.TemplateEngineFactory;

import javax.inject.Inject;
import javax.mail.Message;
import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

import static com.cloudogu.scm.review.CurrentUserResolver.getCurrentUser;
import static com.cloudogu.scm.review.CurrentUserResolver.getCurrentUserDisplayName;

@Slf4j
public class EmailNotificationService {

  private final MailService mailService;
  private final TemplateEngineFactory templateEngineFactory;
  private final ScmConfiguration configuration;
  private final MailContext mailContext;

  @Inject
  public EmailNotificationService(MailService mailService, TemplateEngineFactory templateEngineFactory, ScmConfiguration configuration, MailContext mailContext) {
    this.mailService = mailService;
    this.templateEngineFactory = templateEngineFactory;
    this.configuration = configuration;
    this.mailContext = mailContext;
  }

  public void sendEmails(EmailRenderer emailRenderer, Set<Recipient> recipients, Set<Recipient> reviewer) throws IOException, MailSendBatchException {
    if (!mailService.isConfigured()){
      log.warn("cannot send Email because the mail server is not configured");
      return ;
    }
    final String emailContent = emailRenderer.getMailContent(configuration.getBaseUrl(), templateEngineFactory, false);
    final String emailSubject = emailRenderer.getMailSubject();
    final String displayName = getCurrentUserDisplayName();

    Set<Recipient> subscriberWithoutReviewers = recipients.stream()
      .filter(recipient -> !reviewer.contains(recipient))
      .collect(Collectors.toSet());
    Set<Recipient> subscribingReviewers = recipients.stream()
      .filter(reviewer::contains)
      .collect(Collectors.toSet());

    sendEmails(subscriberWithoutReviewers, emailContent, emailSubject, displayName);

    if (!subscribingReviewers.isEmpty()){
      String reviewerEmailContent = emailRenderer.getMailContent(configuration.getBaseUrl(), templateEngineFactory, true);
      sendEmails(subscribingReviewers, reviewerEmailContent, emailSubject, displayName);
    }
  }

  private void sendEmails(Set<Recipient> recipients, String emailContent, String emailSubject, String displayName) throws MailSendBatchException {
    for (Recipient recipient : recipients) {
      if (!recipient.getAddress().equals(getCurrentUser().getMail())) {
        Email email = createEmail(emailContent, emailSubject, displayName);
        email.addRecipient(recipient.getName(), recipient.getAddress(), Message.RecipientType.TO);
        mailService.send(email);
      }
    }
  }

  private Email createEmail(String content, String emailSubject, String fromName) {
    Email email = new Email();
    email.setFromAddress(fromName, mailContext.getConfiguration().getFrom());
    email.setSubject(emailSubject);
    email.setTextHTML(content);
    return email;
  }
}
