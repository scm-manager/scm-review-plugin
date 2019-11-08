package com.cloudogu.scm.review.pullrequest.api;

import sonia.scm.ContextEntry;
import sonia.scm.ExceptionWithContext;
import sonia.scm.repository.NamespaceAndName;
import sonia.scm.repository.api.MergeCommandResult;

import java.util.List;

import static sonia.scm.ContextEntry.ContextBuilder.entity;

class MergeConflictException extends ExceptionWithContext {
  private static final String CODE = "DTRhFJFgU1";

  MergeConflictException(NamespaceAndName namespaceAndName, String source, String target, MergeCommandResult mergeResult) {
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
