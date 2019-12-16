package com.cloudogu.scm.review.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.sse.OutboundSseEvent;
import javax.ws.rs.sse.SseEventSink;

class Client {

  private static final Logger LOG = LoggerFactory.getLogger(Client.class);

  private final SseEventAdapter eventAdapter;
  private final SseEventSink eventSink;
  private final String sessionId;

  Client(SseEventAdapter eventAdapter, SseEventSink eventSink, String sessionId) {
    this.eventAdapter = eventAdapter;
    this.eventSink = eventSink;
    this.sessionId = sessionId;
  }

  String getSessionId() {
    return sessionId;
  }

  void send(Message message) {
    OutboundSseEvent event = eventAdapter.create(message);
    if (!isClosed()) {
      LOG.debug("send message to client with session id {}", sessionId);
      eventSink.send(event);
    } else {
      LOG.debug("client has closed the connection, before we could send the message");
    }
  }

  boolean isClosed() {
    return eventSink.isClosed();
  }
}
