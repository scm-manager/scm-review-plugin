package com.cloudogu.scm.review.events;

import lombok.Value;

import sonia.scm.security.SessionId;

import javax.ws.rs.sse.Sse;
import javax.ws.rs.sse.SseEventSink;

@Value
public class Registration {
  private final Sse sse;
  private final SseEventSink eventSink;
  private final SessionId sessionId;
}
