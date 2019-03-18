package com.cloudogu.scm.review.emailnotification;

import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import com.cloudogu.scm.review.pullrequest.service.Recipient;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.codemonkey.simplejavamail.Email;
import sonia.scm.config.ScmConfiguration;
import sonia.scm.mail.api.MailService;
import sonia.scm.repository.Repository;
import sonia.scm.security.Role;
import sonia.scm.template.Template;
import sonia.scm.template.TemplateEngine;
import sonia.scm.template.TemplateEngineFactory;
import sonia.scm.user.User;

import javax.inject.Inject;
import javax.mail.Message;
import java.io.StringWriter;
import java.text.MessageFormat;
import java.util.Map;

@Slf4j
public class EmailNotificationService {

  private static final String FROM_EMAIL = "no-replay@scm-manager.com";
  public static final String PATH_BASE = "com/cloudogu/scm/email/template/";
  private final static String SUBJECT_PATTERN = "Re: [SCM-Manager] Pull request #{0}: {1} ({2}/{3})";

  public static final String SCM_PULL_REQUEST_URL_PATTERN = "{0}/repo/{1}/{2}/pull-request/{3}";

  private MailService mailService;
  private TemplateEngineFactory templateEngineFactory;
  private ScmConfiguration configuration;

  @Inject
  public EmailNotificationService(MailService mailService, TemplateEngineFactory templateEngineFactory, ScmConfiguration configuration) {
    this.mailService = mailService;
    this.templateEngineFactory = templateEngineFactory;
    this.configuration = configuration;
  }


  public boolean sendEmail(EmailContext emailContext, Notification notification) {
    Map<String, Object> env = Maps.newHashMap();

    Repository repository = emailContext.getRepository();
    PullRequest pullRequest;
    if (emailContext.getPullRequest() != null) {
      pullRequest = emailContext.getPullRequest();
    } else if (emailContext.getOldPullRequest() != null) {
      pullRequest = emailContext.getPullRequest();
    }else{
      log.error("Cannot send Email! Pull Request no is found");
      return false;
    }
    String emailSubject = getEmailSubject(repository, pullRequest);
    String displayName = getCurrentUserDisplayName();
    String pullRequestLink = getPullRequestLink(repository,  pullRequest);

    env.put("title", emailSubject);
    env.put("displayName", displayName);
    env.put("link", pullRequestLink);
    env.put("repository", repository);
    env.put("pullRequest", emailContext.getPullRequest());
    env.put("oldPullRequest", emailContext.getOldPullRequest());
    env.put("comment", emailContext.getComment());
    env.put("oldComment", emailContext.getOldComment());

    String path = PATH_BASE + notification.getTemplate();
    TemplateEngine templateEngine = this.templateEngineFactory.getEngineByExtension(path);
    try {
      Template template = templateEngine.getTemplate(path);
      StringWriter writer = new StringWriter();

      template.execute(writer, env);
      String content = writer.toString();

      // create and send one address per recipient
      for (Recipient emailAddress : emailContext.getRecipients()) {
        Email email = createEmail(content, emailSubject, displayName);
        email.addRecipient(emailAddress.getName(), emailAddress.getAddress(), Message.RecipientType.TO);
        mailService.send(email);
      }

    } catch (Exception ex) {
      log.error("Error on sending Email ", ex);
    }

    return true;
  }

  private String getPullRequestLink(Repository repository, PullRequest pullRequest) {
    String baseUrl = configuration.getBaseUrl();
    return MessageFormat.format(SCM_PULL_REQUEST_URL_PATTERN, baseUrl, repository.getNamespace(), repository.getName(), pullRequest.getId());
  }

  private Email createEmail(String content, String emailSubject, String fromName) {
    Email email = new Email();
    email.setFromAddress(fromName, FROM_EMAIL);
    email.setSubject(emailSubject);
    email.setTextHTML(content);
    return email;
  }

  private String getEmailSubject(Repository repository, PullRequest pullRequest) {
    return MessageFormat.format(SUBJECT_PATTERN, pullRequest.getId(), pullRequest.getTitle(), repository.getNamespace(), repository.getName());
  }

  private String getCurrentUserDisplayName() {
    Subject subjectPrincipals = SecurityUtils.getSubject();
    String displayName = subjectPrincipals.getPrincipals().getPrimaryPrincipal().toString();
    if (subjectPrincipals.isPermitted(Role.USER)) {
      User user = subjectPrincipals.getPrincipals().oneByType(User.class);
      displayName = user.getDisplayName();
      if (Strings.isNullOrEmpty(displayName)) {
        displayName = user.getName();
      }
    }
    return displayName;
  }
}
