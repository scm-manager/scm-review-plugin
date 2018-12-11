package com.cloudogu.scm.review.comment.service;

import lombok.Data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

@Data
@XmlRootElement(name = "pull-request-comments")
@XmlAccessorType(XmlAccessType.FIELD)
public class PullRequestComments {

  @XmlElement(name = "comment")
  private List<PullRequestComment> comments = new ArrayList<>();
}
