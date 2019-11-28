package com.cloudogu.scm.review.pullrequest.service;

import sonia.scm.BadRequestException;
import sonia.scm.repository.Repository;

import static sonia.scm.ContextEntry.ContextBuilder.entity;

/**
 * Indicates that a pull request should be created, that has no diff.
 */
public class NoDifferenceException extends BadRequestException {

  public static final String CODE = "CVRj80D6z1";

  public NoDifferenceException(Repository repository) {
    super(entity(repository).build(), "No differences found between selected branches");
  }

  @Override
  public String getCode() {
    return CODE;
  }
}
