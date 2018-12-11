package com.cloudogu.scm.review.comment.service;

import com.cloudogu.scm.review.XmlInstantAdapter;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@XmlRootElement(name = "comment")
@XmlAccessorType(XmlAccessType.FIELD)
public class PullRequestComment {

  private int id;
  private String comment;
  private String author;
  @XmlJavaTypeAdapter(XmlInstantAdapter.class)
  private Instant date;
}
