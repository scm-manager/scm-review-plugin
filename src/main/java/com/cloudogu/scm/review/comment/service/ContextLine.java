/*
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

  public static ContextLine copy(DiffLine diffLine) {
    ContextLine contextLine = new ContextLine();
    contextLine.oldLineNumber = diffLine.getOldLineNumber().isPresent() ? diffLine.getOldLineNumber().getAsInt() : null;
    contextLine.newLineNumber = diffLine.getNewLineNumber().isPresent() ? diffLine.getNewLineNumber().getAsInt() : null;
    contextLine.content = diffLine.getContent();
    return contextLine;
  }
}
