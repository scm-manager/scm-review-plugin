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

  private void removeClosedClients() {
    channels.values().forEach(Channel::removeClosedOrTimeoutClients);
  }

  @EqualsAndHashCode
  @AllArgsConstructor
  static class ChannelId {
    private final String repositoryId;
    private final String pullRequestId;
  }
}
