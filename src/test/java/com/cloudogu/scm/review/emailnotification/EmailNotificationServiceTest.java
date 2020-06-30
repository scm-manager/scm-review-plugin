/*
 * MIT License
 *
 * Copyright (c) 2020-present Cloudogu GmbH and Contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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

import java.util.Collections;
import java.util.Map;

import static java.util.Locale.ENGLISH;
import static java.util.Locale.GERMAN;
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

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private MailService.SubjectBuilder subjectBuilder;

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
  void shouldSendEmailToRecipient() throws MailSendBatchException {
    mockDependencies();
    when(mailService.isConfigured()).thenReturn(true);
    when(mailService.emailTemplateBuilder()).thenReturn(envelopeBuilder);

    service.sendEmail(Sets.newHashSet("dent", "trillian"), mailTextResolver);

    verify(envelopeBuilder).fromCurrentUser();
    verify(envelopeBuilder).toUser("dent");
    verify(envelopeBuilder).toUser("trillian");
  }

  private void mockDependencies() {
    when(mailService.isConfigured()).thenReturn(true);
    when(configuration.getBaseUrl()).thenReturn("https://scm.hitchhiker.com");

    when(mailTextResolver.getMailSubject(ENGLISH)).thenReturn("Awesome Subject");
    when(mailTextResolver.getMailSubject(GERMAN)).thenReturn("Genialer Betreff");
    when(mailTextResolver.getContentTemplatePath()).thenReturn("/path/to/template");
    when(mailTextResolver.getContentTemplateModel(anyString())).thenReturn(subscriberModel);

    when(envelopeBuilder
      .withSubject("Awesome Subject")).thenReturn(subjectBuilder);
    when(subjectBuilder
      .withSubject(GERMAN, "Genialer Betreff")).thenReturn(subjectBuilder);
    when(mailService.emailTemplateBuilder()).thenReturn(envelopeBuilder);
  }

}
