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

import com.cloudogu.scm.review.CurrentUserResolver;
import com.cloudogu.scm.review.pullrequest.service.BasicPullRequestEvent;
import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import com.google.common.collect.Maps;
import sonia.scm.repository.Repository;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import static java.util.Locale.ENGLISH;
import static java.util.Locale.GERMAN;
import static java.util.ResourceBundle.getBundle;

public abstract class BasicPRMailTextResolver<E extends BasicPullRequestEvent> implements MailTextResolver {

  private static final String SUBJECT_PATTERN = "{0}/{1} {2} (#{3} {4})";
  private static final String SCM_PULL_REQUEST_URL_PATTERN = "{0}/repo/{1}/{2}/pull-request/{3}";

  private static final Map<Locale, ResourceBundle> SUBJECT_BUNDLES = Maps
    .asMap(new HashSet<>(Arrays.asList(ENGLISH, GERMAN)), locale -> getBundle("com.cloudogu.scm.review.emailnotification.Subjects", locale));

  protected String getMailSubject(E event, String displayEventNameKey, Locale locale) {
    Repository repository = event.getRepository();
    PullRequest pullRequest = event.getPullRequest();
    String displayEventName = SUBJECT_BUNDLES.get(locale).getString(displayEventNameKey);
    return MessageFormat.format(SUBJECT_PATTERN, repository.getNamespace(), repository.getName(), displayEventName, pullRequest.getId(), pullRequest.getTitle());
  }

  private String getPullRequestLink(String baseUrl, E event) {
    Repository repository = event.getRepository();
    PullRequest pullRequest = event.getPullRequest();
    return MessageFormat.format(SCM_PULL_REQUEST_URL_PATTERN, baseUrl, repository.getNamespace(), repository.getName(), pullRequest.getId());
  }

  /**
   * The basic environment used by all templates
   *
   * @param basePath   the path url of the ui
   * @param event      the fired event
   * @return basic environment used by all templates
   */
  protected Map<String, Object> getTemplateModel(String basePath, E event) {
    Map<String, Object> result = Maps.newHashMap();
    result.put("namespace", event.getRepository().getNamespace());
    result.put("name", event.getRepository().getName());
    result.put("id", event.getPullRequest().getId());
    result.put("title", event.getPullRequest().getTitle());
    result.put("displayName", CurrentUserResolver.getCurrentUserDisplayName());
    result.put("link", getPullRequestLink(basePath, event));
    result.put("repository", event.getRepository());
    result.put("pullRequest", event.getPullRequest());
    result.put("target", event.getPullRequest().getTarget());
    return result;
  }


}
