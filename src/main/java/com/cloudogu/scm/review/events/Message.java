package com.cloudogu.scm.review.events;

import lombok.Value;

@Value
public class Message {

  private final Type type;
  private final Object payload;

  public enum Type {
    PULL_REQUEST,
    COMMENT,
    REPLY
  }
}
