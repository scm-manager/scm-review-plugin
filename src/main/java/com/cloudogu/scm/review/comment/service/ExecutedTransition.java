package com.cloudogu.scm.review.comment.service;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "transition")
@XmlAccessorType(XmlAccessType.FIELD)
public class ExecutedTransition {

  private CommentTransition transition;
  private long date;
  private String user;

  public ExecutedTransition() {
  }

  public ExecutedTransition(CommentTransition transition, long date, String user) {
    this.transition = transition;
    this.date = date;
    this.user = user;
  }

  public CommentTransition getTransition() {
    return transition;
  }

  public long getDate() {
    return date;
  }

  public String getUser() {
    return user;
  }
}
