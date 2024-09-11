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

package com.cloudogu.scm.review;

import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import sonia.scm.config.ScmConfiguration;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryHookEvent;
import sonia.scm.repository.api.HookMessageProvider;
import sonia.scm.util.HttpUtil;

import java.util.Arrays;

public interface MessageSender {

  void sendMessageForPullRequest(PullRequest pullRequest, String... message);

  void sendCreatePullRequestMessage(String branch, String... message);
}

class ProviderMessageSender implements MessageSender {

  private final ScmConfiguration configuration;
  private final RepositoryHookEvent event;

  public ProviderMessageSender(ScmConfiguration configuration, RepositoryHookEvent event) {
    this.configuration = configuration;
    this.event = event;
  }

  @Override
  public void sendMessageForPullRequest(PullRequest pullRequest, String... message) {
    sendMessages(message, createPullRequestLink(pullRequest));
  }

  @Override
  public void sendCreatePullRequestMessage(String branch, String... message) {
    sendMessages(message, createCreateLink(event.getRepository(), branch));
  }

  private String createCreateLink(Repository repository, String source) {
    return String.format(
      "%s/repo/%s/%s/pull-requests/add/changesets/?source=%s",
      configuration.getBaseUrl(),
      repository.getNamespace(),
      repository.getName(),
      HttpUtil.encode(source));
  }

  private String createPullRequestLink(PullRequest pullRequest) {
    Repository repository = event.getRepository();
    return String.format(
      "%s/repo/%s/%s/pull-request/%s/",
      configuration.getBaseUrl(),
      repository.getNamespace(),
      repository.getName(),
      pullRequest.getId());
  }

  private void sendMessages(String[] messages, String url) {
    HookMessageProvider messageProvider = event.getContext().getMessageProvider();
    messageProvider.sendMessage("");
    Arrays.stream(messages).forEach(messageProvider::sendMessage);
    messageProvider.sendMessage(url);
    messageProvider.sendMessage("");
  }
}

class NoOpMessageSender implements MessageSender {

  @Override
  public void sendMessageForPullRequest(PullRequest pullRequest, String... message) {
    // no op implementation
  }

  @Override
  public void sendCreatePullRequestMessage(String branch, String... message) {
    // no op implementation
  }
}

