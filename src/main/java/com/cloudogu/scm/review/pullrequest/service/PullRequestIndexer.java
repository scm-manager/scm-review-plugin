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

import com.cloudogu.scm.review.PermissionCheck;
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
import sonia.scm.search.ReindexRepositoryEvent;
import sonia.scm.search.SearchEngine;
import sonia.scm.search.SerializableIndexTask;

import javax.inject.Inject;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.util.Optional;

@Extension
@SuppressWarnings("UnstableApiUsage")
public class PullRequestIndexer implements ServletContextListener {

  private final SearchEngine searchEngine;

  @Inject
  public PullRequestIndexer(SearchEngine searchEngine) {
    this.searchEngine = searchEngine;
  }

  @Subscribe
  public void handleEvent(PullRequestEvent event) {
    if (event.getEventType() == HandlerEventType.CREATE || event.getEventType() == HandlerEventType.MODIFY) {
      handleEvent(event.getRepository(), event.getPullRequest());
    }
  }

  @Subscribe
  public void handleEvent(PullRequestUpdatedEvent event) {
    handleEvent(event.getRepository(), event.getPullRequest());
  }

  @Subscribe
  public void handleEvent(PullRequestMergedEvent event) {
    handleEvent(event.getRepository(), event.getPullRequest());
  }

  @Subscribe
  public void handleEvent(PullRequestRejectedEvent event) {
    handleEvent(event.getRepository(), event.getPullRequest());
  }

  @Subscribe
  public void handleEvent(PullRequestEmergencyMergedEvent event) {
    handleEvent(event.getRepository(), event.getPullRequest());
  }

  @Subscribe
  public void handleEvent(ReindexRepositoryEvent event) {
    searchEngine.forType(PullRequest.class).update(new PullRequestIndexer.ReindexRepository(event.getRepository()));
  }

  private void handleEvent(Repository repository, PullRequest pullRequest) {
    searchEngine.forType(PullRequest.class).update(index -> storePullRequest(index, repository, pullRequest));
  }

  @Override
  public void contextInitialized(ServletContextEvent servletContextEvent) {
    searchEngine.forType(PullRequest.class).update(ReindexAll.class);
  }

  @Override
  public void contextDestroyed(ServletContextEvent servletContextEvent) {
   // Nothing to do
  }

  private static void storePullRequest(Index<PullRequest> index, Repository repository, PullRequest pullRequest) {
    index.store(
      Id.of(PullRequest.class, pullRequest.getId()).and(Repository.class, repository.getId()),
      RepositoryPermissions.custom(PermissionCheck.READ_PULL_REQUEST, repository).asShiroString(),
      pullRequest
    );
  }

 static final class ReindexAll implements IndexTask<PullRequest> {

    private final RepositoryManager repositoryManager;
    private final IndexLogStore logStore;
    private final PullRequestService pullRequestService;

    @Inject
    ReindexAll(RepositoryManager repositoryManager, IndexLogStore logStore, PullRequestService pullRequestService) {
      this.repositoryManager = repositoryManager;
      this.logStore = logStore;
      this.pullRequestService = pullRequestService;
    }

    @Override
    public void update(Index<PullRequest> index) {
      Optional<IndexLog> indexLog = logStore.defaultIndex().get(PullRequest.class);
      if (!indexLog.isPresent() || indexLog.get().getVersion() != PullRequest.VERSION) {
        reindexAll(index);
      }
    }

    @Override
    public void afterUpdate() {
      logStore.defaultIndex().log(PullRequest.class, PullRequest.VERSION);
    }

    private void reindexAll(Index<PullRequest> index) {
      index.delete().all();
      for (Repository repo : repositoryManager.getAll()) {
        reindexRepository(pullRequestService, index, repo);
      }
    }
  }

 static final class ReindexRepository implements SerializableIndexTask<PullRequest> {

   private transient PullRequestService pullRequestService;

   private final Repository repository;

   ReindexRepository(Repository repository) {
     this.repository = repository;
   }

    @Override
    public void update(Index<PullRequest> index) {
      index.delete().by(repository);
      reindexRepository(pullRequestService, index, repository);
    }

    @Inject
   public void setPullRequestService(PullRequestService pullRequestService) {
     this.pullRequestService = pullRequestService;
   }
 }

  private static void reindexRepository(PullRequestService pullRequestService, Index<PullRequest> index, Repository repository) {
    if (pullRequestService.supportsPullRequests(repository)) {
      for (PullRequest pr : pullRequestService.getAll(repository.getNamespace(), repository.getName())) {
        storePullRequest(index, repository, pr);
      }
    }
  }
}
