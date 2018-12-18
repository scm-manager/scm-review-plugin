package com.cloudogu.scm.review.pullrequest.service;

import com.cloudogu.scm.review.BranchResolver;
import com.cloudogu.scm.review.RepositoryResolver;
import com.cloudogu.scm.review.StatusChangeNotAllowedException;
import com.google.inject.Inject;
import sonia.scm.repository.NamespaceAndName;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryPermissions;

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
    return getStore(repository).add(pullRequest);
  }

  @Override
  public void update(String namespace, String name, String pullRequestId, String title, String description) {
    PullRequest pullRequest = get(namespace, name, pullRequestId);
    pullRequest.setTitle(title);
    pullRequest.setDescription(description);
    getStore(namespace, name).update(pullRequest);
  }

  @Override
  public PullRequest get(String namespace, String name, String id) {
    return getStore(namespace, name).get(id);
  }

  @Override
  public List<PullRequest> getAll(String namespace, String name) {
    return getStore(namespace, name).getAll();
  }

  @Override
  public Optional<PullRequest> get(Repository repository, String source, String target, PullRequestStatus status) {
    return getStore(repository).getAll()
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

  @Override
  public void reject(Repository repository, PullRequest pullRequest) {
    RepositoryPermissions.push(repository).check();
    if (pullRequest.getStatus() == PullRequestStatus.OPEN) {
      this.setStatus(repository, pullRequest, PullRequestStatus.REJECTED);
    } else {
      throw new StatusChangeNotAllowedException(repository, pullRequest);
    }
  }

  public void setStatus(Repository repository, PullRequest pullRequest, PullRequestStatus newStatus) {
    pullRequest.setStatus(newStatus);
    getStore(repository).update(pullRequest);

  }

  private PullRequestStore getStore(String namespace, String name) {
    return getStore(getRepository(namespace, name));
  }

  private PullRequestStore getStore(Repository repository) {
    return storeFactory.create(repository);
  }
}
