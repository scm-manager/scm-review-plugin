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

package com.cloudogu.scm.review.pullrequest.service;

import sonia.scm.ContextEntry;
import sonia.scm.ExceptionWithContext;
import sonia.scm.repository.NamespaceAndName;
import sonia.scm.repository.api.MergeCommandResult;

import java.util.List;

import static sonia.scm.ContextEntry.ContextBuilder.entity;

public class MergeConflictException extends ExceptionWithContext {
  private static final String CODE = "DTRhFJFgU1";

  public MergeConflictException(NamespaceAndName namespaceAndName, String source, String target, MergeCommandResult mergeResult) {
    super(
      createContext(namespaceAndName, source, target, mergeResult),
      String.format("conflict in merge between %s and %s", source, target));
  }

  private static List<ContextEntry> createContext(NamespaceAndName namespaceAndName, String source, String target, MergeCommandResult mergeResult) {
    return entity("files", String.join(", ", mergeResult.getFilesWithConflict()))
      .in("branches", String.format("%s -> %s", source, target))
      .in(namespaceAndName)
      .build();
  }

  @Override
  public String getCode() {
    return CODE;
  }
}
