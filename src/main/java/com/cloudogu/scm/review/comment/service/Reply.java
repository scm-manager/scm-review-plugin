package com.cloudogu.scm.review.comment.service;

import java.time.Instant;
import java.util.Collections;

public class Reply extends BasicComment {

  public static Reply createReply(String id, String text, String author) {
    Reply comment = new Reply();
    comment.setId(id);
    comment.setComment(text);
    comment.setAuthor(author);
    comment.setDate(Instant.now());
    comment.setMentionUserIds(Collections.emptySet());
    return comment;
  }

  @Override
  public Reply clone() {
    return (Reply) super.clone();
  }
}
