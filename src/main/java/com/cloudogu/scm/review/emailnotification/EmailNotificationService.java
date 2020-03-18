/**
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

import org.apache.shiro.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sonia.scm.config.ScmConfiguration;
import sonia.scm.mail.api.MailSendBatchException;
import sonia.scm.mail.api.MailService;
import sonia.scm.mail.api.MailTemplateType;

import javax.inject.Inject;
import java.util.Set;
import java.util.stream.Collectors;

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

  public void sendEmails(MailTextResolver mailTextResolver, Set<String> subscriber, Set<String> reviewer) throws MailSendBatchException {
    if (!mailService.isConfigured()) {
      LOG.warn("cannot send Email because the mail server is not configured");
      return;
    }

    Object principal = SecurityUtils.getSubject().getPrincipal();

    Set<String> subscriberWithoutReviewers = subscriber.stream()
      .filter(recipient -> !reviewer.contains(recipient))
      .filter(recipient -> !recipient.equals(principal))
      .collect(Collectors.toSet());

    Set<String> subscribingReviewers = subscriber.stream()
      .filter(reviewer::contains)
      .filter(recipient -> !recipient.equals(principal))
      .collect(Collectors.toSet());


    if (!subscriberWithoutReviewers.isEmpty()) {
      sendEmails(mailTextResolver, subscriberWithoutReviewers, false);
    }

    if (!subscribingReviewers.isEmpty()){
      sendEmails(mailTextResolver, subscribingReviewers, true);
    }
  }

  private void sendEmails(MailTextResolver mailTextResolver, Set<String> recipients, boolean reviewer) throws MailSendBatchException {
    MailService.EnvelopeBuilder envelopeBuilder = mailService.emailTemplateBuilder()
      .fromCurrentUser();
    recipients.forEach(envelopeBuilder::toUser);
    envelopeBuilder
      .withSubject(mailTextResolver.getMailSubject(ENGLISH))
      .withSubject(GERMAN, mailTextResolver.getMailSubject(GERMAN))
      .withTemplate(mailTextResolver.getContentTemplatePath(), MailTemplateType.MARKDOWN_HTML)
      .andModel(mailTextResolver.getContentTemplateModel(configuration.getBaseUrl(), reviewer))
      .send();
  }

}
