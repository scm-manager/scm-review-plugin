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

package com.cloudogu.scm.review.events;

import com.cloudogu.scm.review.pullrequest.service.BasicPullRequestEvent;
import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import com.github.legman.Subscribe;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.Subject;
import sonia.scm.EagerSingleton;
import sonia.scm.plugin.Extension;
import sonia.scm.repository.Repository;
import sonia.scm.security.SessionId;
import sonia.scm.sse.Channel;
import sonia.scm.sse.ChannelRegistry;
import sonia.scm.sse.Message;

@Singleton
@Extension
@EagerSingleton
class EventListener {

  private final ChannelRegistry registry;

  @Inject
  public EventListener(ChannelRegistry registry) {
    this.registry = registry;
  }

  @Subscribe
  public void handle(BasicPullRequestEvent event) {
    Channel channel = channel(event);
    channel.broadcast(message(event));
  }

  private Message message(BasicPullRequestEvent event) {
    return new Message("pullRequest", String.class, event.getClass().getName(), senderId());
  }

  private SessionId senderId() {
    Subject subject = SecurityUtils.getSubject();
    PrincipalCollection principals = subject.getPrincipals();
    return principals.oneByType(SessionId.class);
  }

  private Channel channel(BasicPullRequestEvent event) {
    return channel(event.getRepository(), event.getPullRequest());
  }

  private Channel channel(Repository repository, PullRequest pullRequest) {
    return registry.channel(new ChannelId(repository, pullRequest));
  }
}
