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
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalField;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicLong;
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
  void shouldCloseEventSinkOnFailure() throws InterruptedException {
    CompletionStage future = CompletableFuture.supplyAsync(() -> {
      throw new RuntimeException("failed to send message");
    });
    when(eventSink.send(any())).thenReturn(future);

    client.send(message);

    Thread.sleep(50L);

    verify(eventSink).close();
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
