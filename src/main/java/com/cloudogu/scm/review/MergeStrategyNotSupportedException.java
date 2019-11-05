package com.cloudogu.scm.review;

import sonia.scm.ExceptionWithContext;
import sonia.scm.repository.Repository;
import sonia.scm.repository.api.MergeStrategy;

import static sonia.scm.ContextEntry.ContextBuilder.entity;

public class MergeStrategyNotSupportedException extends ExceptionWithContext {

  private static final String CODE = "hHZH8HtXd2";

  public MergeStrategyNotSupportedException(Repository repository, MergeStrategy strategy) {
    super(entity(repository).build(), String.format("merge strategy %s is not supported", strategy));
  }

  @Override
  public String getCode() {
    return CODE;
  }
}
