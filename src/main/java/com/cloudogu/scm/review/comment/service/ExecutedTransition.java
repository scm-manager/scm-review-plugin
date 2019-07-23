package com.cloudogu.scm.review.comment.service;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "transition")
@XmlAccessorType(XmlAccessType.FIELD)
public class ExecutedTransition<T extends Transition> {

  private String id;
  private T transition;
  private long date;
  private String user;

  public ExecutedTransition() {
  }

  public ExecutedTransition(String id, T transition, long date, String user) {
    this.id = id;
    this.transition = transition;
    this.date = date;
    this.user = user;
  }

  public String getId() {
    return id;
  }

  public T getTransition() {
    return transition;
  }

  public long getDate() {
    return date;
  }

  public String getUser() {
    return user;
  }
}
