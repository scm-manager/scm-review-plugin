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
import sonia.scm.search.SearchEngine;

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
      PullRequest pullRequest = event.getPullRequest();
      Repository repository = event.getRepository();
      searchEngine.forType(PullRequest.class).update(index -> storePullRequest(index, repository, pullRequest));
    }
  }

  @Override
  public void contextInitialized(ServletContextEvent servletContextEvent) {
    searchEngine.forType(PullRequest.class).update(ReindexAll.class);
  }

  @Override
  public void contextDestroyed(ServletContextEvent servletContextEvent) {

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
        reindexRepository(index, repo);
      }
    }

    private void reindexRepository(Index<PullRequest> index, Repository repository) {
      for (PullRequest pr : pullRequestService.getAll(repository.getNamespace(), repository.getName())) {
        storePullRequest(index, repository, pr);
      }
    }
  }
}
