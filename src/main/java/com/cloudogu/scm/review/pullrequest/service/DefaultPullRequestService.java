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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.cloudogu.scm.review.pullrequest.service.PullRequestApprovalEvent.ApprovalCause.APPROVAL_REMOVED;
import static com.cloudogu.scm.review.pullrequest.service.PullRequestApprovalEvent.ApprovalCause.APPROVED;
import static com.cloudogu.scm.review.pullrequest.service.PullRequestStatus.MERGED;
import static com.cloudogu.scm.review.pullrequest.service.PullRequestStatus.OPEN;
import static com.cloudogu.scm.review.pullrequest.service.PullRequestStatus.REJECTED;

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
  public String add(Repository repository, PullRequest pullRequest) {
    verifyNewChangesetsOnSource(repository, pullRequest.getSource(), pullRequest.getTarget());
    pullRequest.setCreationDate(Instant.now());
    pullRequest.setLastModified(null);
    computeSubscriberForNewPullRequest(pullRequest);
    String id = getStore(repository).add(pullRequest);
    eventBus.post(new PullRequestEvent(repository, pullRequest, null, HandlerEventType.CREATE));
    return id;
  }

  private void verifyNewChangesetsOnSource(Repository repository, String source, String target) {
    try (RepositoryService repositoryService = this.repositoryServiceFactory.create(repository)) {
      ChangesetPagingResult changesets = repositoryService.getLogCommand()
        .setStartChangeset(source)
        .setAncestorChangeset(target)
        .setPagingStart(0)
        .setPagingLimit(1)
        .getChangesets();

      if (changesets.getChangesets().isEmpty()) {
        throw new NoDifferenceException(repository);
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
    Set<String> addedReviewers = computeAddedReviewersForChangedPullRequest(oldPullRequest, pullRequest);
    Set<String> removedReviewers = computeRemovedReviewersForChangedPullRequest(oldPullRequest, pullRequest);

    Set<String> newSubscriber = new HashSet<>(oldPullRequest.getSubscriber());
    newSubscriber.addAll(addedReviewers);

    Map<String, Boolean> newReviewers = new HashMap<>(oldPullRequest.getReviewer());
    addedReviewers.forEach(reviewer -> newReviewers.putIfAbsent(reviewer, false));
    removedReviewers.forEach(newReviewers::remove);

    PullRequest newPullRequest = oldPullRequest.toBuilder()
      .targetRevision(pullRequest.getTargetRevision())
      .title(pullRequest.getTitle())
      .description(pullRequest.getDescription())
      .lastModified(Instant.now())
      .reviewer(newReviewers)
      .build();
    newPullRequest.setSubscriber(newSubscriber);
    store.update(newPullRequest);
    eventBus.post(new PullRequestEvent(repository, newPullRequest, oldPullRequest, HandlerEventType.MODIFY));
  }

  private Set<String> computeAddedReviewersForChangedPullRequest(PullRequest oldPullRequest, PullRequest changedPullRequest) {
    return changedPullRequest
      .getReviewer()
      .keySet()
      .stream()
      .filter(r -> !oldPullRequest.getReviewer().containsKey(r))
      .collect(Collectors.toSet());
  }

  private Set<String> computeRemovedReviewersForChangedPullRequest(PullRequest oldPullRequest, PullRequest changedPullRequest) {
    return oldPullRequest
      .getReviewer()
      .keySet()
      .stream()
      .filter(r -> !changedPullRequest.getReviewer().containsKey(r))
      .collect(Collectors.toSet());
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
  public void reject(Repository repository, String pullRequestId, PullRequestRejectedEvent.RejectionCause cause) {
    PermissionCheck.checkMerge(repository);
    setRejected(repository, pullRequestId, cause);
  }

  @Override
  public void setRejected(Repository repository, String pullRequestId, PullRequestRejectedEvent.RejectionCause cause) {
    PullRequest pullRequest = get(repository, pullRequestId);
    if (pullRequest.getStatus() == OPEN) {
      pullRequest.setSourceRevision(branchResolver.resolve(repository, pullRequest.getSource()).getRevision());
      pullRequest.setTargetRevision(branchResolver.resolve(repository, pullRequest.getTarget()).getRevision());
      pullRequest.setStatus(REJECTED);
      getStore(repository).update(pullRequest);
      eventBus.post(new PullRequestRejectedEvent(repository, pullRequest, cause));
    } else if (pullRequest.getStatus() == MERGED) {
      throw new StatusChangeNotAllowedException(repository, pullRequest);
    }
  }

  @Override
  public void setRevisions(Repository repository, String pullRequestId, String targetRevision, String revisionToMerge) {
    PullRequest pullRequest = get(repository, pullRequestId);
    pullRequest.setTargetRevision(targetRevision);
    pullRequest.setSourceRevision(revisionToMerge);
    getStore(repository).update(pullRequest);
  }

  @Override
  public void setMerged(Repository repository, String pullRequestId) {
    PullRequest pullRequest = get(repository, pullRequestId);
    if (pullRequest.getStatus() == OPEN) {
      pullRequest.setStatus(MERGED);
      getStore(repository).update(pullRequest);
      eventBus.post(new PullRequestMergedEvent(repository, pullRequest));
    } else if (pullRequest.getStatus() == REJECTED) {
      throw new StatusChangeNotAllowedException(repository, pullRequest);
    }
  }

  @Override
  public void updated(Repository repository, String pullRequestId) {
    PullRequest pullRequest = get(repository, pullRequestId);

    if (pullRequest.getStatus() == OPEN) {
      eventBus.post(new PullRequestUpdatedEvent(repository, pullRequest));
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
  public void approve(NamespaceAndName namespaceAndName, String pullRequestId, User user) {
    Repository repository = getRepository(namespaceAndName.getNamespace(), namespaceAndName.getName());
    PermissionCheck.checkComment(repository);
    PullRequest pullRequest = getPullRequestFromStore(repository, pullRequestId);
    pullRequest.addApprover(user.getId());
    getStore(repository).update(pullRequest);
    eventBus.post(new PullRequestApprovalEvent(repository, pullRequest, APPROVED));
  }

  @Override
  public void disapprove(NamespaceAndName namespaceAndName, String pullRequestId, User user) {
    Repository repository = getRepository(namespaceAndName.getNamespace(), namespaceAndName.getName());
    PermissionCheck.checkComment(repository);
    PullRequest pullRequest = getPullRequestFromStore(repository, pullRequestId);
    Set<String> approver = pullRequest.getReviewer().keySet();
    approver.stream()
      .filter(recipient -> user.getId().equals(recipient))
      .findFirst()
      .ifPresent(
        approval -> {
          pullRequest.removeApprover(approval);
          getStore(repository).update(pullRequest);
          eventBus.post(new PullRequestApprovalEvent(repository, pullRequest, APPROVAL_REMOVED));
        });
  }

  @Override
  public boolean isUserSubscribed(Repository repository, String pullRequestId, User user) {
    return getStore(repository).get(pullRequestId).getSubscriber()
      .stream()
      .anyMatch(recipient -> user.getId().equals(recipient));
  }

  @Override
  public void subscribe(Repository repository, String pullRequestId, User user) {
    PullRequest pullRequest = getPullRequestFromStore(repository, pullRequestId);
    pullRequest.addSubscriber(user.getId());
    getStore(repository).update(pullRequest);
  }

  @Override
  public void unsubscribe(Repository repository, String pullRequestId, User user) {
    PullRequest pullRequest = getPullRequestFromStore(repository, pullRequestId);
    Set<String> subscriber = pullRequest.getSubscriber();
    subscriber.stream()
      .filter(recipient -> user.getId().equals(recipient))
      .findFirst()
      .ifPresent(pullRequest::removeSubscriber);
    getStore(repository).update(pullRequest);
  }

  @Override
  public void markAsReviewed(Repository repository, String pullRequestId, String path, User user) {
    PullRequest pullRequest = getPullRequestFromStore(repository, pullRequestId);
    Set<ReviewMark> reviewMarks = new HashSet<>(pullRequest.getReviewMarks());
    reviewMarks.add(new ReviewMark(path, user.getId()));
    pullRequest.setReviewMarks(reviewMarks);
    getStore(repository).update(pullRequest);
  }

  private PullRequest getPullRequestFromStore(Repository repository, String pullRequestId) {
    PullRequestStore store = getStore(repository);
    return store.get(pullRequestId);
  }

  private PullRequestStore getStore(String namespace, String name) {
    return getStore(getRepository(namespace, name));
  }

  private PullRequestStore getStore(Repository repository) {
    return storeFactory.create(repository);
  }
}
