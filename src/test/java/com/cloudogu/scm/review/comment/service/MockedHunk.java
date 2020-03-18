/**
 * MIT License
 *
 * Copyright (c) 2020-present Cloudogu GmbH and Contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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
