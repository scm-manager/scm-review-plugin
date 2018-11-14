package com.cloudogu.scm.review;

import sonia.scm.repository.Branch;
import sonia.scm.repository.InternalRepositoryException;
import sonia.scm.repository.Repository;
import sonia.scm.repository.api.BranchesCommandBuilder;
import sonia.scm.repository.api.RepositoryServiceFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.util.List;

import static sonia.scm.ContextEntry.ContextBuilder.entity;
import static sonia.scm.NotFoundException.notFound;

public class BranchResolver {
  private final RepositoryServiceFactory repositoryServiceFactory;

  @Inject
  public BranchResolver(RepositoryServiceFactory repositoryServiceFactory) {
    this.repositoryServiceFactory = repositoryServiceFactory;
  }

  public Branch resolve(Repository repository, String branchName) {
    try {
      BranchesCommandBuilder branchesCommand = repositoryServiceFactory.create(repository).getBranchesCommand();
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
}
