package com.cloudogu.scm.review.emailnotification;

import com.cloudogu.scm.review.pullrequest.service.Recipient;
import com.google.common.collect.Lists;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.assertj.core.util.Sets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import sonia.scm.config.ScmConfiguration;
import sonia.scm.mail.api.MailConfiguration;
import sonia.scm.mail.api.MailContext;
import sonia.scm.mail.api.MailSendParams;
import sonia.scm.mail.api.MailService;
import sonia.scm.mail.spi.MailContentRendererFactory;
import sonia.scm.store.InMemoryConfigurationEntryStoreFactory;
import sonia.scm.store.InMemoryConfigurationStore;
import sonia.scm.store.InMemoryConfigurationStoreFactory;
import sonia.scm.user.User;

import java.util.Collections;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EmailNotificationServiceTest {

  @Mock
  private MailService mailService;

  @Mock
  private ScmConfiguration configuration;

  EmailNotificationService service;

  private final Subject subject = mock(Subject.class);

  @Test
  void shouldSendEmailForSubsciberOnly() throws Exception {

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

    ArgumentCaptor<MailSendParams> emailCaptor = ArgumentCaptor.forClass(MailSendParams.class);
    doNothing().when(mailService).send(emailCaptor.capture());

    when(mailService.isConfigured()).thenReturn(true);

    Recipient recipient1 = new Recipient("user1", "email1@d.de");

    Recipient reviewer1 = new Recipient("reviewer1", "email2@d.de");

    Recipient reviewer2 = new Recipient("reviewer2", "email3@d.de");

    HashSet<Recipient> subscriber = Sets.newHashSet(Lists.newArrayList(recipient1, reviewer1));
    HashSet<Recipient> reviewer = Sets.newHashSet(Lists.newArrayList(reviewer1, reviewer2));

    MailTextResolver mailTextResolver = mock(MailTextResolver.class);
    when(mailTextResolver.getContentTemplateModel(path, false)).thenReturn(Collections.singletonMap("en", "normal content"));
    when(mailTextResolver.getContentTemplateModel(path, true)).thenReturn(Collections.singletonMap("en", "reviewer content"));

    initService();
    service.sendEmails(mailTextResolver, subscriber, reviewer);
    assertThat(emailCaptor.getAllValues())
      .hasSize(2)
      .flatExtracting("userEmails");
    // TODO:
//      .extracting("textHTML")
//      .containsExactlyInAnyOrder("normal content", "reviewer content");
    reset(mailService);
  }

  private void initService() {
    MailConfiguration mailConfiguration = mock(MailConfiguration.class);
    when(mailConfiguration.getFrom()).thenReturn("no-replay@scm-manager.com");
    when(mailConfiguration.getLanguage()).thenReturn("en");
    InMemoryConfigurationStoreFactory storeFactory = new InMemoryConfigurationStoreFactory(new InMemoryConfigurationStore());
    MailContext mailContext = new MailContext(storeFactory, new InMemoryConfigurationEntryStoreFactory(), mock(MailContentRendererFactory.class));
    storeFactory.getStore(null).set(mailConfiguration);

    service = new EmailNotificationService(mailService, configuration, mailContext);
  }

  @Test
  void shouldNotSendEmailForNotConfiguredMailServer() throws Exception {
    initService();
    when(mailService.isConfigured()).thenReturn(false);
    Recipient recipient1 = new Recipient("user1", "email1@d.de");
    Recipient recipient2 = new Recipient("user2", "email1@d.de");
    HashSet<Recipient> subscriber = Sets.newHashSet(Lists.newArrayList(recipient1, recipient2));
    MailTextResolver mailTextResolver = mock(MailTextResolver.class);

    service.sendEmails(mailTextResolver, subscriber, null);

    verify(mailService, never()).send(any(MailSendParams.class));
    reset(mailService);
  }


}
