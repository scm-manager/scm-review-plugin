package com.cloudogu.scm.review.emailnotification;

import com.cloudogu.scm.review.pullrequest.service.Recipient;
import com.google.common.collect.Lists;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.assertj.core.api.Assertions;
import org.assertj.core.util.Sets;
import org.codemonkey.simplejavamail.Email;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import sonia.scm.config.ScmConfiguration;
import sonia.scm.mail.api.MailConfiguration;
import sonia.scm.mail.api.MailContext;
import sonia.scm.mail.api.MailSendBatchException;
import sonia.scm.mail.api.MailService;
import sonia.scm.template.TemplateEngineFactory;
import sonia.scm.user.User;

import java.io.IOException;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
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
  void shouldSendEmailForSubsciberOnly() throws IOException, MailSendBatchException {

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

    ArgumentCaptor<Email> emailCaptor = ArgumentCaptor.forClass(Email.class);
    doNothing().when(mailService).send(emailCaptor.capture());

    MailConfiguration mailConfiguration = mock(MailConfiguration.class);
    when(mailConfiguration.getFrom()).thenReturn("no-replay@scm-manager.com");
    when(mailService.isConfigured()).thenReturn(true);
    when(mailContext.getConfiguration()).thenReturn(mailConfiguration);
    Recipient recipient1 = new Recipient("user1", "email1@d.de");

    Recipient reviewer1 = new Recipient("reviewer1", "email2@d.de");

    Recipient reviewer2 = new Recipient("reviewer2", "email3@d.de");

    HashSet<Recipient> subscriber = Sets.newHashSet(Lists.newArrayList(recipient1, reviewer1));
    HashSet<Recipient> reviewer = Sets.newHashSet(Lists.newArrayList(reviewer1, reviewer2));

    EmailRenderer emailRenderer = mock(EmailRenderer.class);
    when(emailRenderer.getMailContent(path, templateEngineFactory, false)).thenReturn("normal content");
    when(emailRenderer.getMailContent(path, templateEngineFactory, true)).thenReturn("reviewer content");
    when(emailRenderer.getMailSubject()).thenReturn("subject");
    service.sendEmails(emailRenderer, subscriber, reviewer);
    assertThat(emailCaptor.getAllValues())
      .hasSize(2)
      .extracting("textHTML")
      .containsExactlyInAnyOrder("normal content", "reviewer content");
    reset(mailService);
  }

  @Test
  void shouldNotSendEmailForNotConfiguredMailServer() throws IOException, MailSendBatchException {
    when(mailService.isConfigured()).thenReturn(false);
    Recipient recipient1 = new Recipient("user1", "email1@d.de");
    Recipient recipient2 = new Recipient("user2", "email1@d.de");
    HashSet<Recipient> subscriber = Sets.newHashSet(Lists.newArrayList(recipient1, recipient2));
    EmailRenderer emailRenderer = mock(EmailRenderer.class);

    service.sendEmails(emailRenderer, subscriber, null);

    verify(mailService, never()).send(any(Email.class));
    reset(mailService);
  }


}
