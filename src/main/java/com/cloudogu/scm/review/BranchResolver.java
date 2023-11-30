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

import sonia.scm.repository.Branch;
import sonia.scm.repository.InternalRepositoryException;
import sonia.scm.repository.Repository;
import sonia.scm.repository.api.BranchesCommandBuilder;
import sonia.scm.repository.api.RepositoryService;
import sonia.scm.repository.api.RepositoryServiceFactory;

import javax.inject.Inject;
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
