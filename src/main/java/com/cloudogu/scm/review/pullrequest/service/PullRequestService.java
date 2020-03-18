/**
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
package com.cloudogu.scm.review.pullrequest.service;

import sonia.scm.repository.NamespaceAndName;
import sonia.scm.repository.Repository;
import sonia.scm.user.User;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static com.cloudogu.scm.review.CurrentUserResolver.getCurrentUser;


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
  Optional<PullRequest> get(Repository repository, String source, String target, PullRequestStatus status);

  /**
   * Return all pull requests related to the given repository
   *
   * @return all pull requests related to the given repository
   */
  List<PullRequest> getAll(String namespace, String name);

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

  void reject(Repository repository, String pullRequestId, PullRequestRejectedEvent.RejectionCause cause);

  void setRejected(Repository repository, String pullRequestId, PullRequestRejectedEvent.RejectionCause cause);

  void setRevisions(Repository repository, String id, String targetRevision, String revisionToMerge);

  void setMerged(Repository repository, String pullRequestId);

  void updated(Repository repository, String pullRequestId);

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
}
