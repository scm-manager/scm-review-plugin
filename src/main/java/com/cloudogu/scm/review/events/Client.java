package com.cloudogu.scm.review.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sonia.scm.security.SessionId;

import javax.ws.rs.sse.OutboundSseEvent;
import javax.ws.rs.sse.SseEventSink;
import java.io.Closeable;
import java.time.Clock;
import java.time.Instant;

class Client implements Closeable {

  private static final Logger LOG = LoggerFactory.getLogger(Client.class);

  private final SseEventAdapter eventAdapter;
  private final SseEventSink eventSink;
  private final SessionId sessionId;
  private final Clock clock;
  private Instant lastUsed;

  Client(SseEventAdapter eventAdapter, SseEventSink eventSink, SessionId sessionId, Clock clock) {
    this.eventAdapter = eventAdapter;
    this.eventSink = eventSink;
    this.sessionId = sessionId;
    this.clock = clock;
    this.lastUsed = Instant.now(clock);
  }

  public Instant getLastUsed() {
    return lastUsed;
  }

  SessionId getSessionId() {
    return sessionId;
  }

  void send(Message message) {
    OutboundSseEvent event = eventAdapter.create(message);
    if (!isClosed()) {
      LOG.debug("send message to client with session id {}", getSessionId());
      lastUsed = Instant.now(clock);
      eventSink.send(event).exceptionally(e -> {
        if (LOG.isTraceEnabled()) {
          LOG.trace("failed to send event to client with session id {}:", sessionId, e);
        } else {
          LOG.debug("failed to send event to client with session id {}: {}", sessionId, e.getMessage());
        }
        close();
        return null;
      });
    } else {
      LOG.debug("client has closed the connection, before we could send the message");
    }
  }

  boolean isClosed() {
    return eventSink.isClosed();
  }

  @Override
  public void close() {
    LOG.trace("close sse session of client with session id: {}", sessionId);
    eventSink.close();
  }
}
