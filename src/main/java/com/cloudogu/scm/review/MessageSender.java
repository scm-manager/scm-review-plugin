package com.cloudogu.scm.review;

import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import sonia.scm.config.ScmConfiguration;
import sonia.scm.repository.PostReceiveRepositoryHookEvent;
import sonia.scm.repository.Repository;
import sonia.scm.repository.api.HookMessageProvider;

import javax.inject.Inject;
import java.util.Arrays;

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
      "%s/repo/%s/%s/pull-requests/%s/",
      configuration.getBaseUrl(),
      repository.getNamespace(),
      repository.getName(),
      pullRequest.getId());
  }

  private void sendMessages(String... messages) {
    HookMessageProvider messageProvider = event.getContext().getMessageProvider();
    Arrays.stream(messages).forEach(messageProvider::sendMessage);
  }
}
