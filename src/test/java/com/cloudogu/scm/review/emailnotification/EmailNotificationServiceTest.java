package com.cloudogu.scm.review.emailnotification;

import com.cloudogu.scm.review.pullrequest.service.Recipient;
import com.google.common.collect.Lists;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.assertj.core.util.Sets;
import org.codemonkey.simplejavamail.Email;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.config.ScmConfiguration;
import sonia.scm.mail.api.MailConfiguration;
import sonia.scm.mail.api.MailContext;
import sonia.scm.mail.api.MailSendBatchException;
import sonia.scm.mail.api.MailService;
import sonia.scm.template.TemplateEngineFactory;
import sonia.scm.user.User;

import java.io.IOException;
import java.util.HashSet;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailNotificationServiceTest {

  @Mock
  TemplateEngineFactory templateEngineFactory;

  @Mock
  private MailService mailService;

  @Mock
  private MailContext mailContext;

  @Mock
  private ScmConfiguration configuration;

  @InjectMocks
  EmailNotificationService service;

  private final Subject subject = mock(Subject.class);

  @Test
  void shouldSendEmail() throws IOException, MailSendBatchException {

    ThreadContext.bind(subject);

    PrincipalCollection principals = mock(PrincipalCollection.class);
    when(subject.getPrincipals()).thenReturn(principals);
    when(subject.isPermitted(any(String.class))).thenReturn(true);
    String currentUser = "username";
    when(principals.getPrimaryPrincipal()).thenReturn(currentUser);
    User user1 = new User();
    user1.setName("user1");
    user1.setDisplayName("User 1");
    when(principals.oneByType(User.class)).thenReturn(user1);

    String path = "http://www.scm-manager.com";
    when(configuration.getBaseUrl()).thenReturn(path);

    MailConfiguration mailConfiguration = mock(MailConfiguration.class);
    when(mailConfiguration.getFrom()).thenReturn("no-replay@scm-manager.com");
    when(mailService.isConfigured()).thenReturn(true);
    when(mailContext.getConfiguration()).thenReturn(mailConfiguration);
    Recipient recipient1 = new Recipient("user1", "email1@d.de");
    Recipient recipient2 = new Recipient("user2", "email11@d.de");

    Recipient reviewer1 = new Recipient("reviewer1", "email2@d.de");
    Recipient reviewer2 = new Recipient("reviewer2", "email3@d.de");

    HashSet<Recipient> subscriber = Sets.newHashSet(Lists.newArrayList(recipient1, recipient2));
    HashSet<Recipient> reviewer = Sets.newHashSet(Lists.newArrayList(reviewer1, reviewer2));

    EmailRenderer emailRenderer = mock(EmailRenderer.class);
    when(emailRenderer.getMailContent(path, templateEngineFactory, false)).thenReturn("mail content");
    when(emailRenderer.getMailSubject()).thenReturn("subject");
    service.sendEmails(emailRenderer, subscriber, reviewer);
    ArgumentCaptor<Email> emailCaptor = ArgumentCaptor.forClass(Email.class);
    verify(mailService, times(4)).send(emailCaptor.capture());
    reset(mailService);
  }

  @Test
  void shouldNotSendEmailForNotConfiguredMailServer() throws IOException, MailSendBatchException {
    when(mailService.isConfigured()).thenReturn(false);
    Recipient recipient1 = new Recipient("user1", "email1@d.de");
    Recipient recipient2 = new Recipient("user2", "email1@d.de");
    HashSet<Recipient> subscriber = Sets.newHashSet(Lists.newArrayList(recipient1, recipient2));
    EmailRenderer emailRenderer = mock(EmailRenderer.class);

    service.sendEmail(emailRenderer, subscriber);

    verify(mailService, never()).send(any());
    reset(mailService);
  }


}
