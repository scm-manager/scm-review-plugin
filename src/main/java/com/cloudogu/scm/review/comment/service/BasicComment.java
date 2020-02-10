package com.cloudogu.scm.review.comment.service;

import com.cloudogu.scm.review.XmlInstantAdapter;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static java.util.Collections.unmodifiableList;

@XmlRootElement(name = "comment")
@XmlAccessorType(XmlAccessType.FIELD)
public abstract class BasicComment implements Cloneable {

  private String id;
  private String comment;
  private String author;
  @XmlJavaTypeAdapter(XmlInstantAdapter.class)
  private Instant date;
  private Set<String> mentionUserIds;

  private List<ExecutedTransition> executedTransitions = new ArrayList<>();

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

  public List<ExecutedTransition> getExecutedTransitions() {
    return unmodifiableList(executedTransitions);
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

  public void addTextTransition(ExecutedTransition<TextTransition> transition) {
    this.executedTransitions.add(transition);
  }

  void addTransition(ExecutedTransition<?> transition) {
    this.executedTransitions.add(transition);
  }


  public Set<String> getMentionUserIds() {
    return mentionUserIds;
  }

  public void setMentionUserIds(Set<String> mentionUserIds) {
    this.mentionUserIds = mentionUserIds;
  }

  public BasicComment clone() {
    try {
      return (BasicComment) super.clone();
    } catch (CloneNotSupportedException ex) {
      throw new RuntimeException(ex);
    }
  }
}
