package com.cloudogu.scm.review.comment.service;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@XmlRootElement(name = "context")
@XmlAccessorType(XmlAccessType.FIELD)
public class InlineContext {

  private List<ContextLine> lines;

  public List<ContextLine> getLines() {
    return lines;
  }
}
