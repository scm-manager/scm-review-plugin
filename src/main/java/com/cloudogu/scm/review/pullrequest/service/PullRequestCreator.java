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

import com.cloudogu.scm.review.CurrentUserResolver;
import com.cloudogu.scm.review.PermissionCheck;
import com.cloudogu.scm.review.comment.service.Comment;
import com.cloudogu.scm.review.comment.service.CommentService;
import com.cloudogu.scm.review.comment.service.CommentType;
import jakarta.inject.Inject;
import sonia.scm.repository.Repository;
import sonia.scm.user.User;

import java.util.List;

import static sonia.scm.AlreadyExistsException.alreadyExists;
import static sonia.scm.ContextEntry.ContextBuilder.entity;
import static sonia.scm.ScmConstraintViolationException.Builder.doThrow;

public class PullRequestCreator {

  private final PullRequestService service;
  private final CommentService commentService;

  @Inject
  public PullRequestCreator(PullRequestService service, CommentService commentService) {
    this.service = service;
    this.commentService = commentService;
  }

  public String openNewPullRequest(String namespace, String name, PullRequest pullRequest, List<String> initialTasks) {
    Repository repository = service.getRepository(namespace, name);
    PermissionCheck.checkCreate(repository);

    if (pullRequest.getStatus() == null) {
      pullRequest.setStatus(PullRequestStatus.OPEN);
    } else {
      doThrow().violation("illegal status", "pullRequest", "status")
        .when(pullRequest.getStatus().isClosed());
    }

    String source = pullRequest.getSource();
    String target = pullRequest.getTarget();

    service.getInProgress(repository, source, target)
      .ifPresent(existing -> {
        throw alreadyExists(entity("Pull Request", existing.getId()).in(repository));
      });

    service.checkBranch(repository, source);
    service.checkBranch(repository, target);

    verifyBranchesDiffer(source, target);

    User user = CurrentUserResolver.getCurrentUser();

    pullRequest.setAuthor(user.getId());

    String id = service.add(repository, pullRequest);

    createInitialTasks(namespace, name, id, initialTasks);
    return id;
  }

  private void createInitialTasks(String namespace, String name, String id, List<String> initialTasks) {
    if (initialTasks == null) {
      return;
    }
    for (String task : initialTasks) {
      Comment comment = new Comment();
      comment.setComment(task);
      comment.setType(CommentType.TASK_TODO);
      commentService.add(namespace, name, id, comment);
    }
  }

  private void verifyBranchesDiffer(String source, String target) {
    doThrow()
      .violation("source branch and target branch must differ", "pullRequest", "source")
      .violation("source branch and target branch must differ", "pullRequest", "target")
      .when(source.equals(target));
  }
}
