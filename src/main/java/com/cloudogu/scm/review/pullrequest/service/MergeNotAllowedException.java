/*
 * Copyright (c) 2020 - present Cloudogu GmbH
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Affero General Public License as published by the Free
 * Software Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see https://www.gnu.org/licenses/.
 */

package com.cloudogu.scm.review.pullrequest.service;

import sonia.scm.ContextEntry;
import sonia.scm.ExceptionWithContext;
import sonia.scm.repository.Repository;

import java.util.Collection;
import java.util.stream.Collectors;

import static java.util.Collections.unmodifiableCollection;

public class MergeNotAllowedException extends ExceptionWithContext {

  private final Collection<MergeObstacle> obstacles;

  public MergeNotAllowedException(Repository repository, PullRequest pullRequest, Collection<MergeObstacle> obstacles) {
    super(ContextEntry.ContextBuilder.entity(PullRequest.class, pullRequest.getId()).in(repository).build(), buildMessage(obstacles));
    this.obstacles = obstacles;
  }

  public Collection<MergeObstacle> getObstacles() {
    return unmodifiableCollection(obstacles);
  }

  private static String buildMessage(Collection<MergeObstacle> obstacles) {
    return obstacles.stream().map(o -> o.getMessage()).collect(Collectors.joining(",\n", "The merge was prevented by other plugins:\n", ""));
  }

  @Override
  public String getCode() {
    return "9nRnT3cjk1";
  }
}
