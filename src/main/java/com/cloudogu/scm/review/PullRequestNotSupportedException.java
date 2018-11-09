package com.cloudogu.scm.review;

import sonia.scm.ExceptionWithContext;
import sonia.scm.repository.Repository;

import static sonia.scm.ContextEntry.ContextBuilder.entity;

public class PullRequestNotSupportedException extends ExceptionWithContext {

  private static final String CODE = "8xR8t7eBY1";
  private static final String MESSAGE_TEMPLATE = "merge is not supported by repositories of type %s, which is required for pull requests";

  public PullRequestNotSupportedException(Repository repository) {
    super(entity(repository).build(), String.format(MESSAGE_TEMPLATE, repository.getType()));
  }

  @Override
  public String getCode() {
    return CODE;
  }
}
