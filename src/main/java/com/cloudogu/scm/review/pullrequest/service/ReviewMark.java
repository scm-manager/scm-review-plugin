package com.cloudogu.scm.review.pullrequest.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@AllArgsConstructor
@NoArgsConstructor
@Data
@XmlRootElement(name = "mark")
@XmlAccessorType(XmlAccessType.FIELD)
public class ReviewMark {

  private String file;
  private String user;
}
