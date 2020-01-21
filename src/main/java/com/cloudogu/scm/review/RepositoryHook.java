package com.cloudogu.scm.review;

import com.cloudogu.scm.review.config.service.ConfigService;
import com.github.legman.Subscribe;
import lombok.extern.slf4j.Slf4j;
import sonia.scm.EagerSingleton;
import sonia.scm.plugin.Extension;
import sonia.scm.repository.Changeset;
import sonia.scm.repository.PreReceiveRepositoryHookEvent;
import sonia.scm.repository.Repository;
import sonia.scm.repository.api.HookBranchProvider;
import sonia.scm.repository.api.HookContext;
import sonia.scm.repository.api.HookFeature;

import javax.inject.Inject;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

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
      mergeRunning.set(null);
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
    List<String> branches = getBranches(context, repository);
    for (String branch : branches) {
      if (service.isBranchProtected(repository, branch)) {
        throw new BranchOnlyWritableByMergeException(repository, branch);
      }
    }
  }

  private List<String> getBranches(HookContext eventContext, Repository repository) {
    HookBranchProvider branchProvider = eventContext.getBranchProvider();
    LinkedList<String> branches = new LinkedList<>();
    branches.addAll(branchProvider.getCreatedOrModified());
    branches.addAll(branchProvider.getDeletedOrClosed());
    return branches;
  }
}
