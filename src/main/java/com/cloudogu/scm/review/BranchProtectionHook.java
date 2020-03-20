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
public class BranchProtectionHook {

  private ConfigService service;

  private final ThreadLocal<Object> mergeRunning = new InheritableThreadLocal<>();

  @Inject
  public BranchProtectionHook(ConfigService service) {
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
