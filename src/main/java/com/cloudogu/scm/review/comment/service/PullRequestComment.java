package com.cloudogu.scm.review.comment.service;

import com.cloudogu.scm.review.XmlInstantAdapter;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.time.Instant;

@XmlRootElement(name = "comment")
@XmlAccessorType(XmlAccessType.FIELD)
public abstract class PullRequestComment implements Cloneable {

  private String id;
  private String comment;
  private String author;
  @XmlJavaTypeAdapter(XmlInstantAdapter.class)
  private Instant date;

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

  public PullRequestComment clone() {
    try {
      return (PullRequestComment) super.clone();
    } catch (CloneNotSupportedException ex) {
      throw new RuntimeException(ex);
    }
  }
}
