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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.security.SessionId;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChannelTest {

  @Mock
  private ClientFactory clientFactory;

  @Mock
  private Clock clock;

  @InjectMocks
  private Channel channel;

  @BeforeEach
  void mockTheClock() {
    lenient().when(clock.instant()).then(ic -> Instant.now());
  }

  @Test
  void shouldNotifyClient() {
    Client client = register("1-2-3");
    Message message = broadcast("1-2-4");
    verify(client).send(message);
  }

  @Test
  void shouldNotNotifySender() {
    Client client = register("1-2-3");
    Message message = broadcast("1-2-3");
    verify(client, never()).send(message);
  }

  @Test
  void shouldRemoveClosedClients() {
    Client closedClient = register("1-2-3");
    when(closedClient.isClosed()).thenReturn(true);
    Client activeClient = register("1-2-4");

    channel.removeClosedOrTimeoutClients();

    Message message = broadcast("1-2-5");
    verify(closedClient, never()).send(message);
    verify(activeClient).send(message);
  }

  @Test
  void shouldRemoveClientsWhichAreNotUsedWithin30Seconds() {
    Client timedOutClient = register("1-2-3");
    when(timedOutClient.getLastUsed()).thenReturn(Instant.now().minus(31L, ChronoUnit.SECONDS));
    Client activeClient = register("1-2-4");
    when(activeClient.getLastUsed()).thenReturn(Instant.now().minus(29L, ChronoUnit.SECONDS));

    channel.removeClosedOrTimeoutClients();

    Message message = broadcast("1-2-5");
    verify(timedOutClient, never()).send(message);
    verify(timedOutClient).close();
    verify(activeClient).send(message);
    verify(activeClient, never()).close();
  }

  @Test
  void shouldNotifyAllClientsForMessagesWithoutSenderId() {
    Client one = register("1-2-3");
    Client two = register("1-2-4");
    Client three = register(null);

    Message message = broadcast(null);

    verify(one).send(message);
    verify(two).send(message);
    verify(three).send(message);
  }

  private Message broadcast(String sender) {
    Message message = new Message(String.class, "ka");
    SessionId senderSession = sessionId(sender);
    channel.broadcast(senderSession, message);
    return message;
  }

  private Client register(String sessionIdString) {
    SessionId sessionId = sessionId(sessionIdString);
    Registration registration = new Registration(null, null, sessionId);
    Client client = mock(Client.class);
    lenient().when(client.getSessionId()).thenReturn(sessionId);
    lenient().when(client.getLastUsed()).thenReturn(Instant.now());
    when(clientFactory.create(registration)).thenReturn(client);
    channel.register(registration);
    return client;
  }

  private SessionId sessionId(String sessionIdString) {
    SessionId sessionId = null;
    if (sessionIdString != null) {
      sessionId = SessionId.valueOf(sessionIdString);
    }
    return sessionId;
  }

}
