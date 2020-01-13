package com.cloudogu.scm.review.pullrequest.service;

import sonia.scm.ContextEntry;
import sonia.scm.ExceptionWithContext;
import sonia.scm.repository.Repository;

import java.util.Collection;
import java.util.stream.Collectors;

public class MergeNotAllowedException extends ExceptionWithContext {
  public MergeNotAllowedException(Repository repository, PullRequest pullRequest, Collection<MergeObstacle> obstacles) {
    super(ContextEntry.ContextBuilder.entity(PullRequest.class, pullRequest.getId()).in(repository).build(), buildMessage(obstacles));
  }

  private static String buildMessage(Collection<MergeObstacle> obstacles) {
    return obstacles.stream().map(o -> o.getMessage()).collect(Collectors.joining(",\n", "The merge was prevented by other plugins:\n", ""));
  }

  @Override
  public String getCode() {
    return "9nRnT3cjk1";
  }
}
