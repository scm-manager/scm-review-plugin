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

import sonia.scm.mail.api.Category;
import sonia.scm.mail.api.Topic;

import java.util.Locale;
import java.util.Map;

public interface MailTextResolver {

  Category CATEGORY = new Category("review-plugin");
  Topic TOPIC_PR_CHANGED = new Topic(CATEGORY, "prChanged");
  Topic TOPIC_PR_UPDATED = new Topic(CATEGORY, "prUpdated");
  Topic TOPIC_APPROVALS = new Topic(CATEGORY, "approvals");
  Topic TOPIC_MENTIONS = new Topic(CATEGORY, "mentions");
  Topic TOPIC_COMMENTS = new Topic(CATEGORY, "comments");
  Topic TOPIC_REPLIES = new Topic(CATEGORY, "replies");
  Topic TOPIC_CLOSED = new Topic(CATEGORY, "closed");

  String getMailSubject(Locale locale);

  String getContentTemplatePath();

  Map<String, Object> getContentTemplateModel(String basePath);

  Topic getTopic();

  String getPullRequestId();
}
