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
