package com.cloudogu.scm.review.pullrequest.service;

import com.cloudogu.scm.review.BranchResolver;
import com.cloudogu.scm.review.CurrentUserResolver;
import com.cloudogu.scm.review.PermissionCheck;
import com.cloudogu.scm.review.RepositoryResolver;
import com.cloudogu.scm.review.StatusChangeNotAllowedException;
import com.google.inject.Inject;
import sonia.scm.HandlerEventType;
import sonia.scm.event.ScmEventBus;
import sonia.scm.repository.ChangesetPagingResult;
import sonia.scm.repository.InternalRepositoryException;
import sonia.scm.repository.NamespaceAndName;
import sonia.scm.repository.Repository;
import sonia.scm.repository.api.RepositoryService;
import sonia.scm.repository.api.RepositoryServiceFactory;
import sonia.scm.user.User;

import java.io.IOException;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Map;
import java.util.stream.Collectors;

public class DefaultPullRequestService implements PullRequestService {

  private final BranchResolver branchResolver;
  private final RepositoryResolver repositoryResolver;
  private final PullRequestStoreFactory storeFactory;
  private final ScmEventBus eventBus;
  private final RepositoryServiceFactory repositoryServiceFactory;

  @Inject
  public DefaultPullRequestService(RepositoryResolver repositoryResolver, BranchResolver branchResolver, PullRequestStoreFactory storeFactory, ScmEventBus eventBus, RepositoryServiceFactory repositoryServiceFactory) {
    this.repositoryResolver = repositoryResolver;
    this.branchResolver = branchResolver;
    this.storeFactory = storeFactory;
    this.eventBus = eventBus;
    this.repositoryServiceFactory = repositoryServiceFactory;
  }

  @Override
  public String add(Repository repository, PullRequest pullRequest) throws NoDifferenceException {
    verifyNewChangesetsOnSource(repository, pullRequest.getSource(), pullRequest.getTarget());
    pullRequest.setCreationDate(Instant.now());
    pullRequest.setLastModified(null);
    computeSubscriberForNewPullRequest(pullRequest);
    eventBus.post(new PullRequestEvent(repository, pullRequest, null, HandlerEventType.CREATE));
    return getStore(repository).add(pullRequest);
  }

  private void verifyNewChangesetsOnSource(Repository repository, String source, String target) throws NoDifferenceException {
    try (RepositoryService repositoryService = this.repositoryServiceFactory.create(repository)) {
      ChangesetPagingResult changesets = repositoryService.getLogCommand()
        .setStartChangeset(source)
        .setAncestorChangeset(target)
        .setPagingStart(0)
        .setPagingLimit(1)
        .getChangesets();

      if (changesets.getChangesets().isEmpty()) {
        throw new NoDifferenceException();
      }
    } catch (IOException e) {
      throw new InternalRepositoryException(repository, "error checking for diffs between branches", e);
    }
  }

  private void computeSubscriberForNewPullRequest(PullRequest pullRequest) {
    User user = CurrentUserResolver.getCurrentUser();
    Set<String> subscriber = new HashSet<>(pullRequest.getSubscriber());
    subscriber.addAll(pullRequest.getReviewer().keySet());
    subscriber.add(user.getId());
    pullRequest.setSubscriber(subscriber);
  }

  @Override
  public void update(Repository repository, String pullRequestId, PullRequest pullRequest) {
    PullRequestStore store = getStore(repository);
    PullRequest oldPullRequest = store.get(pullRequestId);
    pullRequest.setCreationDate(oldPullRequest.getCreationDate());
    pullRequest.setLastModified(Instant.now());
    computeSubscriberForChangedPullRequest(oldPullRequest, pullRequest);
    eventBus.post(new PullRequestEvent(repository, pullRequest, oldPullRequest, HandlerEventType.MODIFY));
    store.update(pullRequest);
  }

  private void computeSubscriberForChangedPullRequest(PullRequest oldPullRequest, PullRequest changedPullRequest) {
    Set<String> newSubscriber = new HashSet<>(oldPullRequest.getSubscriber());
    Set<String> addedReviewers = changedPullRequest.getReviewer().keySet().stream().filter(r -> !oldPullRequest.getReviewer().keySet().contains(r)).collect(Collectors.toSet());
    newSubscriber.addAll(addedReviewers);
    changedPullRequest.setSubscriber(newSubscriber);
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
      eventBus.post(new PullRequestRejectedEvent(repository, pullRequest));
    } else {
      throw new StatusChangeNotAllowedException(repository, pullRequest);
    }
  }

  @Override
  public boolean hasUserApproved(Repository repository, String pullRequestId, User user) {
    return getStore(repository).get(pullRequestId).getReviewer().entrySet()
      .stream()
      .filter(Map.Entry::getValue)
      .anyMatch(entry -> entry.getKey().equals(user.getId()));
  }

  @Override
  public void approve(Repository repository, String pullRequestId, User user) {
    PullRequestStore store = getStore(repository);
    PullRequest pullRequest = store.get(pullRequestId);
    pullRequest.addApprover(user.getId());
    store.update(pullRequest);
  }

  @Override
  public void disapprove(Repository repository, String pullRequestId, User user) {
    PullRequestStore store = getStore(repository);
    PullRequest pullRequest = store.get(pullRequestId);
    Set<String> approver = pullRequest.getReviewer().keySet();
    approver.stream()
      .filter(recipient -> user.getId().equals(recipient))
      .findFirst()
      .ifPresent(pullRequest::removeApprover);
    store.update(pullRequest);
  }

  @Override
  public boolean isUserSubscribed(Repository repository, String pullRequestId, User user) {
    return getStore(repository).get(pullRequestId).getSubscriber()
      .stream()
      .anyMatch(recipient -> user.getId().equals(recipient));
  }

  @Override
  public void subscribe(Repository repository, String pullRequestId, User user) {
    PullRequestStore store = getStore(repository);
    PullRequest pullRequest = store.get(pullRequestId);
    pullRequest.addSubscriber(user.getId());
    store.update(pullRequest);
  }

  @Override
  public void unsubscribe(Repository repository, String pullRequestId, User user) {
    PullRequestStore store = getStore(repository);
    PullRequest pullRequest = store.get(pullRequestId);
    Set<String> subscriber = pullRequest.getSubscriber();
    subscriber.stream()
      .filter(recipient -> user.getId().equals(recipient))
      .findFirst()
      .ifPresent(pullRequest::removeSubscriber);
    store.update(pullRequest);
  }

  @Override
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
