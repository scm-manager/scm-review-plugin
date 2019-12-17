package com.cloudogu.scm.review.events;

import java.time.Clock;

class ClientFactory {

  Client create(Registration registration) {
    SseEventAdapter adapter = new SseEventAdapter(registration.getSse());
    return new Client(adapter, registration.getEventSink(), registration.getSessionId(), Clock.systemUTC());
  }

}
