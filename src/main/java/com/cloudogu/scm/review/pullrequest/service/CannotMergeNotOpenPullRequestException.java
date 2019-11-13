package com.cloudogu.scm.review.pullrequest.service;

import sonia.scm.ExceptionWithContext;
import sonia.scm.repository.Repository;

import static sonia.scm.ContextEntry.ContextBuilder.entity;

public class CannotMergeNotOpenPullRequestException extends ExceptionWithContext {

  public static final String CODE = "FTRhcI0To1";

  public CannotMergeNotOpenPullRequestException(Repository repository, PullRequest pullRequest) {
    super(entity(PullRequest.class, pullRequest.getId()).in(repository).build(), "cannot merge pull requests that are not open");
  }

  @Override
  public String getCode() {
    return CODE;
  }
}
