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

import sonia.scm.repository.Branch;
import sonia.scm.repository.InternalRepositoryException;
import sonia.scm.repository.Repository;
import sonia.scm.repository.api.BranchesCommandBuilder;
import sonia.scm.repository.api.RepositoryService;
import sonia.scm.repository.api.RepositoryServiceFactory;

import jakarta.inject.Inject;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static sonia.scm.ContextEntry.ContextBuilder.entity;
import static sonia.scm.NotFoundException.notFound;

public class BranchResolver {
  private final RepositoryServiceFactory repositoryServiceFactory;

  @Inject
  public BranchResolver(RepositoryServiceFactory repositoryServiceFactory) {
    this.repositoryServiceFactory = repositoryServiceFactory;
  }

  public Branch resolve(Repository repository, String branchName) {
    try (RepositoryService repositoryService = repositoryServiceFactory.create(repository)) {
      BranchesCommandBuilder branchesCommand = repositoryService.getBranchesCommand();
      List<Branch> allBranches = branchesCommand.getBranches().getBranches();
      return allBranches
        .stream()
        .filter(b -> b.getName().equals(branchName))
        .findFirst()
        .orElseThrow(() -> notFound(entity("branch", branchName).in(repository)));
    } catch (IOException e) {
      throw new InternalRepositoryException(repository, "could not read branches", e);
    }
  }

  public Optional<Branch> find(Repository repository, String branchName) {
    try (RepositoryService repositoryService = repositoryServiceFactory.create(repository)) {
      BranchesCommandBuilder branchesCommand = repositoryService.getBranchesCommand();
      List<Branch> allBranches = branchesCommand.getBranches().getBranches();
      return allBranches
        .stream()
        .filter(b -> b.getName().equals(branchName))
        .findFirst();
    } catch (IOException e) {
      throw new InternalRepositoryException(repository, "could not read branches", e);
    }
  }
}
