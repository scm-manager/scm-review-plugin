package com.cloudogu.scm.review.comment.service;

public enum SystemCommentType {

  MERGED("merged"),
  REJECTED("rejected"),
  SOURCE_DELETED("sourceDeleted");

  private final String key;

  SystemCommentType(String key) {
    this.key = key;
  }

  public String getKey() {
    return key;
  }
}
