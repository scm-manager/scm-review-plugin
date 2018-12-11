package com.cloudogu.scm.review.pullrequest.service;

import com.cloudogu.scm.review.BranchResolver;
import com.cloudogu.scm.review.RepositoryResolver;
import com.google.inject.Inject;
import sonia.scm.repository.NamespaceAndName;
import sonia.scm.repository.Repository;

import java.util.List;
import java.util.Optional;

public class DefaultPullRequestService implements PullRequestService {

  private final BranchResolver branchResolver;
  private final RepositoryResolver repositoryResolver;
  private final PullRequestStoreFactory storeFactory;

  @Inject
  public DefaultPullRequestService(RepositoryResolver repositoryResolver, BranchResolver branchResolver, PullRequestStoreFactory storeFactory) {
    this.repositoryResolver = repositoryResolver;
    this.branchResolver = branchResolver;
    this.storeFactory = storeFactory;
  }


  @Override
  public String add(Repository repository, PullRequest pullRequest) {
    return storeFactory.create(repository).add(pullRequest);
  }

  @Override
  public PullRequest get(String namespace, String name, String id) {
    return storeFactory.create(getRepository(namespace, name)).get(id);
  }

  @Override
  public List<PullRequest> getAll(String namespace, String name) {
    return storeFactory.create(getRepository(namespace, name)).getAll();
  }

  @Override
  public Optional<PullRequest> get(Repository repository, String source, String target, PullRequestStatus status) {
    return storeFactory.create(repository).getAll()
      .stream()
      .filter(pullRequest ->
        pullRequest.getStatus().equals(status) &&
          pullRequest.getSource().equals(source) &&
          pullRequest.getTarget().equals(target))
      .findFirst();
  }

  @Override
  public void checkBranch(Repository repository, String branch) {
    branchResolver.resolve(repository, branch);
  }

  @Override
  public Repository getRepository(String namespace, String name) {
    return repositoryResolver.resolve(new NamespaceAndName(namespace, name));
  }

  void setStatus(Repository repository, PullRequest pullRequest, PullRequestStatus newStatus) {
    pullRequest.setStatus(newStatus);
    storeFactory.create(repository).update(pullRequest);
  }
}
