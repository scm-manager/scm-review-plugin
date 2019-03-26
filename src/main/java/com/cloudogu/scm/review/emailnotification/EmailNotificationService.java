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
    email.setFromAddress(fromName, mailContext.getConfiguration().getFrom());
    email.setSubject(emailSubject);
    email.setTextHTML(content);
    return email;
  }
}
