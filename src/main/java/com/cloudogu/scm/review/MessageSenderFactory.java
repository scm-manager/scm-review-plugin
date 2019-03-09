package com.cloudogu.scm.review;

import sonia.scm.config.ScmConfiguration;
import sonia.scm.repository.PostReceiveRepositoryHookEvent;

import javax.inject.Inject;

public class MessageSenderFactory {

  private final ScmConfiguration configuration;

  @Inject
  public MessageSenderFactory(ScmConfiguration configuration) {
    this.configuration = configuration;
  }

  public MessageSender create(PostReceiveRepositoryHookEvent event) {
    return new MessageSender(configuration, event);
  }
}
