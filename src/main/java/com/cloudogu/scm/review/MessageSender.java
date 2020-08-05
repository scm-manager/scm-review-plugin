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
package com.cloudogu.scm.review;

import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import sonia.scm.config.ScmConfiguration;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryHookEvent;
import sonia.scm.repository.api.HookMessageProvider;

import java.util.Arrays;

public interface MessageSender {

  void sendMessageForPullRequest(PullRequest pullRequest, String message);

  void sendCreatePullRequestMessage(String branch, String message);
}

class ProviderMessageSender implements MessageSender {

  private final ScmConfiguration configuration;
  private final RepositoryHookEvent event;

  public ProviderMessageSender(ScmConfiguration configuration, RepositoryHookEvent event) {
    this.configuration = configuration;
    this.event = event;
  }

  @Override
  public void sendMessageForPullRequest(PullRequest pullRequest, String message) {
    sendMessages(message, createPullRequestLink(pullRequest));
  }

  @Override
  public void sendCreatePullRequestMessage(String branch, String message) {
    sendMessages(message, createCreateLink(event.getRepository(), branch));
  }

  private String createCreateLink(Repository repository, String source) {
    return String.format(
      "%s/repo/%s/%s/pull-requests/add/changesets/?source=%s",
      configuration.getBaseUrl(),
      repository.getNamespace(),
      repository.getName(),
      source);
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

  private void sendMessages(String... messages) {
    HookMessageProvider messageProvider = event.getContext().getMessageProvider();
    messageProvider.sendMessage("");
    Arrays.stream(messages).forEach(messageProvider::sendMessage);
    messageProvider.sendMessage("");
  }
}

class NoOpMessageSender implements MessageSender {

  @Override
  public void sendMessageForPullRequest(PullRequest pullRequest, String message) {
    // no op implementation
  }

  @Override
  public void sendCreatePullRequestMessage(String branch, String message) {
    // no op implementation
  }
}

