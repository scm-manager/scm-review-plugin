/*
 * Copyright (c) 2020 - present Cloudogu GmbH
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Affero General Public License as published by the Free
 * Software Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see https://www.gnu.org/licenses/.
 */

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

  public static class Builder {

    private final MockedDiffLine diffLine = new MockedDiffLine();

    Builder oldLineNumber(int oldLineNumber) {
      diffLine.oldLineNumber = of(oldLineNumber);
      return this;
    }

    public Builder newLineNumber(int newLineNumber) {
      diffLine.newLineNumber = of(newLineNumber);
      return this;
    }

    Builder content(String content) {
      diffLine.content = content;
      return this;
    }

    public MockedDiffLine get() {
      return diffLine;
    }
  }
}
