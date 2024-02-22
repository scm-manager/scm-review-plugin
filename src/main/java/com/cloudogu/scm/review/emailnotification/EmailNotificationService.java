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

import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sonia.scm.config.ScmConfiguration;
import sonia.scm.mail.api.MailSendBatchException;
import sonia.scm.mail.api.MailService;
import sonia.scm.mail.api.MailTemplateType;

import java.util.Map;
import java.util.Set;

import static java.util.Locale.ENGLISH;
import static java.util.Locale.GERMAN;

public class EmailNotificationService {

  private static final Logger LOG = LoggerFactory.getLogger(EmailNotificationService.class);

  private final MailService mailService;
  private final ScmConfiguration configuration;

  @Inject
  public EmailNotificationService(MailService mailService, ScmConfiguration configuration) {
    this.mailService = mailService;
    this.configuration = configuration;
  }

  void sendEmail(Set<String> recipients, MailTextResolver mailTextResolver) throws MailSendBatchException {
    if (recipients.isEmpty()) {
      return;
    }
    if (!mailService.isConfigured()) {
      LOG.debug("cannot send Email because the mail server is not configured");
      return;
    }

    Map<String, Object> contentTemplateModel = mailTextResolver.getContentTemplateModel(configuration.getBaseUrl());

    MailService.EnvelopeBuilder envelopeBuilder = mailService.emailTemplateBuilder().fromCurrentUser();
    recipients.forEach(envelopeBuilder::toUser);
    MailService.MailBuilder mailBuilder = envelopeBuilder
      .onEntity(mailTextResolver.getPullRequestId())
      .onTopic(mailTextResolver.getTopic())
      .withSubject(mailTextResolver.getMailSubject(ENGLISH))
      .withSubject(GERMAN, mailTextResolver.getMailSubject(GERMAN))
      .withTemplate(mailTextResolver.getContentTemplatePath(), MailTemplateType.MARKDOWN_HTML)
      .andModel(contentTemplateModel);

    if (isPriority(mailTextResolver)) {
      mailBuilder.send();
    } else {
      mailBuilder.queueMails();
    }
  }

  private boolean isPriority(MailTextResolver resolver) {
    if (resolver.getTopic() == MailTextResolver.TOPIC_MENTIONS || resolver.getTopic() == MailTextResolver.TOPIC_CLOSED) {
      return true;
    }

    return resolver instanceof PullRequestEventMailTextResolver && ((PullRequestEventMailTextResolver) resolver).isCreated();
  }
}
