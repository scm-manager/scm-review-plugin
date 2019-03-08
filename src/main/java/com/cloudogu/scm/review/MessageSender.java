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

  @Inject
  public MessageSender(ScmConfiguration configuration) {
    this.configuration = configuration;
  }

  public void sendMessageForPullRequest(PostReceiveRepositoryHookEvent event, PullRequest pullRequest, String message) {
    sendMessages(event, message, createPullRequestLink(event, pullRequest));
  }

  public void sendCreatePullRequestMessage(PostReceiveRepositoryHookEvent event, String branch, String message) {
    sendMessages(event, message, createCreateLink(event.getRepository(), branch));
  }

  private String createCreateLink(Repository repository, String source) {
    return String.format(
      "%s/repo/%s/%s/pull-requests/add/changesets/?source=%s",
      configuration.getBaseUrl(),
      repository.getNamespace(),
      repository.getName(),
      source);
  }

  private String createPullRequestLink(PostReceiveRepositoryHookEvent event, PullRequest pullRequest) {
    Repository repository = event.getRepository();
    return String.format(
      "%s/repo/%s/%s/pull-requests/%s/",
      configuration.getBaseUrl(),
      repository.getNamespace(),
      repository.getName(),
      pullRequest.getId());
  }

  private void sendMessages(PostReceiveRepositoryHookEvent event, String... messages) {
    HookMessageProvider messageProvider = event.getContext().getMessageProvider();
    Arrays.stream(messages).forEach(messageProvider::sendMessage);
  }
}
