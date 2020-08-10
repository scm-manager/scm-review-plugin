/*
 * MIT License
 *
 * Copyright (c) 2020-present Cloudogu GmbH and Contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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
