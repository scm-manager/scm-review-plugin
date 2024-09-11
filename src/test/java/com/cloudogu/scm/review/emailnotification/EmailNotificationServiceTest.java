/*
 * Copyright (c) 2020 - present Cloudogu GmbH
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Affero General Public License as published by the Free
 * Software Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see https://www.gnu.org/licenses/.
 */

package com.cloudogu.scm.review.emailnotification;

import com.google.common.collect.Sets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import sonia.scm.config.ScmConfiguration;
import sonia.scm.mail.api.MailSendBatchException;
import sonia.scm.mail.api.MailService;
import sonia.scm.mail.api.MailTemplateType;
import sonia.scm.mail.api.Topic;

import java.util.Collections;
import java.util.Map;

import static java.util.Locale.ENGLISH;
import static java.util.Locale.GERMAN;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EmailNotificationServiceTest {

  @Mock
  private MailService mailService;

  @Mock(answer = Answers.RETURNS_SELF)
  private MailService.EnvelopeBuilder envelopeBuilder;

  @Mock
  private MailService.SubjectBuilder subjectBuilder;

  @Mock
  private MailService.TemplateBuilder templateBuilder;

  @Mock
  private MailService.MailBuilder mailBuilder;

  @Mock
  private ScmConfiguration configuration;

  @InjectMocks
  private EmailNotificationService service;

  @Mock
  private MailTextResolver mailTextResolver;

  private final Map<String,Object> subscriberModel = Collections.singletonMap("subscriber", true);

  @Test
  void shouldNotSendEmailForNotConfiguredMailServer() throws Exception {
    when(mailService.isConfigured()).thenReturn(false);

    service.sendEmail(Collections.singleton("trillian"), mailTextResolver);
    service.sendEmail(Collections.emptySet(), mailTextResolver);

    verify(mailService).isConfigured();
    verifyNoMoreInteractions(mailService);
  }

  @Test
  void shouldSendPrioEmailToRecipient() throws MailSendBatchException {
    mockDependencies(MailTextResolver.TOPIC_MENTIONS);

    service.sendEmail(Sets.newHashSet("dent", "trillian"), mailTextResolver);

    verify(envelopeBuilder).fromCurrentUser();
    verify(envelopeBuilder).toUser("dent");
    verify(envelopeBuilder).toUser("trillian");
    verify(mailBuilder).send();
  }

  @Test
  void shouldQueueEmailsForRecipients() throws MailSendBatchException {
    mockDependencies(MailTextResolver.TOPIC_APPROVALS);

    service.sendEmail(Sets.newHashSet("dent", "trillian"), mailTextResolver);

    verify(envelopeBuilder).fromCurrentUser();
    verify(envelopeBuilder).toUser("dent");
    verify(envelopeBuilder).toUser("trillian");
    verify(mailBuilder).queueMails();
  }

  private void mockDependencies(Topic topic) {
    when(mailService.isConfigured()).thenReturn(true);
    when(configuration.getBaseUrl()).thenReturn("https://scm.hitchhiker.com");

    when(mailTextResolver.getMailSubject(ENGLISH)).thenReturn("Awesome Subject");
    when(mailTextResolver.getMailSubject(GERMAN)).thenReturn("Genialer Betreff");
    when(mailTextResolver.getContentTemplatePath()).thenReturn("/path/to/template");
    when(mailTextResolver.getContentTemplateModel(anyString())).thenReturn(subscriberModel);
    when(mailTextResolver.getTopic()).thenReturn(topic);

    when(envelopeBuilder
      .withSubject("Awesome Subject")).thenReturn(subjectBuilder);
    when(subjectBuilder
      .withSubject(GERMAN, "Genialer Betreff")).thenReturn(subjectBuilder);
    when(subjectBuilder.withTemplate("/path/to/template", MailTemplateType.MARKDOWN_HTML))
      .thenReturn(templateBuilder);
    when(templateBuilder.andModel(any())).thenReturn(mailBuilder);
    when(mailService.emailTemplateBuilder()).thenReturn(envelopeBuilder);
  }

}
