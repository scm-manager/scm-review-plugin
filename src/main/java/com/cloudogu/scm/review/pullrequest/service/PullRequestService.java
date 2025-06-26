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

import com.cloudogu.scm.review.CurrentUserResolver;
import com.cloudogu.scm.review.PermissionCheck;
import com.cloudogu.scm.review.pullrequest.api.PullRequestSelector;
import com.cloudogu.scm.review.pullrequest.api.RequestParameters;
import com.cloudogu.scm.review.pullrequest.dto.PullRequestCheckResultDto;
import sonia.scm.repository.NamespaceAndName;
import sonia.scm.repository.Repository;
import sonia.scm.user.User;

import java.util.Collection;
import java.util.List;
import java.util.Optional;


/**
 * The Pull Request Service manage the stored pull requests
 *
 * @author Mohamed Karray
 * @since 2.0.0
 */
public interface PullRequestService {


  /**
   * Add a new Pull Request related to the given repository and return the id
   *
   * @return the id of the created pull request
   */
  String add(Repository repository, PullRequest pullRequest);


  /**
   * Return the pull request of the given repository and with the given id
   *
   * @return the pull request of the given repository and with the given id
   */
  default PullRequest get(String namespace, String name, String pullRequestId) {
    return get(getRepository(namespace, name), pullRequestId);
  }

  PullRequest get(Repository repository, String pullRequestId);

  /**
   * Return the pull request with the given repository, source, target and status
   *
   * @return the pull request with the given repository, source, target and status
   */
  Optional<PullRequest> getInProgress(Repository repository, String source, String target);

  /**
   * Return all pull requests related to the given repository
   *
   * @return all pull requests related to the given repository
   */
  List<PullRequest> getAll(String namespace, String name);

  List<PullRequest> getAll(String namespace, String name, RequestParameters parameters);

  int count(String namespace, String name, PullRequestSelector selector);

  /**
   * Check if the branch exists in the repository
   */
  void checkBranch(Repository repository, String branch);

  /**
   * Return the repository with the given name and namespace
   *
   * @return the repository with the given name and namespace
   */
  Repository getRepository(String namespace, String name);

  /**
   * Update the title and the description of the pull request with id <code>pullRequestId</code>
   * the modified Date will be set to now.
   */
  default void update(String namespace, String name, String pullRequestId, PullRequest pullRequest) {
    update(getRepository(namespace, name), pullRequestId, pullRequest);
  }

  void update(Repository repository, String pullRequestId, PullRequest pullRequest);

  default void reject(Repository repository, String pullRequestId, String message) {
    PermissionCheck.checkReject(repository, get(repository, pullRequestId));
    setRejected(repository, pullRequestId, PullRequestRejectedEvent.RejectionCause.REJECTED_BY_USER, message);
  }

  /**
   *
   * @deprecated Pull requests should be rejected with a message by the user. Use
   * {@link #reject(Repository, String, String)} instead.
   */
  @Deprecated(since = "2.28.0")
  default void reject(Repository repository, String pullRequestId, PullRequestRejectedEvent.RejectionCause cause) {
    PermissionCheck.checkMerge(repository);
    setRejected(repository, pullRequestId, cause);
  }

  default void setRejected(Repository repository, String pullRequestId, PullRequestRejectedEvent.RejectionCause cause) {
    setRejected(repository, pullRequestId, cause, null);
  }

  /**
   * Reopen a previously rejected pull request.
   *
   * @since 2.33.0
   */
  void reopen(Repository repository, String pullRequestId);

  /**
   * @since 2.33.0
   */
  PullRequestCheckResultDto.PullRequestCheckStatus checkIfPullRequestIsValid(Repository repository, String source, String target);

  /**
   * @since 3.2.0
   */
  PullRequestCheckResultDto.PullRequestCheckStatus checkIfPullRequestIsValid(Repository repository, PullRequest pullRequest, String target);

  void setRejected(Repository repository, String pullRequestId, PullRequestRejectedEvent.RejectionCause cause, String message);

  void setRevisions(Repository repository, String id, String targetRevision, String revisionToMerge);

  void setMerged(Repository repository, String pullRequestId);

  void setEmergencyMerged(Repository repository, String pullRequestId, String overrideMessage, List<String> ignoredMergeObstacles);

  void sourceRevisionChanged(Repository repository, String pullRequestId);

  void targetRevisionChanged(Repository repository, String pullRequestId);

  boolean hasUserApproved(Repository repository, String pullRequestId, User user);

  default boolean hasUserApproved(Repository repository, String pullRequestId) {
    return hasUserApproved(repository, pullRequestId, getCurrentUser());
  }

  void approve(NamespaceAndName namespaceAndName, String pullRequestId, User user);

  default void approve(NamespaceAndName namespaceAndName, String pullRequestId) {
    approve(namespaceAndName, pullRequestId, getCurrentUser());
  }

  void disapprove(NamespaceAndName namespaceAndName, String pullRequestId, User user);

  default void disapprove(NamespaceAndName namespaceAndName, String pullRequestId) {
    disapprove(namespaceAndName, pullRequestId, getCurrentUser());
  }

  boolean isUserSubscribed(Repository repository, String pullRequestId, User user);

  default boolean isUserSubscribed(Repository repository, String pullRequestId) {
    return isUserSubscribed(repository, pullRequestId, getCurrentUser());
  }

  void subscribe(Repository repository, String pullRequestId, User user);

  default void subscribe(Repository repository, String pullRequestId) {
    subscribe(repository, pullRequestId, getCurrentUser());
  }

  void unsubscribe(Repository repository, String pullRequestId, User user);

  default void unsubscribe(Repository repository, String pullRequestId) {
    unsubscribe(repository, pullRequestId, getCurrentUser());
  }

  void markAsReviewed(Repository repository, String pullRequestId, String path, User user);

  default void markAsReviewed(Repository repository, String pullRequestId, String path) {
    markAsReviewed(repository, pullRequestId, path, getCurrentUser());
  }

  void markAsNotReviewed(Repository repository, String pullRequestId, String path, User user);

  default void markAsNotReviewed(Repository repository, String pullRequestId, String path) {
    markAsNotReviewed(repository, pullRequestId, path, getCurrentUser());
  }

  void removeReviewMarks(Repository repository, String pullRequestId, Collection<ReviewMark> marksToBeRemoved);

  boolean supportsPullRequests(Repository repository);

  void convertToPR(Repository repository, String pullRequestId);

  default User getCurrentUser() {
    return CurrentUserResolver.getCurrentUser();
  }
}
