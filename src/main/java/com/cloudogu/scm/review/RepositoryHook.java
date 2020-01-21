package com.cloudogu.scm.review;

import com.cloudogu.scm.review.config.service.ConfigService;
import com.github.legman.Subscribe;
import lombok.extern.slf4j.Slf4j;
import sonia.scm.EagerSingleton;
import sonia.scm.plugin.Extension;
import sonia.scm.repository.PreReceiveRepositoryHookEvent;
import sonia.scm.repository.Repository;
import sonia.scm.repository.api.HookContext;
import sonia.scm.repository.api.HookFeature;

import javax.inject.Inject;

@Slf4j
@Extension
@EagerSingleton
public class RepositoryHook {

  private ConfigService service;

  private final ThreadLocal<Object> mergeRunning = new InheritableThreadLocal<>();

  @Inject
  public RepositoryHook(ConfigService service) {
    this.service = service;
  }

  public void runPrivileged(Runnable r) {
    mergeRunning.set(new Object());
    try {
      r.run();
    } finally {
      mergeRunning.remove();
    }
  }

  @Subscribe(async = false)
  public void onEvent(PreReceiveRepositoryHookEvent event) {
    if (mergeRunning.get() != null) {
      return;
    }
    HookContext context = event.getContext();
    if (context == null) {
      log.warn("there is no context in the received repository hook");
      return;
    }
    if (!context.isFeatureSupported(HookFeature.BRANCH_PROVIDER)) {
      log.debug("The repository does not support branches. Skip check.");
      return;
    }

    Repository repository = event.getRepository();
    if (repository == null) {
      log.warn("there is no repository in the received repository hook");
      return;
    }

    if (!service.isEnabled(repository)){
      log.trace("branch protection is disabled.");
      return;
    }

    log.trace("received hook for repository {}", repository.getName());
    for (String branch : context.getBranchProvider().getCreatedOrModified()) {
      if (service.isBranchProtected(repository, branch)) {
        context.getChangesetProvider().getChangesets().forEach(
          changeset -> {
            if (changeset.getBranches().contains(branch) && !changeset.getParents().isEmpty()) {
              throw new BranchOnlyWritableByMergeException(repository, branch);
            }
          }
        );
      }
    }
    for (String branch : context.getBranchProvider().getDeletedOrClosed()) {
      if (service.isBranchProtected(repository, branch)) {
        throw new BranchOnlyWritableByMergeException(repository, branch);
      }
    }
  }
}
