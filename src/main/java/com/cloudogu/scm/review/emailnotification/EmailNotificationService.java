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
