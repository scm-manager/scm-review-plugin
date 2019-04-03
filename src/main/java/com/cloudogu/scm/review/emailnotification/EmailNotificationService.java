package com.cloudogu.scm.review.emailnotification;

import com.cloudogu.scm.review.pullrequest.service.Recipient;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.codemonkey.simplejavamail.Email;
import sonia.scm.config.ScmConfiguration;
import sonia.scm.mail.api.MailContentRenderer;
import sonia.scm.mail.api.MailContext;
import sonia.scm.mail.api.MailSendParams;
import sonia.scm.mail.api.MailService;
import sonia.scm.mail.spi.DefaultMailContentRenderer;
import sonia.scm.plugin.PluginLoader;
import sonia.scm.template.TemplateEngineFactory;

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
  private final TemplateEngineFactory templateEngineFactory;
  private final ScmConfiguration configuration;
  private final MailContext mailContext;
  private PluginLoader pluginLoader;

  @Inject
  public EmailNotificationService(MailService mailService, TemplateEngineFactory templateEngineFactory, ScmConfiguration configuration, MailContext mailContext, PluginLoader pluginLoader) {
    this.mailService = mailService;
    this.templateEngineFactory = templateEngineFactory;
    this.configuration = configuration;
    this.mailContext = mailContext;
    this.pluginLoader = pluginLoader;
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
      MailContentRenderer renderer = new DefaultMailContentRenderer(templateEngineFactory, path, templateModel, mailContext.getConfiguration(), mailContext.getUserLanguageConfiguration(), pluginLoader);
      sendEmails(subscriberWithoutReviewers, emailSubject, displayName, renderer);
    }

    if (!subscribingReviewers.isEmpty()){
      Map<String, Object> reviewerTemplateModel = mailTextResolver.getContentTemplateModel(configuration.getBaseUrl(), true);
      MailContentRenderer reviewerRenderer = new DefaultMailContentRenderer(templateEngineFactory, path, reviewerTemplateModel, mailContext.getConfiguration(), mailContext.getUserLanguageConfiguration(), pluginLoader);
      sendEmails(subscribingReviewers, emailSubject, displayName, reviewerRenderer);
    }
  }


  private void sendEmails(Set<Recipient> recipients,  String emailSubject, String displayName, MailContentRenderer renderer) throws Exception {
    for (Recipient recipient : recipients) {
      if (!recipient.getAddress().equals(getCurrentUser().getMail())) {
        Email email = createEmail( emailSubject, displayName);
        email.addRecipient(recipient.getName(), recipient.getAddress(), Message.RecipientType.TO);
        MailSendParams mailSendParams = MailSendParams.builder()
          .mailContentRenderer(renderer)
          .emails(Lists.newArrayList(email))
          .userId(recipient.getName())
          .build();
        mailService.send(mailSendParams);
      }
    }
  }

  private Email createEmail(String emailSubject, String fromName) {
    Email email = new Email();
    email.setFromAddress(fromName, mailContext.getConfiguration().getFrom());
    email.setSubject(emailSubject);
    return email;
  }
}
