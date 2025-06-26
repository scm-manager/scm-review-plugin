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

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import sonia.scm.repository.api.DiffLine;

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
