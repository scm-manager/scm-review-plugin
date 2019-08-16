package com.cloudogu.scm.review.comment.service;

import sonia.scm.repository.api.DiffLine;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import java.util.OptionalInt;

@XmlAccessorType(XmlAccessType.FIELD)
public class ContextLine implements DiffLine {

  private Integer oldLineNumber;
  private Integer newLineNumber;
  private String content;

  @Override
  public OptionalInt getOldLineNumber() {
    return oldLineNumber == null? OptionalInt.empty(): OptionalInt.of(oldLineNumber);
  }

  @Override
  public OptionalInt getNewLineNumber() {
    return newLineNumber == null? OptionalInt.empty(): OptionalInt.of(newLineNumber);
  }

  @Override
  public String getContent() {
    return content;
  }

  static ContextLine copy(DiffLine diffLine) {
    ContextLine contextLine = new ContextLine();
    contextLine.oldLineNumber = diffLine.getOldLineNumber().isPresent() ? diffLine.getOldLineNumber().getAsInt() : null;
    contextLine.newLineNumber = diffLine.getNewLineNumber().isPresent() ? diffLine.getNewLineNumber().getAsInt() : null;
    contextLine.content = diffLine.getContent();
    return contextLine;
  }
}
