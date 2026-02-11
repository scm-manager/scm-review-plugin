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

package com.cloudogu.scm.review.pullrequest.service;

import com.cloudogu.scm.review.BranchResolver;
import com.cloudogu.scm.review.CurrentUserResolver;
import com.cloudogu.scm.review.PermissionCheck;
import com.cloudogu.scm.review.RepositoryResolver;
import com.cloudogu.scm.review.StatusChangeNotAllowedException;
import com.cloudogu.scm.review.pullrequest.api.PullRequestSelector;
import com.cloudogu.scm.review.pullrequest.api.PullRequestSortSelector;
import com.cloudogu.scm.review.pullrequest.api.RequestParameters;
import com.cloudogu.scm.review.pullrequest.dto.PullRequestCheckResultDto;
import com.google.common.base.Strings;
import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.UnauthorizedException;
import sonia.scm.ContextEntry;
import sonia.scm.HandlerEventType;
import sonia.scm.NotFoundException;
import sonia.scm.event.ScmEventBus;
import sonia.scm.repository.ChangesetPagingResult;
import sonia.scm.repository.NamespaceAndName;
import sonia.scm.repository.Repository;
import sonia.scm.repository.api.Command;
import sonia.scm.repository.api.RepositoryService;
import sonia.scm.repository.api.RepositoryServiceFactory;
import sonia.scm.store.Condition;
import sonia.scm.store.QueryableStore;
import sonia.scm.user.User;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.cloudogu.scm.review.pullrequest.dto.PullRequestCheckResultDto.PullRequestCheckStatus.BRANCHES_NOT_DIFFER;
import static com.cloudogu.scm.review.pullrequest.dto.PullRequestCheckResultDto.PullRequestCheckStatus.PR_ALREADY_EXISTS;
import static com.cloudogu.scm.review.pullrequest.dto.PullRequestCheckResultDto.PullRequestCheckStatus.PR_VALID;
import static com.cloudogu.scm.review.pullrequest.dto.PullRequestCheckResultDto.PullRequestCheckStatus.SAME_BRANCHES;
import static com.cloudogu.scm.review.pullrequest.service.PullRequestApprovalEvent.ApprovalCause.APPROVAL_REMOVED;
import static com.cloudogu.scm.review.pullrequest.service.PullRequestApprovalEvent.ApprovalCause.APPROVED;
import static com.cloudogu.scm.review.pullrequest.service.PullRequestStatus.DRAFT;
import static com.cloudogu.scm.review.pullrequest.service.PullRequestStatus.MERGED;
import static com.cloudogu.scm.review.pullrequest.service.PullRequestStatus.OPEN;
import static com.cloudogu.scm.review.pullrequest.service.PullRequestStatus.REJECTED;
import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static sonia.scm.AlreadyExistsException.alreadyExists;

@Slf4j
public class DefaultPullRequestService implements PullRequestService {

  private final BranchResolver branchResolver;
  private final RepositoryResolver repositoryResolver;
  private final PullRequestStoreBuilder storeFactory;
  private final ScmEventBus eventBus;
  private final RepositoryServiceFactory repositoryServiceFactory;

  @Inject
  public DefaultPullRequestService(RepositoryResolver repositoryResolver, BranchResolver branchResolver, PullRequestStoreBuilder storeFactory, ScmEventBus eventBus, RepositoryServiceFactory repositoryServiceFactory) {
    this.repositoryResolver = repositoryResolver;
    this.branchResolver = branchResolver;
    this.storeFactory = storeFactory;
    this.eventBus = eventBus;
    this.repositoryServiceFactory = repositoryServiceFactory;
  }

