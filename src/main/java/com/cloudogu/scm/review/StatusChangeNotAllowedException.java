package com.cloudogu.scm.review;

import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import sonia.scm.BadRequestException;
import sonia.scm.repository.Repository;

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
