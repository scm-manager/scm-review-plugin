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

package com.cloudogu.scm.review.comment.service;

import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import sonia.scm.repository.Repository;

import jakarta.inject.Inject;
import java.util.stream.Stream;

public class CommentCollector {

  private final CommentService commentService;

  @Inject
  public CommentCollector(CommentService commentService) {
    this.commentService = commentService;
  }

  public Stream<Comment> collectNonOutdated(Repository repository, PullRequest pullRequest) {
    return commentService.getAll(repository.getNamespace(), repository.getName(), pullRequest.getId())
      .stream()
      .filter(c -> !c.isOutdated());
  }
}
