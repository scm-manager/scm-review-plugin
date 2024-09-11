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
import sonia.scm.repository.api.Hunk;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.OptionalInt;

public class MockedHunk implements Hunk {

  private Collection<DiffLine> diffLines = new ArrayList<>();

  @Override
  public int getOldStart() {
    return diffLines.stream().map(DiffLine::getOldLineNumber).filter(OptionalInt::isPresent).map(OptionalInt::getAsInt).mapToInt(i -> i).min().orElse(0);
  }

  @Override
  public int getOldLineCount() {
    return (int) diffLines.stream().map(DiffLine::getOldLineNumber).filter(OptionalInt::isPresent).count();
  }

  @Override
  public int getNewStart() {
    return diffLines.stream().map(DiffLine::getNewLineNumber).filter(OptionalInt::isPresent).map(OptionalInt::getAsInt).mapToInt(i -> i).min().orElse(0);
  }

  @Override
  public int getNewLineCount() {
    return (int) diffLines.stream().map(DiffLine::getNewLineNumber).filter(OptionalInt::isPresent).count();
  }

  @Override
  public Iterator<DiffLine> iterator() {
    return diffLines.iterator();
  }

  static class Builder {

    private final MockedHunk hunk = new MockedHunk();

    Builder addDiffLine(DiffLine diffLine) {
      hunk.diffLines.add(diffLine);
      return this;
    }

    MockedHunk get() {
      return hunk;
    }
  }
}
