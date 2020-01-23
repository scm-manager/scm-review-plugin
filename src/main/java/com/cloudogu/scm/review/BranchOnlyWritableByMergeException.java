package com.cloudogu.scm.review;

import sonia.scm.ExceptionWithContext;
import sonia.scm.repository.Repository;

import static sonia.scm.ContextEntry.ContextBuilder.entity;

public class BranchOnlyWritableByMergeException extends ExceptionWithContext {

  public static final String CODE = "FuRoChFWt1";

  public BranchOnlyWritableByMergeException(Repository repository, String branch) {
    super(entity("Branch", branch).in(repository).build(), "The branch " + branch + " is only writable using pull requests");
  }

  @Override
  public String getCode() {
    return CODE;
  }
}
