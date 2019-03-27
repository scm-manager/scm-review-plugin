package com.cloudogu.scm.review.pullrequest.service;

import com.cloudogu.scm.review.BranchResolver;
import com.cloudogu.scm.review.PermissionCheck;
import com.cloudogu.scm.review.RepositoryResolver;
import com.cloudogu.scm.review.StatusChangeNotAllowedException;
import com.google.inject.Inject;
import sonia.scm.HandlerEventType;
import sonia.scm.event.ScmEventBus;
import sonia.scm.repository.NamespaceAndName;
import sonia.scm.repository.Repository;
import sonia.scm.user.User;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public class DefaultPullRequestService implements PullRequestService {

  private final BranchResolver branchResolver;
  private final RepositoryResolver repositoryResolver;
  private final PullRequestStoreFactory storeFactory;
  private final ScmEventBus eventBus;

  @Inject
  public DefaultPullRequestService(RepositoryResolver repositoryResolver, BranchResolver branchResolver, PullRequestStoreFactory storeFactory, ScmEventBus eventBus) {
    this.repositoryResolver = repositoryResolver;
    this.branchResolver = branchResolver;
    this.storeFactory = storeFactory;
    this.eventBus = eventBus;
  }

  @Override
  public String add(Repository repository, PullRequest pullRequest) {
    eventBus.post(new PullRequestEvent(repository, pullRequest, null, HandlerEventType.CREATE));
    return getStore(repository).add(pullRequest);
  }

  @Override
  public void update(Repository repository, String pullRequestId, PullRequest pullRequest) {
    PullRequestStore store = getStore(repository);
    PullRequest oldPullRequest = store.get(pullRequestId);
    pullRequest.setSubscriber(oldPullRequest.getSubscriber());
    eventBus.post(new PullRequestEvent(repository, pullRequest, oldPullRequest, HandlerEventType.MODIFY));
    store.update(pullRequest);
  }

  @Override
  public PullRequest get(Repository repository, String id) {
    return getStore(repository).get(id);
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
    PermissionCheck.checkMerge(repository);
    if (pullRequest.getStatus() == PullRequestStatus.OPEN) {
      this.setStatus(repository, pullRequest, PullRequestStatus.REJECTED);
    } else {
      throw new StatusChangeNotAllowedException(repository, pullRequest);
    }
  }

  @Override
  public boolean isUserSubscribed(Repository repository, String pullRequestId, User user) {
    return getStore(repository).get(pullRequestId).getSubscriber()
      .stream()
      .anyMatch(recipient -> user.getId().equals(recipient.getName()));
  }

  @Override
  public void subscribe(Repository repository, String pullRequestId, User user) {
    PullRequestStore store = getStore(repository);
    PullRequest pullRequest = store.get(pullRequestId);
    pullRequest.getSubscriber().add(new Recipient(user.getId(), user.getMail()));
    store.update(pullRequest);

  }

  @Override
  public void unsubscribe(Repository repository, String pullRequestId, User user) {
    PullRequestStore store = getStore(repository);
    PullRequest pullRequest = store.get(pullRequestId);
    Set<Recipient> subscriber = pullRequest.getSubscriber();
    subscriber.stream()
      .filter(recipient -> user.getId().equals(recipient.getName()))
      .findFirst()
      .ifPresent(subscriber::remove);
    store.update(pullRequest);
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
