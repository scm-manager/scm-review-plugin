package com.cloudogu.scm.review.pullrequest.service;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@XmlRootElement(name = "mark")
@XmlAccessorType(XmlAccessType.FIELD)
@EqualsAndHashCode
public class ReviewMark {

  private String file;
  private String user;
}
