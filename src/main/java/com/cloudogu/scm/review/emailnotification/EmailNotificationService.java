package com.cloudogu.scm.review.emailnotification;

import com.cloudogu.scm.review.pullrequest.service.Recipient;
import lombok.extern.slf4j.Slf4j;
import org.codemonkey.simplejavamail.Email;
import sonia.scm.config.ScmConfiguration;
import sonia.scm.mail.api.MailSendBatchException;
import sonia.scm.mail.api.MailService;
import sonia.scm.template.TemplateEngineFactory;

import javax.inject.Inject;
import javax.mail.Message;
import java.io.IOException;
import java.util.Set;

import static com.cloudogu.scm.review.CurrentUserResolver.getCurrentUser;
import static com.cloudogu.scm.review.CurrentUserResolver.getCurrentUserDisplayName;

@Slf4j
public class EmailNotificationService {

  private static final String FROM_EMAIL = "no-replay@scm-manager.com";
  private MailService mailService;
  private TemplateEngineFactory templateEngineFactory;
  private ScmConfiguration configuration;

  @Inject
  public EmailNotificationService(MailService mailService, TemplateEngineFactory templateEngineFactory, ScmConfiguration configuration) {
    this.mailService = mailService;
    this.templateEngineFactory = templateEngineFactory;
    this.configuration = configuration;
  }

  public void sendEmail(EmailRenderer emailRenderer, Set<Recipient> recipients) throws IOException, MailSendBatchException {
    String emailContent = emailRenderer.getMailContent(configuration.getBaseUrl(), templateEngineFactory);
    String emailSubject = emailRenderer.getMailSubject();
    String displayName = getCurrentUserDisplayName();

    for (Recipient emailAddress : recipients) {
      if (!emailAddress.getAddress().equals(getCurrentUser().getMail())) {
        Email email = createEmail(emailContent, emailSubject, displayName);
        email.addRecipient(emailAddress.getName(), emailAddress.getAddress(), Message.RecipientType.TO);
        mailService.send(email);
      }
    }
  }

  private Email createEmail(String content, String emailSubject, String fromName) {
    Email email = new Email();
    email.setFromAddress(fromName, FROM_EMAIL);
    email.setSubject(emailSubject);
    email.setTextHTML(content);
    return email;
  }
}
