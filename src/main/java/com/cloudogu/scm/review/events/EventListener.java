package com.cloudogu.scm.review.events;

import com.cloudogu.scm.review.comment.service.Comment;
import com.cloudogu.scm.review.comment.service.CommentEvent;
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
import sonia.scm.security.SessionId;
import sonia.scm.repository.Repository;

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
  public void handle(PullRequestEvent event) {
    Channel channel = channel(event);
    channel.broadcast(senderId(), message(event));
  }

  @Subscribe
  public void handle(CommentEvent event) {
    Channel channel = channel(event);
    channel.broadcast(senderId(), message(event));
  }

  private Message message(PullRequestEvent event) {
    PullRequestPayload payload = new PullRequestPayload();
    payload.setChangeType(event.getEventType());
    return new Message(Message.Type.PULL_REQUEST, payload);
  }

  private SessionId senderId() {
    Subject subject = SecurityUtils.getSubject();
    PrincipalCollection principals = subject.getPrincipals();
    return principals.oneByType(SessionId.class);
  }

  private Message message(CommentEvent event) {
    CommentPayload payload = new CommentPayload();
    payload.setChangeType(event.getEventType());
    Comment comment = changedItem(event);
    if (comment != null) {
      payload.setCommentId(comment.getId());
    }
    return new Message(Message.Type.COMMENT, payload);
  }

  private Channel channel(PullRequestEvent event) {
    return channel(event.getRepository(), changedItem(event));
  }

  private <T> T changedItem(HandlerEvent<T> event) {
    T item = event.getItem();
    if (item == null) {
      item = event.getOldItem();
    }
    return item;
  }

  private Channel channel(CommentEvent event) {
    return channel(event.getRepository(), event.getPullRequest());
  }

  private Channel channel(Repository repository, PullRequest pullRequest) {
    return registry.channel(repository, pullRequest);
  }

  @Data
  static class PullRequestPayload {
    private HandlerEventType changeType;
  }

  @Data
  static class CommentPayload {
    private HandlerEventType changeType;
    private String commentId;
  }
}
