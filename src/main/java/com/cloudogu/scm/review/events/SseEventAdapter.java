package com.cloudogu.scm.review.events;

import com.google.common.annotations.VisibleForTesting;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.sse.OutboundSseEvent;
import javax.ws.rs.sse.Sse;

class SseEventAdapter {

  @VisibleForTesting
  static final String NAME = "pullRequest";

  private final Sse sse;

  SseEventAdapter(Sse sse) {
    this.sse = sse;
  }

  OutboundSseEvent create(Message message) {
    return sse.newEventBuilder()
      .name(NAME)
      .mediaType(MediaType.APPLICATION_JSON_TYPE)
      .data(message.getType(), message.getData())
      .build();
  }

}
