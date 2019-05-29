package com.cloudogu.scm.review.comment.service;

import com.cloudogu.scm.review.XmlInstantAdapter;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.time.Instant;

@XmlRootElement(name = "comment")
@XmlAccessorType(XmlAccessType.FIELD)
public class PullRequestComment implements Cloneable {

  public static PullRequestComment createResponse(String id, String text, String author) {
    PullRequestComment comment = new PullRequestComment();
    comment.setId(id);
    comment.setComment(text);
    comment.setAuthor(author);
    comment.setDate(Instant.now());
    return comment;
  }

  private String id;
  private String comment;
  private String author;
  @XmlJavaTypeAdapter(XmlInstantAdapter.class)
  private Instant date;
  private boolean systemComment;
  private boolean done;

  public String getId() {
    return id;
  }

  public String getComment() {
    return comment;
  }

  public String getAuthor() {
    return author;
  }

  public Instant getDate() {
    return date;
  }

  public boolean isSystemComment() {
    return systemComment;
  }

  public boolean isDone() {
    return done;
  }

  public void setId(String id) {
    this.id = id;
  }

  public void setAuthor(String author) {
    this.author = author;
  }

  public void setDate(Instant date) {
    this.date = date;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }

  public void setDone(boolean done) {
    this.done = done;
  }

  void setSystemComment(boolean systemComment) {
    this.systemComment = systemComment;
  }

  public PullRequestComment clone() {
    try {
      return (PullRequestComment) super.clone();
    } catch (CloneNotSupportedException ex) {
      throw new RuntimeException(ex);
    }
  }
}
