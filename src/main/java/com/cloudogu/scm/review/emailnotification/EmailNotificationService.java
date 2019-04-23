package com.cloudogu.scm.review.emailnotification;

import lombok.extern.slf4j.Slf4j;
import org.codemonkey.simplejavamail.Email;
import sonia.scm.config.ScmConfiguration;
import sonia.scm.mail.api.MailContext;
import sonia.scm.mail.api.MailSendBatchException;
import sonia.scm.mail.api.MailService;
import sonia.scm.template.TemplateEngineFactory;
import sonia.scm.user.DisplayUser;
import sonia.scm.user.UserDisplayManager;

import javax.inject.Inject;
import javax.mail.Message;
import java.io.IOException;
import java.util.Optional;
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
  private final UserDisplayManager userDisplayManager;

  @Inject
  public EmailNotificationService(MailService mailService, TemplateEngineFactory templateEngineFactory, ScmConfiguration configuration, MailContext mailContext, UserDisplayManager userDisplayManager) {
    this.mailService = mailService;
    this.templateEngineFactory = templateEngineFactory;
    this.configuration = configuration;
    this.mailContext = mailContext;
    this.userDisplayManager = userDisplayManager;
  }

  public void sendEmails(EmailRenderer emailRenderer, Set<String> recipients, Set<String> reviewer) throws IOException, MailSendBatchException {
    if (!mailService.isConfigured()){
      log.warn("cannot send Email because the mail server is not configured");
      return ;
    }
    final String emailContent = emailRenderer.getMailContent(configuration.getBaseUrl(), templateEngineFactory, false);
    final String emailSubject = emailRenderer.getMailSubject();
    final String displayName = getCurrentUserDisplayName();

    Set<String> subscriberWithoutReviewers = recipients.stream()
      .filter(recipient -> !reviewer.contains(recipient))
      .collect(Collectors.toSet());
    Set<String> subscribingReviewers = recipients.stream()
      .filter(reviewer::contains)
      .collect(Collectors.toSet());

    sendEmails(subscriberWithoutReviewers, emailContent, emailSubject, displayName);

    if (!subscribingReviewers.isEmpty()){
      String reviewerEmailContent = emailRenderer.getMailContent(configuration.getBaseUrl(), templateEngineFactory, true);
      sendEmails(subscribingReviewers, reviewerEmailContent, emailSubject, displayName);
    }
  }

  private void sendEmails(Set<String> recipients, String emailContent, String emailSubject, String displayName) throws MailSendBatchException {
    for (String recipient : recipients) {
      if (!recipient.equals(getCurrentUser().getId())) {
        Optional<DisplayUser> displayUser = userDisplayManager.get(recipient);
        if (displayUser.isPresent()) {
          Email email = createEmail(emailContent, emailSubject, displayName);
          email.addRecipient(displayUser.get().getDisplayName(), displayUser.get().getMail(), Message.RecipientType.TO);
          mailService.send(email);
        }
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
