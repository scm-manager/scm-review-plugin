package com.cloudogu.scm.review.service;

import sonia.scm.repository.Repository;
import sonia.scm.BadRequestException;

import static sonia.scm.ContextEntry.ContextBuilder.entity;


public class StatusChangeNotAllowedException extends BadRequestException {
  public StatusChangeNotAllowedException(Repository repository, PullRequest pullRequest) {
    super(entity(PullRequest.class, pullRequest.getId()).in(repository).build(), "illegal status change");
  }

  @Override
  public String getCode() {
    return "BcRBqXYze1";
  }
}
