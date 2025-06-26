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

package com.cloudogu.scm.review.pullrequest.landingpage;

import com.cloudogu.scm.landingpage.mytasks.MyTask;
import com.cloudogu.scm.landingpage.mytasks.MyTaskProvider;
import com.cloudogu.scm.review.comment.service.Comment;
import com.cloudogu.scm.review.comment.service.CommentQueryFields;
import com.cloudogu.scm.review.comment.service.CommentStoreFactory;
import com.cloudogu.scm.review.comment.service.CommentType;
import com.cloudogu.scm.review.pullrequest.dto.PullRequestMapper;
import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import com.cloudogu.scm.review.pullrequest.service.PullRequestQueryFields;
import com.cloudogu.scm.review.pullrequest.service.PullRequestStatus;
import com.cloudogu.scm.review.pullrequest.service.PullRequestStoreFactory;
import jakarta.inject.Inject;
import org.apache.shiro.SecurityUtils;
import sonia.scm.plugin.Extension;
import sonia.scm.plugin.Requires;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryManager;
import sonia.scm.store.QueryableStore;

import java.util.Objects;
import java.util.stream.Collectors;

@Extension
@Requires("scm-landingpage-plugin")
public class MyOpenTasks implements MyTaskProvider {

  private final PullRequestMapper mapper;

  private final PullRequestStoreFactory pullRequestStoreFactory;
  private final CommentStoreFactory commentStoreFactory;
  private final RepositoryManager repositoryManager;

  @Inject
  public MyOpenTasks(PullRequestMapper mapper, PullRequestStoreFactory pullRequestStoreFactory, CommentStoreFactory commentStoreFactory, RepositoryManager repositoryManager) {
    this.mapper = mapper;
    this.pullRequestStoreFactory = pullRequestStoreFactory;
    this.commentStoreFactory = commentStoreFactory;
    this.repositoryManager = repositoryManager;
  }

  @Override
  public Iterable<MyTask> getTasks() {
    String subject = SecurityUtils.getSubject().getPrincipal().toString();
    try (QueryableStore<PullRequest> store = pullRequestStoreFactory.getOverall()) {
      return store
        .query(
          PullRequestQueryFields.AUTHOR.eq(subject),
          PullRequestQueryFields.STATUS.eq(PullRequestStatus.OPEN)
        ).withIds()
        .findAll()
        .stream()
        .filter(
          result ->
          {
            try (QueryableStore<Comment> commentQueryableStore = commentStoreFactory.get(
              result.getParentId(Repository.class).get(),
              result.getId()
            )) {
              return commentQueryableStore
                .query(CommentQueryFields.TYPE.eq(CommentType.TASK_TODO))
                .count() > 0;
            }
          }
        )
        .map(
          result -> {
            Repository repository = repositoryManager.get(result.getParentId(Repository.class).get());
            if (repository == null) {
              return null;
            }
            PullRequest pullRequest = result.getEntity();
            return new MyPullRequestTodos(repository, pullRequest, mapper);
          }
        ).filter(Objects::nonNull)
        .collect(Collectors.toList());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
