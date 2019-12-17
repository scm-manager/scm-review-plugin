package com.cloudogu.scm.review.events;

import com.cloudogu.scm.review.comment.service.BasicComment;
import com.cloudogu.scm.review.comment.service.BasicCommentEvent;
import com.cloudogu.scm.review.comment.service.CommentEvent;
import com.cloudogu.scm.review.comment.service.ReplyEvent;
import com.cloudogu.scm.review.pullrequest.service.BasicPullRequestEvent;
import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import com.cloudogu.scm.review.pullrequest.service.PullRequestEvent;
import com.github.legman.Subscribe;
import lombok.Data;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.Subject;
import sonia.scm.EagerSingleton;
import sonia.scm.HandlerEventType;
import sonia.scm.event.HandlerEvent;
import sonia.scm.plugin.Extension;
import sonia.scm.repository.Repository;
import sonia.scm.security.SessionId;

import javax.inject.Inject;
import javax.inject.Singleton;

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
    channel.broadcast(senderId(), message(event));
  }

  private Message message(BasicPullRequestEvent event) {
    return new Message(String.class, event.getClass().getName());
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
    return registry.channel(repository, pullRequest);
  }
}
