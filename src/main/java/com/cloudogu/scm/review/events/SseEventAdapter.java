package com.cloudogu.scm.review.events;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.sse.OutboundSseEvent;
import javax.ws.rs.sse.Sse;

class SseEventAdapter {

  private final Sse sse;

  SseEventAdapter(Sse sse) {
    this.sse = sse;
  }

  OutboundSseEvent create(Message message) {
    return sse.newEventBuilder()
      .name(message.getType().name())
      .mediaType(MediaType.APPLICATION_JSON_TYPE)
      .data(message.getPayload())
      .build();
  }

}
