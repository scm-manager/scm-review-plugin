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
package com.cloudogu.scm.review.comment.service;

import com.cloudogu.scm.review.PermissionCheck;
import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import com.cloudogu.scm.review.pullrequest.service.PullRequestService;
import com.github.legman.Subscribe;
import sonia.scm.HandlerEventType;
import sonia.scm.plugin.Extension;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryManager;
import sonia.scm.repository.RepositoryPermissions;
import sonia.scm.search.Id;
import sonia.scm.search.Index;
import sonia.scm.search.IndexLog;
import sonia.scm.search.IndexLogStore;
import sonia.scm.search.IndexTask;
import sonia.scm.search.SearchEngine;

import javax.inject.Inject;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
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
      handleEvent(repository, pullRequest, IndexedComment.transform(pullRequest.getId(), comment));
    } else if (event.getEventType() == HandlerEventType.DELETE) {
      BasicComment deletedComment = event.getOldItem();
      searchEngine.forType(IndexedComment.class).update(index -> index.delete().byId(createCommentId(deletedComment.getId(), pullRequest.getId(), repository.getId())));
    }
  }

  @Subscribe
  public void handleEvent(ReplyEvent event) {
    Reply comment = event.getItem();
    PullRequest pullRequest = event.getPullRequest();
    Repository repository = event.getRepository();
    handleEvent(repository, pullRequest, IndexedComment.transform(pullRequest.getId(), comment));
  }

  private void handleEvent(Repository repository, PullRequest pullRequest, IndexedComment comment) {
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
      Optional<IndexLog> indexLog = logStore.defaultIndex().get(PullRequest.class);
      if (!indexLog.isPresent() || indexLog.get().getVersion() != IndexedComment.VERSION) {
        reindexAll(index);
      }
    }

    @Override
    public void afterUpdate() {
      logStore.defaultIndex().log(PullRequest.class, IndexedComment.VERSION);
    }

    private void reindexAll(Index<IndexedComment> index) {
      index.delete().all();
      for (Repository repo : repositoryManager.getAll()) {
        reindexRepository(index, repo);
      }
    }

    private void reindexRepository(Index<IndexedComment> index, Repository repository) {
      if (pullRequestService.supportsPullRequests(repository)) {
        for (PullRequest pr : pullRequestService.getAll(repository.getNamespace(), repository.getName())) {
          for (Comment comment : commentService.getAll(repository.getNamespace(), repository.getName(), pr.getId())) {
            storeComment(index, repository, pr, IndexedComment.transform(pr.getId(), comment));
          }
        }
      }
    }
  }
}
