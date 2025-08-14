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

import com.cloudogu.scm.review.PermissionCheck;
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
    searchEngine.forType(PullRequest.class).update(new ReindexRepositoryTask(event.getRepository()));
  }

  @Subscribe
  public void handleEvent(RepositoryImportEvent event) {
    if (!event.isFailed()) {
      searchEngine.forType(PullRequest.class).update(new IndexRepositoryTask(event.getItem()));
    }
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
        indexRepository(pullRequestService, index, repo);
      }
    }
  }

  static final class IndexRepositoryTask implements SerializableIndexTask<PullRequest> {

    private transient PullRequestService pullRequestService;

    private final Repository repository;

    IndexRepositoryTask(Repository repository) {
      this.repository = repository;
    }

    @Override
    public void update(Index<PullRequest> index) {
      indexRepository(pullRequestService, index, repository);
    }

    @Inject
    public void setPullRequestService(PullRequestService pullRequestService) {
      this.pullRequestService = pullRequestService;
    }
  }

  static final class ReindexRepositoryTask implements SerializableIndexTask<PullRequest> {

    private transient PullRequestService pullRequestService;

    private final Repository repository;

    ReindexRepositoryTask(Repository repository) {
      this.repository = repository;
    }

    @Override
    public void update(Index<PullRequest> index) {
      index.delete().by(Repository.class, repository).execute();
      indexRepository(pullRequestService, index, repository);
    }

    @Inject
    public void setPullRequestService(PullRequestService pullRequestService) {
      this.pullRequestService = pullRequestService;
    }
  }

  private static void indexRepository(PullRequestService pullRequestService, Index<PullRequest> index, Repository repository) {
    if (pullRequestService.supportsPullRequests(repository)) {
      for (PullRequest pr : pullRequestService.getAll(repository.getNamespace(), repository.getName())) {
        storePullRequest(index, repository, pr);
      }
    }
  }
}
