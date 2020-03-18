/**
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
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.security.SessionId;

import javax.ws.rs.sse.OutboundSseEvent;
import javax.ws.rs.sse.SseEventSink;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoField;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;

import static java.time.temporal.ChronoUnit.MINUTES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClientTest {

  @Mock
  private SseEventAdapter adapter;

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private SseEventSink eventSink;

  @Mock
  private OutboundSseEvent event;

  @Mock
  private Message message;

  @Mock
  private Clock clock;

  private Client client;

  private static final Instant INITIAL_DATE = LocalDateTime.of(2019, 12, 17, 13, 0).toInstant(ZoneOffset.UTC);

  @BeforeEach
  void mockTheClock() {
    final AtomicReference<Instant> ref = new AtomicReference<>(INITIAL_DATE);
    when(clock.instant()).then(ic -> {
      Instant date = ref.get();
      date = date.plus(1L, MINUTES);
      ref.set(date);
      return date;
    });
  }

  @BeforeEach
  void createClient() {
    client = new Client(adapter, eventSink, SessionId.valueOf("1-2-3"), clock);
    lenient().when(adapter.create(message)).thenReturn(event);
  }

  @Test
  void shouldBroadcastEvent() {
    client.send(message);

    verify(eventSink).send(event);
  }

  @Test
  void shouldNotBroadcastToClosedClients() {
    when(eventSink.isClosed()).thenReturn(true);

    client.send(message);

    verify(eventSink, never()).send(event);
  }

  @Test
  @SuppressWarnings("unchecked")
  void shouldCloseEventSinkOnFailure() {
    CompletionStage future = CompletableFuture.supplyAsync(() -> {
      throw new RuntimeException("failed to send message");
    });
    when(eventSink.send(any())).thenReturn(future);

    client.send(message);

    verify(eventSink, timeout(50L)).close();
  }

  @Test
  void shouldSetLastUsedAfterCreation() {
    Instant instant = client.getLastUsed();
    assertThat(instant.atZone(ZoneOffset.UTC).get(ChronoField.MINUTE_OF_HOUR)).isEqualTo(1L);
  }

  @Test
  void shouldUpdateLastUsedAfterSend() {
    assertThat(client.getLastUsed().atZone(ZoneOffset.UTC).get(ChronoField.MINUTE_OF_HOUR)).isEqualTo(1L);
    client.send(message);
    assertThat(client.getLastUsed().atZone(ZoneOffset.UTC).get(ChronoField.MINUTE_OF_HOUR)).isEqualTo(2L);
    client.send(message);
    assertThat(client.getLastUsed().atZone(ZoneOffset.UTC).get(ChronoField.MINUTE_OF_HOUR)).isEqualTo(3L);
  }

}
