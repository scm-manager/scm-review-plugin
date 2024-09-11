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
