package com.cloudogu.scm.review.comment.service;

import sonia.scm.repository.api.DiffLine;

import java.util.OptionalInt;

import static java.util.OptionalInt.empty;
import static java.util.OptionalInt.of;

public class MockedDiffLine implements DiffLine {

  private OptionalInt oldLineNumber = empty();
  private OptionalInt newLineNumber = empty();
  private String content;

  @Override
  public OptionalInt getOldLineNumber() {
    return oldLineNumber;
  }

  @Override
  public OptionalInt getNewLineNumber() {
    return newLineNumber;
  }

  @Override
  public String getContent() {
    return null;
  }

  static class Builder {

    private final MockedDiffLine diffLine = new MockedDiffLine();

    Builder oldLineNumber(int oldLineNumber) {
      diffLine.oldLineNumber = of(oldLineNumber);
      return this;
    }

    Builder newLineNumber(int newLineNumber) {
      diffLine.newLineNumber = of(newLineNumber);
      return this;
    }

    Builder content(String content) {
      diffLine.content = content;
      return this;
    }

    MockedDiffLine get() {
      return diffLine;
    }
  }
}