  @Override
  public String add(Repository repository, PullRequest pullRequest) {
    verifyBranchCheck(checkIfPullRequestIsValid(repository, pullRequest, pullRequest.getSource(), pullRequest.getTarget()), repository, pullRequest);
    Instant now = Instant.now();
    pullRequest.setCreationDate(now);
    pullRequest.setLastModified(now);
    computeSubscriberForNewPullRequest(pullRequest);
    try (PullRequestStore store = getStore(repository)) {
      String id = store.add(pullRequest);
      eventBus.post(new PullRequestEvent(repository, pullRequest, null, HandlerEventType.CREATE));
      return id;
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
    try (PullRequestStore store = getStore(repository)) {
      PullRequest oldPullRequest = store.get(pullRequestId);
      Set<String> addedReviewers = computeAddedReviewersForChangedPullRequest(oldPullRequest, pullRequest);
      Set<String> removedReviewers = computeRemovedReviewersForChangedPullRequest(oldPullRequest, pullRequest);

      Set<String> newSubscriber = new HashSet<>(oldPullRequest.getSubscriber());
      newSubscriber.addAll(addedReviewers);

      Map<String, Boolean> newReviewers = new HashMap<>(oldPullRequest.getReviewer());

      if (oldPullRequest.isOpen() && pullRequest.isDraft()) {
        eventBus.post(new PullRequestStatusChangedEvent(repository, pullRequest, DRAFT));
        newReviewers
          .keySet()
          .forEach(reviewer -> newReviewers.put(reviewer, false));
      } else if (oldPullRequest.isDraft() && pullRequest.isOpen()) {
        eventBus.post(new PullRequestStatusChangedEvent(repository, pullRequest, OPEN));
      }

      addedReviewers.forEach(reviewer -> newReviewers.putIfAbsent(reviewer, false));
      removedReviewers.forEach(newReviewers::remove);

      PullRequest newPullRequest = oldPullRequest.toBuilder()
        .targetRevision(pullRequest.getTargetRevision())
        .title(pullRequest.getTitle())
        .description(pullRequest.getDescription())
        .lastModified(Instant.now())
        .reviewer(newReviewers)
        .labels(pullRequest.getLabels())
        .target(pullRequest.getTarget())
        .shouldDeleteSourceBranch(pullRequest.isShouldDeleteSourceBranch())
        .build();

      if (oldPullRequest.isInProgress() && pullRequest.getStatus() != null && pullRequest.getStatus().isInProgress()) {
        newPullRequest.setStatus(pullRequest.getStatus());
      }

      newPullRequest.setSubscriber(newSubscriber);
      boolean targetBranchHasChanged = !StringUtils.equals(newPullRequest.getTarget(), oldPullRequest.getTarget());
      if (targetBranchHasChanged) {
        verifyBranchCheck(checkIfPullRequestIsValid(repository, pullRequest, newPullRequest.getTarget()), repository, pullRequest);
        removeAllApprover(newPullRequest);
      }
      store.update(newPullRequest);
      eventBus.post(new PullRequestEvent(repository, newPullRequest, oldPullRequest, HandlerEventType.MODIFY));
      if (targetBranchHasChanged) {
        eventBus.post(new PullRequestUpdatedEvent(repository, newPullRequest));
      }
    }
  }

  private void verifyBranchCheck(PullRequestCheckResultDto.PullRequestCheckStatus checkStatus, Repository repository, PullRequest pullRequest) {
    switch (checkStatus) {
      case BRANCHES_NOT_DIFFER -> throw new NoDifferenceException(repository);
      case PR_ALREADY_EXISTS -> throw alreadyExists(ContextEntry.ContextBuilder.entity(PullRequest.class, pullRequest.getId()).in(repository));
      case SAME_BRANCHES -> throw new SameBranchesNotAllowedException(repository, pullRequest);
      default -> {}
    }
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
    try (PullRequestStore store = getStore(repository)) {
      return store.get(id);
    }
  }

  @Override
  public List<PullRequest> getAll(String namespace, String name) {
    try (PullRequestStore store = getStore(namespace, name)) {
      return store.getAll();
    }
  }

  @Override
  public List<PullRequest> getAll(String namespace, String name, RequestParameters requestParameters) {
    Collection<Condition<PullRequest>> conditions =
      new ArrayList<>(computeConditions(requestParameters.pullRequestSelector()));
    if (!Strings.isNullOrEmpty(requestParameters.source())) {
      conditions.add(PullRequestQueryFields.SOURCE.eq(requestParameters.source()));
    }
    if (!Strings.isNullOrEmpty(requestParameters.target())) {
      conditions.add(PullRequestQueryFields.TARGET.eq(requestParameters.target()));
    }
    PullRequestSortSelector.Sort sort = requestParameters.pullRequestSortSelector().getSort();
    try (QueryableStore<PullRequest> store = storeFactory.createQueryable(getRepository(namespace, name))) {
      return store
        .query(conditions.toArray(new Condition[0]))
        .orderBy(sort.field(), sort.orderOptions())
        .findAll(requestParameters.offset(), requestParameters.limit());
    }
  }

  public int count(String namespace, String name, PullRequestSelector selector) {
    Collection<Condition<PullRequest>> conditions =
      computeConditions(selector);
    try (QueryableStore<PullRequest> store = storeFactory.createQueryable(getRepository(namespace, name))) {
      return (int) store
        .query(conditions.toArray(new Condition[0]))
        .count();
    }
  }

  private static Collection<Condition<PullRequest>> computeConditions(PullRequestSelector selector) {
    return switch (selector) {
      case ALL -> emptyList();
      case MINE -> {
        String userId = SecurityUtils.getSubject().getPrincipal().toString();
        yield List.of(
          PullRequestQueryFields.AUTHOR.eq(userId)
        );
      }
      case MERGED -> List.of(PullRequestQueryFields.STATUS.eq(MERGED));
      case REJECTED -> List.of(PullRequestQueryFields.STATUS.eq(REJECTED));
      case REVIEWER -> List.of(
        PullRequestQueryFields.REVIEWER.containsKey(SecurityUtils.getSubject().getPrincipal().toString()),
        PullRequestQueryFields.STATUS.eq(OPEN)
      );
      case IN_PROGRESS, OPEN -> List.of(PullRequestQueryFields.STATUS.in(OPEN, DRAFT));
    };
  }

  @Override
  public Optional<PullRequest> getInProgress(Repository repository, String source, String target) {
    List<PullRequestStatus> inProgressStatus = stream(PullRequestStatus.values())
      .filter(PullRequestStatus::isInProgress)
      .toList();
    try (PullRequestStore store = getStore(repository)) {
      return store.getAll()
        .stream()
        .filter(pullRequest ->
          inProgressStatus.contains(pullRequest.getStatus()) &&
            pullRequest.getSource().equals(source) &&
            pullRequest.getTarget().equals(target))
        .findFirst();
    }
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
  public void setRejected(Repository repository, String pullRequestId, PullRequestRejectedEvent.RejectionCause cause, String message) {
    PullRequest pullRequest = get(repository, pullRequestId);
    if (pullRequest.isInProgress()) {
      PullRequestStatus previousStatus = pullRequest.getStatus();
      pullRequest.setSourceRevision(getBranchRevision(repository, pullRequest.getSource()));
      pullRequest.setTargetRevision(getBranchRevision(repository, pullRequest.getTarget()));
      setPullRequestClosed(pullRequest);
      pullRequest.setStatus(REJECTED);
      updatePullRequest(repository, pullRequest);
      eventBus.post(new PullRequestRejectedEvent(repository, pullRequest, cause, message, previousStatus));
    } else if (pullRequest.isMerged()) {
      throw new StatusChangeNotAllowedException(repository, pullRequest);
    }
  }

  @Override
  public void reopen(Repository repository, String pullRequestId) {
    try (PullRequestStore store = getStore(repository)) {
      PullRequest pullRequest = store.get(pullRequestId);
      PermissionCheck.checkCreate(repository);
      checkBranch(repository, pullRequest.getSource());
      checkBranch(repository, pullRequest.getTarget());

      boolean isPrCreationValid = PR_VALID.equals(checkIfPullRequestIsValid(repository, pullRequest.getSource(), pullRequest.getTarget()));
      if (pullRequest.isRejected() && isPrCreationValid) {
        pullRequest.setStatus(OPEN);
        pullRequest.setSourceRevision(null);
        pullRequest.setTargetRevision(null);
        pullRequest.setReviser(null);
        pullRequest.setCloseDate(null);
        store.update(pullRequest);
        eventBus.post(new PullRequestReopenedEvent(repository, pullRequest));
      } else {
        throw new StatusChangeNotAllowedException(repository, pullRequest);
      }
    }
  }

  @Override
  public PullRequestCheckResultDto.PullRequestCheckStatus checkIfPullRequestIsValid(Repository repository, String source, String target) {
    return checkIfPullRequestIsValid(repository, null, source, target);
  }

  @Override
  public PullRequestCheckResultDto.PullRequestCheckStatus checkIfPullRequestIsValid(Repository repository, PullRequest pullRequest, String target) {
    return checkIfPullRequestIsValid(repository, pullRequest, pullRequest.getSource(), target);
  }

  private PullRequestCheckResultDto.PullRequestCheckStatus checkIfPullRequestIsValid(Repository repository, PullRequest pullRequest, String source, String target) {
    if (source.equals(target)) {
      return SAME_BRANCHES;
    }

    if (getInProgress(repository, source, target).filter(pr -> pullRequest == null || !pr.getId().equals(pullRequest.getId())).isPresent()) {
      return PR_ALREADY_EXISTS;
    }

    try (RepositoryService repositoryService = repositoryServiceFactory.create(repository)) {
      ChangesetPagingResult changesets = repositoryService
        .getLogCommand()
        .setStartChangeset(source)
        .setAncestorChangeset(target)
        .setPagingLimit(1)
        .getChangesets();

      if (changesets == null || changesets.getChangesets() == null || changesets.getChangesets().isEmpty()) {
        return BRANCHES_NOT_DIFFER;
      }
    } catch (IOException e) {
      log.warn("could not check if pull request is valid in repository {} for source branch {} and target branch {}", repository, source, target, e);
    }

    return PR_VALID;
  }

  private String getBranchRevision(Repository repository, String branch) {
    try {
      return branchResolver.resolve(repository, branch).getRevision();
    } catch (NotFoundException e) {
      // If the branch was already deleted before we cannot set the revision, so we use the branch name.
      return branch;
    }
  }

  @Override
  public void setRevisions(Repository repository, String pullRequestId, String targetRevision, String revisionToMerge) {
    PullRequest pullRequest = get(repository, pullRequestId);
    pullRequest.setTargetRevision(targetRevision);
    pullRequest.setSourceRevision(revisionToMerge);
    updatePullRequest(repository, pullRequest);
  }

  @Override
  public void setMerged(Repository repository, String pullRequestId) {
    PullRequest pullRequest = get(repository, pullRequestId);
    if (pullRequest.isInProgress()) {
      pullRequest.setStatus(MERGED);
      setPullRequestClosed(pullRequest);
      updatePullRequest(repository, pullRequest);
      eventBus.post(new PullRequestMergedEvent(repository, pullRequest));
    } else if (pullRequest.isRejected()) {
      throw new StatusChangeNotAllowedException(repository, pullRequest);
    }
  }

  @Override
  public void setEmergencyMerged(Repository repository, String pullRequestId, String overrideMessage, List<String> ignoredMergeObstacles) {
    PullRequest pullRequest = get(repository, pullRequestId);
    pullRequest.setOverrideMessage(overrideMessage);
    pullRequest.setEmergencyMerged(true);
    pullRequest.setStatus(MERGED);
    setPullRequestClosed(pullRequest);
    pullRequest.setIgnoredMergeObstacles(ignoredMergeObstacles);
    updatePullRequest(repository, pullRequest);
    eventBus.post(new PullRequestEmergencyMergedEvent(repository, pullRequest));
  }

  private void setPullRequestClosed(PullRequest pullRequest) {
    pullRequest.setReviser(getCurrentUser().getName());
    pullRequest.setCloseDate(Instant.now());
  }

  @Override
  public void sourceRevisionChanged(Repository repository, String pullRequestId) {
    PullRequest pullRequest = get(repository, pullRequestId);
    if (pullRequest.isInProgress()) {
      removeAllApprover(pullRequest);
      updatePullRequest(repository, pullRequest);
      eventBus.post(new PullRequestUpdatedEvent(repository, pullRequest));
    }
  }

  @Override
  public void targetRevisionChanged(Repository repository, String pullRequestId) {
    PullRequest pullRequest = get(repository, pullRequestId);
    if (pullRequest.isInProgress()) {
      eventBus.post(new PullRequestUpdatedEvent(repository, pullRequest));
    }
  }

  @Override
  public boolean hasUserApproved(Repository repository, String pullRequestId, User user) {
    try (PullRequestStore store = getStore(repository)) {
      return store.get(pullRequestId).getReviewer().entrySet()
        .stream()
        .filter(Map.Entry::getValue)
        .anyMatch(entry -> entry.getKey().equals(user.getId()));
    }
  }

  @Override
  public void approve(NamespaceAndName namespaceAndName, String pullRequestId, User user) {
    Repository repository = getRepository(namespaceAndName.getNamespace(), namespaceAndName.getName());
    PermissionCheck.checkComment(repository);
    PullRequest pullRequest = getPullRequestFromStore(repository, pullRequestId);
    boolean isNewApprover = !pullRequest.getReviewer().containsKey(user.getId());
    pullRequest.addApprover(user.getId());
    updatePullRequest(repository, pullRequest);
    eventBus.post(new PullRequestApprovalEvent(repository, pullRequest, user, isNewApprover, APPROVED));
  }

  @Override
  public void disapprove(NamespaceAndName namespaceAndName, String pullRequestId, User user) {
    Repository repository = getRepository(namespaceAndName.getNamespace(), namespaceAndName.getName());
    PermissionCheck.checkComment(repository);
    PullRequest pullRequest = getPullRequestFromStore(repository, pullRequestId);
    boolean isNewApprover = !pullRequest.getReviewer().containsKey(user.getId());
    Set<String> approver = pullRequest.getReviewer().keySet();
    approver.stream()
      .filter(recipient -> user.getId().equals(recipient))
      .findFirst()
      .ifPresent(
        approval -> {
          pullRequest.removeApprover(approval);
          updatePullRequest(repository, pullRequest);
          eventBus.post(new PullRequestApprovalEvent(repository, pullRequest, user, isNewApprover, APPROVAL_REMOVED));
        });
  }

  @Override
  public boolean isUserSubscribed(Repository repository, String pullRequestId, User user) {
    try (PullRequestStore store = getStore(repository)) {
      return store.get(pullRequestId).getSubscriber()
        .stream()
        .anyMatch(recipient -> user.getId().equals(recipient));
    }
  }

  @Override
  public void subscribe(Repository repository, String pullRequestId, User user) {
    PullRequest pullRequest = getPullRequestFromStore(repository, pullRequestId);
    pullRequest.addSubscriber(user.getId());
    updatePullRequest(repository, pullRequest);
    eventBus.post(new PullRequestSubscribedEvent(
      repository, pullRequest, user, PullRequestSubscribedEvent.EventType.SUBSCRIBED
    ));
  }

  @Override
  public void unsubscribe(Repository repository, String pullRequestId, User user) {
    PullRequest pullRequest = getPullRequestFromStore(repository, pullRequestId);
    Set<String> subscriber = pullRequest.getSubscriber();
    subscriber.stream()
      .filter(recipient -> user.getId().equals(recipient))
      .findFirst()
      .ifPresent(pullRequest::removeSubscriber);
    updatePullRequest(repository, pullRequest);
    eventBus.post(new PullRequestSubscribedEvent(
      repository, pullRequest, user, PullRequestSubscribedEvent.EventType.UNSUBSCRIBED
    ));
  }

  @Override
  public void markAsReviewed(Repository repository, String pullRequestId, String path, User user) {
    try (PullRequestStore store = getStore(repository)) {
      PullRequest pullRequest = store.get(pullRequestId);
      Set<ReviewMark> reviewMarks = new HashSet<>(pullRequest.getReviewMarks());
      ReviewMark reviewMark = new ReviewMark(path, user.getId());
      reviewMarks.add(reviewMark);
      pullRequest.setReviewMarks(reviewMarks);
      store.update(pullRequest);
      eventBus.post(new PullRequestReviewMarkEvent(
        repository, pullRequest, reviewMark, PullRequestReviewMarkEvent.EventType.ADDED
      ));
    }
  }

  @Override
  public void markAsNotReviewed(Repository repository, String pullRequestId, String path, User user) {
    try (PullRequestStore store = getStore(repository)) {
      PullRequest pullRequest = store.get(pullRequestId);
      Set<ReviewMark> reviewMarks = new HashSet<>(pullRequest.getReviewMarks());
      ReviewMark reviewMark = new ReviewMark(path, user.getId());
      reviewMarks.remove(reviewMark);
      pullRequest.setReviewMarks(reviewMarks);
      store.update(pullRequest);
      eventBus.post(new PullRequestReviewMarkEvent(
        repository, pullRequest, reviewMark, PullRequestReviewMarkEvent.EventType.REMOVED
      ));
    }
  }

  @Override
  public void removeReviewMarks(Repository repository, String pullRequestId, Collection<ReviewMark> marksToBeRemoved) {
    if (marksToBeRemoved.isEmpty()) {
      return;
    }
    PullRequest pullRequest = getPullRequestFromStore(repository, pullRequestId);
    Set<ReviewMark> newReviewMarks = new HashSet<>(pullRequest.getReviewMarks());
    newReviewMarks.removeAll(marksToBeRemoved);
    pullRequest.setReviewMarks(newReviewMarks);
    updatePullRequest(repository, pullRequest);

    for (ReviewMark reviewMark : marksToBeRemoved) {
      eventBus.post(new PullRequestReviewMarkEvent(
        repository, pullRequest, reviewMark, PullRequestReviewMarkEvent.EventType.REMOVED
      ));
    }
  }

  @Override
  public boolean supportsPullRequests(Repository repository) {
    try (RepositoryService repositoryService = repositoryServiceFactory.create(repository)) {
      return repositoryService.isSupported(Command.MERGE);
    }
  }

  @Override
  public void convertToPR(Repository repository, String pullRequestId) {
    PullRequest pullRequest = get(repository, pullRequestId);

    if (!PermissionCheck.mayMerge(repository) &&
      !pullRequest.getAuthor().equals(SecurityUtils.getSubject().getPrincipal().toString())) {
      throw new UnauthorizedException();
    }

    if (pullRequest.isDraft()) {
      PullRequest oldPullRequest = pullRequest.toBuilder().build();
      pullRequest.setStatus(OPEN);
      updatePullRequest(repository, pullRequest);
      eventBus.post(new PullRequestStatusChangedEvent(repository, pullRequest, OPEN));
      eventBus.post(new PullRequestEvent(repository, pullRequest, oldPullRequest, HandlerEventType.MODIFY));
    } else {
      throw new StatusChangeNotAllowedException(repository, pullRequest);
    }
  }

  private void updatePullRequest(Repository repository, PullRequest pullRequest) {
    try (PullRequestStore store = getStore(repository)) {
      store.update(pullRequest);
    }
  }

  private void removeAllApprover(PullRequest pullRequest) {
    pullRequest.getReviewer()
      .keySet()
      .forEach(pullRequest::removeApprover);
  }

  private PullRequest getPullRequestFromStore(Repository repository, String pullRequestId) {
    try (PullRequestStore store = getStore(repository)) {
      return store.get(pullRequestId);
    }
  }

  private PullRequestStore getStore(String namespace, String name) {
    return getStore(getRepository(namespace, name));
  }

  private PullRequestStore getStore(Repository repository) {
    return storeFactory.create(repository);
  }
}
