package com.cloudogu.scm.review.events;

import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import com.google.common.annotations.VisibleForTesting;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import sonia.scm.repository.Repository;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.Clock;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

@Singleton
public class ChannelRegistry {

  private final ConcurrentHashMap<ChannelId, Channel> channels = new ConcurrentHashMap<>();
  private Function<ChannelId, Channel> channelFactory;

  @Inject
  public ChannelRegistry(ClientFactory clientFactory) {
    this.channelFactory = id -> new Channel(clientFactory, Clock.systemUTC());
  }

  @VisibleForTesting
  void setChannelFactory(Function<ChannelId, Channel> channelFactory) {
    this.channelFactory = channelFactory;
  }

  public Channel channel(Repository repository, PullRequest pullRequest) {
    removeClosedClients();
    ChannelId channelId = new ChannelId(repository.getId(), pullRequest.getId());
    return channels.computeIfAbsent(channelId, channelFactory);
  }

  void removeClosedClients() {
    channels.values().forEach(Channel::removeClosedOrTimeoutClients);
  }

  @EqualsAndHashCode
  @AllArgsConstructor
  static class ChannelId {
    private final String repositoryId;
    private final String pullRequestId;
  }
}
