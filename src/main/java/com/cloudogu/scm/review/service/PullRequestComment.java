package com.cloudogu.scm.review.service;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@XmlRootElement(name = "pull-request")
@XmlAccessorType(XmlAccessType.FIELD)

public class PullRequestComment {
  private String comment;
  private String author;
  private Instant date;
}
