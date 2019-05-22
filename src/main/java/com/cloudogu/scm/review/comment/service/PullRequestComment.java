package com.cloudogu.scm.review.comment.service;

import com.cloudogu.scm.review.XmlInstantAdapter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import sonia.scm.repository.Repository;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.io.Serializable;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@XmlRootElement(name = "comment")
@XmlAccessorType(XmlAccessType.FIELD)
@Builder(toBuilder = true)
public class PullRequestComment implements Cloneable {

  private String id;
  private String comment;
  private String author;
  private Location location;
  @XmlJavaTypeAdapter(XmlInstantAdapter.class)
  private Instant date;
  private boolean systemComment;
  private boolean done;

  @Override
  public PullRequestComment clone() {
    try {
      return (PullRequestComment) super.clone();
    } catch (CloneNotSupportedException ex) {
      throw new RuntimeException(ex);
    }
  }
}
