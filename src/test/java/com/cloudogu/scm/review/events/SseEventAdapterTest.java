package com.cloudogu.scm.review.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.jboss.resteasy.plugins.providers.sse.SseImpl;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.sse.OutboundSseEvent;

import static org.assertj.core.api.Assertions.assertThat;

class SseEventAdapterTest {

  private final SseEventAdapter adapter = new SseEventAdapter(new SseImpl());

  @Test
  void shouldCreateOutboundSseEvent() {
    Payload payload = new Payload("hello");
    Message message = new Message(Payload.class, payload);

    OutboundSseEvent event = adapter.create(message);

    assertThat(event.getName()).isEqualTo(SseEventAdapter.NAME);
    assertThat(event.getMediaType()).isEqualTo(MediaType.APPLICATION_JSON_TYPE);
    assertThat(event.getData()).isEqualTo(payload);
    assertThat(event.getType()).isSameAs(Payload.class);
  }

  @Data
  @AllArgsConstructor
  public static class Payload {
    private String content;
  }

}
