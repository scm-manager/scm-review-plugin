package com.cloudogu.scm.review.events;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.security.SessionId;
import javax.ws.rs.sse.OutboundSseEvent;
import javax.ws.rs.sse.SseEventSink;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClientTest {

  @Mock
  private SseEventAdapter adapter;

  @Mock
  private SseEventSink eventSink;

  @Mock
  private OutboundSseEvent event;

  @Mock
  private Message message;

  private Client client;

  @BeforeEach
  void createClient() {
    client = new Client(adapter, eventSink, SessionId.valueOf("1-2-3"));

    when(adapter.create(message)).thenReturn(event);
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

}
