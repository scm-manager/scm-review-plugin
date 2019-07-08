package com.cloudogu.scm.review.comment.service;

import java.time.Instant;

public class Reply extends PullRequestComment {

  public static Reply createReply(String id, String text, String author) {
    Reply comment = new Reply();
    comment.setId(id);
    comment.setComment(text);
    comment.setAuthor(author);
    comment.setDate(Instant.now());
    return comment;
  }

  @Override
  public Reply clone() {
    return (Reply) super.clone();
  }
}
