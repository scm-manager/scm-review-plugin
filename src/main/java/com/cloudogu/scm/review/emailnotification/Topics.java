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

import sonia.scm.mail.api.Topic;
import sonia.scm.mail.api.TopicProvider;
import sonia.scm.plugin.Extension;

import java.util.Collection;

import static com.cloudogu.scm.review.emailnotification.MailTextResolver.TOPIC_APPROVALS;
import static com.cloudogu.scm.review.emailnotification.MailTextResolver.TOPIC_CLOSED;
import static com.cloudogu.scm.review.emailnotification.MailTextResolver.TOPIC_COMMENTS;
import static com.cloudogu.scm.review.emailnotification.MailTextResolver.TOPIC_MENTIONS;
import static com.cloudogu.scm.review.emailnotification.MailTextResolver.TOPIC_PR_CHANGED;
import static com.cloudogu.scm.review.emailnotification.MailTextResolver.TOPIC_PR_UPDATED;
import static com.cloudogu.scm.review.emailnotification.MailTextResolver.TOPIC_REPLIES;
import static java.util.Arrays.asList;

@Extension
public class Topics implements TopicProvider {

  @Override
  public Collection<Topic> topics() {
    return asList(
      TOPIC_PR_CHANGED,
      TOPIC_PR_UPDATED,
      TOPIC_APPROVALS,
      TOPIC_CLOSED,
      TOPIC_MENTIONS,
      TOPIC_COMMENTS,
      TOPIC_REPLIES
    );
  }
}
