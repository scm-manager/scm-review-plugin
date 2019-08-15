package com.cloudogu.scm.review.comment.service;

import lombok.AllArgsConstructor;
import sonia.scm.repository.api.DiffLine;

import java.util.List;

@AllArgsConstructor
public class InlineContext {

  private List<DiffLine> changes;

  public List<DiffLine> getChanges() {
    return changes;
  }
}
