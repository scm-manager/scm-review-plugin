package com.cloudogu.scm.review.emailnotification;

import com.cloudogu.scm.review.pullrequest.service.Recipient;
import lombok.extern.slf4j.Slf4j;
import org.codemonkey.simplejavamail.Email;
import sonia.scm.config.ScmConfiguration;
import sonia.scm.mail.api.MailContext;
import sonia.scm.mail.api.MailSendParams;
import sonia.scm.mail.api.MailService;

import javax.inject.Inject;
import javax.mail.Message;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.cloudogu.scm.review.CurrentUserResolver.getCurrentUser;
import static com.cloudogu.scm.review.CurrentUserResolver.getCurrentUserDisplayName;

@Slf4j
public class EmailNotificationService {

  private final MailService mailService;
  private final ScmConfiguration configuration;
  private final MailContext mailContext;

  @Inject
  public EmailNotificationService(MailService mailService, ScmConfiguration configuration, MailContext mailContext) {
    this.mailService = mailService;
    this.configuration = configuration;
    this.mailContext = mailContext;
  }

  public void sendEmails(MailTextResolver mailTextResolver, Set<Recipient> recipients, Set<Recipient> reviewer) throws Exception {
    if (!mailService.isConfigured()) {
      log.warn("cannot send Email because the mail server is not configured");
      return;
    }
    String emailSubject = mailTextResolver.getMailSubject();
    String displayName = getCurrentUserDisplayName();

    Set<Recipient> subscriberWithoutReviewers = recipients.stream()
      .filter(recipient -> !reviewer.contains(recipient))
      .collect(Collectors.toSet());
    Set<Recipient> subscribingReviewers = recipients.stream()
      .filter(reviewer::contains)
      .collect(Collectors.toSet());

    String path = mailTextResolver.getContentTemplatePath();
    if (!subscriberWithoutReviewers.isEmpty()) {
      Map<String, Object> templateModel = mailTextResolver.getContentTemplateModel(configuration.getBaseUrl(), false);
      sendEmails(subscriberWithoutReviewers, emailSubject, displayName, path, templateModel);
    }

    if (!subscribingReviewers.isEmpty()){
      Map<String, Object> reviewerTemplateModel = mailTextResolver.getContentTemplateModel(configuration.getBaseUrl(), true);
      sendEmails(subscribingReviewers, emailSubject, displayName, path, reviewerTemplateModel);
    }
  }

  private void sendEmails(Set<Recipient> recipients, String emailSubject, String displayName, String path, Map<String, Object> templateModel) throws Exception {
    MailSendParams mailSendParams = mailContext.withTemplate(path).andModel(templateModel);
    for (Recipient recipient : recipients) {
      if (!recipient.getAddress().equals(getCurrentUser().getMail())) {
        Email email = createEmail(emailSubject, displayName);
        email.addRecipient(recipient.getName(), recipient.getAddress(), Message.RecipientType.TO);
        mailSendParams
          .forUserId(recipient.getName())
          .sendEmail(email);
      }
    }
    mailService.send(mailSendParams);
  }

  private Email createEmail(String emailSubject, String fromName) {
    Email email = new Email();
    email.setFromAddress(fromName, mailContext.getConfiguration().getFrom());
    email.setSubject(emailSubject);
    return email;
  }
}
