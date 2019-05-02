package com.cloudogu.scm.review.pullrequest.service;

import sonia.scm.repository.Repository;
import sonia.scm.user.User;

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
  String add(Repository repository, PullRequest pullRequest) throws NoDifferenceException;


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

  void reject(Repository repository, PullRequest pullRequest);

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
}
