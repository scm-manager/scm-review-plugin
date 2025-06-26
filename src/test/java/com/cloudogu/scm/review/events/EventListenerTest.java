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

import com.cloudogu.scm.review.TestData;
import com.cloudogu.scm.review.comment.service.Comment;
import com.cloudogu.scm.review.comment.service.CommentEvent;
import com.cloudogu.scm.review.comment.service.Reply;
import com.cloudogu.scm.review.comment.service.ReplyEvent;
import com.cloudogu.scm.review.pullrequest.service.BasicPullRequestEvent;
import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import com.cloudogu.scm.review.pullrequest.service.PullRequestEvent;
import com.cloudogu.scm.review.pullrequest.service.PullRequestMergedEvent;
import com.cloudogu.scm.review.pullrequest.service.PullRequestRejectedEvent;
import com.cloudogu.scm.review.pullrequest.service.PullRequestUpdatedEvent;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.HandlerEventType;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryTestData;
import sonia.scm.security.SessionId;
import sonia.scm.sse.Channel;
import sonia.scm.sse.ChannelRegistry;
import sonia.scm.sse.Message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventListenerTest {

  @Mock
  private Subject subject;

  @Mock
  private PrincipalCollection principalCollection;

  @Mock
  private ChannelRegistry registry;

  @Mock
  private Channel channel;

  @InjectMocks
  private EventListener listener;

  @Captor
  private ArgumentCaptor<Message> captor;

  @BeforeEach
  void bindSubject() {
    ThreadContext.bind(subject);
    lenient().when(subject.getPrincipals()).thenReturn(principalCollection);
  }

  @AfterEach
  void unbindSubject() {
    ThreadContext.unbindSubject();
  }

  private SessionId bindSessionId(String sessionIdString) {
    SessionId sessionId = SessionId.valueOf(sessionIdString);
    when(principalCollection.oneByType(SessionId.class)).thenReturn(sessionId);
    return sessionId;
  }

  @Test
  void shouldBroadcastMessageForPullRequestEvent() {
    bindSessionId("1-2-3");
    PullRequestEvent event = createPullRequestEvent();

    listener.handle(event);

    verify(channel).broadcast(captor.capture());

    assertMessageHasCorrectTypeAndName(PullRequestEvent.class);
  }

  @Test
  void shouldSendSessionIdWithMessage() {
    SessionId sessionId = bindSessionId("1-2-3");
    PullRequestEvent event = createPullRequestEvent();

    listener.handle(event);

    verify(channel).broadcast(captor.capture());

    assertMessageHasCorrectTypeAndName(PullRequestEvent.class);
    assertThat(captor.getValue().getSender()).contains(sessionId);
  }

  @Test
  void shouldBroadcastPullRequestMergedEvent() {
    bindSessionId("1-2-3");
    PullRequestMergedEvent event = createPullRequestMergedEvent();

    listener.handle(event);

    verify(channel).broadcast(captor.capture());

    assertMessageHasCorrectTypeAndName(PullRequestMergedEvent.class);
  }

  @Test
  void shouldBroadcastPullRequestRejectedEvent() {
    bindSessionId("1-2-3");
    PullRequestRejectedEvent event = createPullRequestRejectedEvent();

    listener.handle(event);

    verify(channel).broadcast(captor.capture());

    assertMessageHasCorrectTypeAndName(PullRequestRejectedEvent.class);
  }

  @Test
  void shouldBroadcastPullRequestUpdatedEvent() {
    bindSessionId("1-2-3");
    PullRequestUpdatedEvent event = createPullRequestUpdatedEvent();

    listener.handle(event);

    verify(channel).broadcast(captor.capture());

    assertMessageHasCorrectTypeAndName(PullRequestUpdatedEvent.class);
  }

  @Test
  void shouldBroadcastMessageForCommentEvent() {
    bindSessionId("4-2");
    CommentEvent event = createCommentEvent();

    listener.handle(event);

    verify(channel).broadcast(captor.capture());

    assertMessageHasCorrectTypeAndName(CommentEvent.class);
  }

  @Test
  void shouldBroadcastMessageForReplyEvent() {
    SessionId sessionId = bindSessionId("4-2");
    ReplyEvent event = createReplyEvent();

    listener.handle(event);

    verify(channel).broadcast(captor.capture());

    assertMessageHasCorrectTypeAndName(ReplyEvent.class);
  }

  private void assertMessageHasCorrectTypeAndName(Class<? extends BasicPullRequestEvent> event) {
    Message message = captor.getValue();
    assertThat(message.getType()).isEqualTo(String.class);
    assertThat(message.getData()).isEqualTo(event.getName());
  }

  private ReplyEvent createReplyEvent() {
    Repository repository = RepositoryTestData.createHeartOfGold();
    PullRequest pullRequest = TestData.createPullRequest();

    mockChannel(repository, pullRequest);

    Reply reply = new Reply();
    reply.setId("c42");
    return new ReplyEvent(repository, pullRequest, reply, reply, null, HandlerEventType.MODIFY);
  }

  private void mockChannel(Repository repository, PullRequest pullRequest) {
    when(registry.channel(new ChannelId(repository, pullRequest))).thenReturn(channel);
  }

  private CommentEvent createCommentEvent() {
    Repository repository = RepositoryTestData.createHeartOfGold();
    PullRequest pullRequest = TestData.createPullRequest();

    mockChannel(repository, pullRequest);

    Comment comment = new Comment();
    comment.setId("c42");
    return new CommentEvent(repository, pullRequest, null, comment, HandlerEventType.DELETE);
  }

  private PullRequestEvent createPullRequestEvent() {
    Repository repository = RepositoryTestData.createHeartOfGold();
    PullRequest pullRequest = TestData.createPullRequest();

    mockChannel(repository, pullRequest);

    return new PullRequestEvent(repository, pullRequest, null, HandlerEventType.CREATE);
  }

  private PullRequestUpdatedEvent createPullRequestUpdatedEvent() {
    Repository repository = RepositoryTestData.createHeartOfGold();
    PullRequest pullRequest = TestData.createPullRequest();
    mockChannel(repository, pullRequest);
    return new PullRequestUpdatedEvent(
      repository,
      pullRequest
    );
  }

  private PullRequestMergedEvent createPullRequestMergedEvent() {
    Repository repository = RepositoryTestData.createHeartOfGold();
    PullRequest pullRequest = TestData.createPullRequest();
    mockChannel(repository, pullRequest);
    return new PullRequestMergedEvent(
      repository,
      pullRequest
    );
  }

  private PullRequestRejectedEvent createPullRequestRejectedEvent() {
    Repository repository = RepositoryTestData.createHeartOfGold();
    PullRequest pullRequest = TestData.createPullRequest();
    mockChannel(repository, pullRequest);
    return new PullRequestRejectedEvent(
      repository,
      pullRequest,
      PullRequestRejectedEvent.RejectionCause.SOURCE_BRANCH_DELETED
    );
  }

}
