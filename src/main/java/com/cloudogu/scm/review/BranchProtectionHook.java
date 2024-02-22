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
import sonia.scm.repository.Changeset;
import sonia.scm.repository.PreReceiveRepositoryHookEvent;
import sonia.scm.repository.Repository;
import sonia.scm.repository.api.HookContext;
import sonia.scm.repository.api.HookFeature;
import sonia.scm.repository.api.RepositoryService;
import sonia.scm.repository.api.RepositoryServiceFactory;

import jakarta.inject.Inject;
import java.io.IOException;

@Slf4j
@Extension
@EagerSingleton
public class BranchProtectionHook {

  private final ConfigService service;
  private final InternalMergeSwitch internalMergeSwitch;
  private final RepositoryServiceFactory serviceFactory;

  @Inject
  public BranchProtectionHook(ConfigService service, InternalMergeSwitch internalMergeSwitch, RepositoryServiceFactory serviceFactory) {
    this.service = service;
    this.internalMergeSwitch = internalMergeSwitch;
    this.serviceFactory = serviceFactory;
  }

  @Subscribe(async = false)
  public void onEvent(PreReceiveRepositoryHookEvent event) {
    HookContext context = event.getContext();
    Repository repository = event.getRepository();

    if (ignoreHook(context, repository)) {
      return;
    }

    if (!service.isEnabled(repository)) {
      log.trace("branch protection is disabled.");
      return;
    }

    log.trace("received hook for repository {}", repository.getName());
    for (String branch : context.getBranchProvider().getCreatedOrModified()) {
      if (service.isBranchProtected(repository, branch)) {
        if (context.isFeatureSupported(HookFeature.MODIFICATIONS_PROVIDER)) {
          checkChangesUsingModifications(branch, context, repository);
        } else {
          log.warn("falling back to check changesets for branch {} in repository {}", branch, repository);
          checkChangesUsingChangesets(branch, context, repository);
        }
      }
    }
    for (
      String branch : context.getBranchProvider().getDeletedOrClosed()) {
      if (service.isBranchProtected(repository, branch)) {
        throw new BranchOnlyWritableByMergeException(repository, branch);
      }
    }
  }

  private void checkChangesUsingModifications(String branch, HookContext context, Repository repository) {
    context.getModificationsProvider().getModifications(branch).effectedPathsStream().forEach(modifiedPath -> {
      if (service.isBranchPathProtected(repository, branch, modifiedPath)) {
        throw new PathOnlyWritableByMergeException(repository, branch, modifiedPath);
      }
    });
  }

  private void checkChangesUsingChangesets(String branch, HookContext context, Repository repository) {
    context.getChangesetProvider().getChangesets().forEach(
      changeset -> {
        if (mayNotWriteBranchWithoutPr(repository, branch, changeset)) {
          throw new BranchOnlyWritableByMergeException(repository, branch);
        }
      }
    );
  }

  private boolean mayNotWriteBranchWithoutPr(Repository repository, String branch, Changeset changeset) {
    return changeset.getBranches().contains(branch)
      && !changeset.getParents().isEmpty()
      && pathIsProtected(repository, branch, changeset);
  }

  private boolean pathIsProtected(Repository repository, String branch, Changeset changeset) {
    try (RepositoryService repositoryService = serviceFactory.create(repository)) {
      return repositoryService.getModificationsCommand()
        .revision(changeset.getId())
        .getModifications()
        .getEffectedPaths()
        .stream()
        .anyMatch(path -> service.isBranchPathProtected(repository, branch, path));
    } catch (IOException e) {
      log.error("Could not detect whether the branch protection is ");
      return true;
    }
  }

  private boolean ignoreHook(HookContext context, Repository repository) {
    if (internalMergeSwitch.internalMergeRunning()) {
      return true;
    }
    if (context == null) {
      log.warn("there is no context in the received repository hook");
      return true;
    }
    if (!context.isFeatureSupported(HookFeature.BRANCH_PROVIDER)) {
      log.debug("The repository does not support branches. Skip check.");
      return true;
    }
    if (repository == null) {
      log.warn("there is no repository in the received repository hook");
      return true;
    }
    return false;
  }
}
