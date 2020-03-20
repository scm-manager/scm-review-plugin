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
import sonia.scm.repository.PostReceiveRepositoryHookEvent;
import sonia.scm.repository.Repository;
import sonia.scm.repository.api.HookMessageProvider;

import javax.inject.Inject;
import java.util.Arrays;

import static sonia.scm.repository.api.HookFeature.MESSAGE_PROVIDER;

public class MessageSender {

  private final ScmConfiguration configuration;
  private final PostReceiveRepositoryHookEvent event;

  @Inject
  public MessageSender(ScmConfiguration configuration, PostReceiveRepositoryHookEvent event) {
    this.configuration = configuration;
    this.event = event;
  }

  public void sendMessageForPullRequest(PullRequest pullRequest, String message) {
    sendMessages(message, createPullRequestLink(pullRequest));
  }

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
    if (event.getContext().isFeatureSupported(MESSAGE_PROVIDER)) {
      HookMessageProvider messageProvider = event.getContext().getMessageProvider();
      messageProvider.sendMessage("");
      Arrays.stream(messages).forEach(messageProvider::sendMessage);
      messageProvider.sendMessage("");
    }
  }
}
