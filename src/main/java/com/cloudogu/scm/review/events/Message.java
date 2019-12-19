package com.cloudogu.scm.review.events;

import lombok.Value;

@Value
public class Message {
  private final Class<?> type;
  private final Object data;
}
