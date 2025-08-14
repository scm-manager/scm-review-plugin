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

import com.cloudogu.scm.review.PermissionCheck;
import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import com.cloudogu.scm.review.pullrequest.service.PullRequestService;
import com.github.legman.Subscribe;
import jakarta.inject.Inject;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import sonia.scm.HandlerEventType;
import sonia.scm.plugin.Extension;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryImportEvent;
import sonia.scm.repository.RepositoryManager;
import sonia.scm.repository.RepositoryPermissions;
import sonia.scm.search.Id;
import sonia.scm.search.Index;
import sonia.scm.search.IndexLog;
import sonia.scm.search.IndexLogStore;
import sonia.scm.search.IndexTask;
import sonia.scm.search.ReindexRepositoryEvent;
import sonia.scm.search.SearchEngine;
import sonia.scm.search.SerializableIndexTask;

import java.util.Optional;

@Extension
@SuppressWarnings("UnstableApiUsage")
public class CommentIndexer implements ServletContextListener {

  private final SearchEngine searchEngine;

  @Inject
  public CommentIndexer(SearchEngine searchEngine) {
    this.searchEngine = searchEngine;
  }

  @Subscribe
  public void handleEvent(CommentEvent event) {
    Comment comment = event.getItem();
    PullRequest pullRequest = event.getPullRequest();
    Repository repository = event.getRepository();
    if (event.getEventType() == HandlerEventType.CREATE || event.getEventType() == HandlerEventType.MODIFY) {
      updateIndexedComment(repository, pullRequest, IndexedComment.transform(pullRequest.getId(), comment));
    } else if (event.getEventType() == HandlerEventType.DELETE) {
      Comment deletedComment = event.getOldItem();
      searchEngine
        .forType(IndexedComment.class)
        .update(index -> index.delete().byId(createCommentId(deletedComment.getId(), pullRequest.getId(), repository.getId())));
    }
  }

  @Subscribe
  public void handleEvent(ReplyEvent event) {
    Reply comment = event.getItem();
    PullRequest pullRequest = event.getPullRequest();
    Repository repository = event.getRepository();
    if (event.getEventType() == HandlerEventType.CREATE || event.getEventType() == HandlerEventType.MODIFY) {
      updateIndexedComment(repository, pullRequest, IndexedComment.transform(pullRequest.getId(), comment));
    } else if (event.getEventType() == HandlerEventType.DELETE) {
      Reply deletedReply = event.getOldItem();
      searchEngine
        .forType(IndexedComment.class)
        .update(index -> index.delete().byId(createCommentId(deletedReply.getId(), pullRequest.getId(), repository.getId())));
    }
  }

  @Subscribe
  public void handleEvent(RepositoryImportEvent event) {
    if (!event.isFailed()) {
      searchEngine.forType(IndexedComment.class).update(new IndexRepositoryTask(event.getItem()));
    }
  }

  @Subscribe
  public void handleEvent(ReindexRepositoryEvent event) {
    searchEngine.forType(IndexedComment.class).update(new ReindexRepositoryTask(event.getRepository()));
  }

  private void updateIndexedComment(Repository repository, PullRequest pullRequest, IndexedComment comment) {
    searchEngine.forType(IndexedComment.class).update(index -> storeComment(index, repository, pullRequest, comment));
  }

  private static Id<IndexedComment> createCommentId(String commentId, String pullRequestId, String repositoryId) {
    return Id.of(IndexedComment.class, commentId).and(PullRequest.class, pullRequestId).and(Repository.class, repositoryId);
  }

  @Override
  public void contextInitialized(ServletContextEvent servletContextEvent) {
    searchEngine.forType(IndexedComment.class).update(ReindexAll.class);
  }

  @Override
  public void contextDestroyed(ServletContextEvent servletContextEvent) {
    // Nothing to do
  }

  private static void storeComment(Index<IndexedComment> index, Repository repository, PullRequest pullRequest, IndexedComment comment) {
    index.store(
      createCommentId(comment.getId(), pullRequest.getId(), repository.getId()),
      RepositoryPermissions.custom(PermissionCheck.READ_PULL_REQUEST, repository).asShiroString(),
      comment
    );
  }

  private static void indexRepository(PullRequestService pullRequestService, CommentService commentService, Index<IndexedComment> index, Repository repository) {
    if (pullRequestService.supportsPullRequests(repository)) {
      for (PullRequest pr : pullRequestService.getAll(repository.getNamespace(), repository.getName())) {
        for (Comment comment : commentService.getAll(repository.getNamespace(), repository.getName(), pr.getId())) {
          storeComment(index, repository, pr, IndexedComment.transform(pr.getId(), comment));
        }
      }
    }
  }

  static final class ReindexAll implements IndexTask<IndexedComment> {

    private final RepositoryManager repositoryManager;
    private final IndexLogStore logStore;
    private final PullRequestService pullRequestService;
    private final CommentService commentService;

    @Inject
    ReindexAll(RepositoryManager repositoryManager, IndexLogStore logStore, PullRequestService pullRequestService, CommentService commentService) {
      this.repositoryManager = repositoryManager;
      this.logStore = logStore;
      this.pullRequestService = pullRequestService;
      this.commentService = commentService;
    }

    @Override
    public void update(Index<IndexedComment> index) {
      Optional<IndexLog> indexLog = logStore.defaultIndex().get(IndexedComment.class);
      if (!indexLog.isPresent() || indexLog.get().getVersion() != IndexedComment.VERSION) {
        reindexAll(index);
      }
    }

    @Override
    public void afterUpdate() {
      logStore.defaultIndex().log(IndexedComment.class, IndexedComment.VERSION);
    }

    private void reindexAll(Index<IndexedComment> index) {
      index.delete().all();
      for (Repository repo : repositoryManager.getAll()) {
        indexRepository(pullRequestService, commentService, index, repo);
      }
    }
  }

  static final class IndexRepositoryTask implements SerializableIndexTask<IndexedComment> {

    private transient PullRequestService pullRequestService;
    private transient CommentService commentService;

    private final Repository repository;

    IndexRepositoryTask(Repository repository) {
      this.repository = repository;
    }

    @Override
    public void update(Index<IndexedComment> index) {
      indexRepository(pullRequestService, commentService, index, repository);
    }

    @Inject
    public void setPullRequestService(PullRequestService pullRequestService) {
      this.pullRequestService = pullRequestService;
    }

    @Inject
    public void setCommentService(CommentService commentService) {
      this.commentService = commentService;
    }
  }

  static final class ReindexRepositoryTask implements SerializableIndexTask<IndexedComment> {

    private transient PullRequestService pullRequestService;
    private transient CommentService commentService;

    private final Repository repository;

    ReindexRepositoryTask(Repository repository) {
      this.repository = repository;
    }

    @Override
    public void update(Index<IndexedComment> index) {
      index.delete().by(Repository.class, repository).execute();
      indexRepository(pullRequestService, commentService, index, repository);
    }

    @Inject
    public void setPullRequestService(PullRequestService pullRequestService) {
      this.pullRequestService = pullRequestService;
    }

    @Inject
    public void setCommentService(CommentService commentService) {
      this.commentService = commentService;
    }
  }
}
