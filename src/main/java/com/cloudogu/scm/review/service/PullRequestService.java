package com.cloudogu.scm.review.service;

import sonia.scm.repository.Repository;

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
   * @param repository
   * @param pullRequest
   * @return the id of the created pull request
   */
  String add(Repository repository, PullRequest pullRequest);


  /**
   * Return the pull request of the given repository and with the given id
   *
   * @param name
   * @param namespace
   * @param pullRequestId
   * @return the pull request of the given repository and with the given id
   */
  PullRequest get(String namespace, String name, String pullRequestId);

  /**
   * Return the pull request with the given repository, source, target and status
   *
   * @param repository
   * @param source
   * @param target
   * @param status
   * @return the pull request with the given repository, source, target and status
   */
  Optional<PullRequest> get(Repository repository, String source, String target, PullRequestStatus status);

  /**
   * Return all pull requests related to the given repository
   *
   * @param name
   * @param namespace
   *
   * @return all pull requests related to the given repository
   */
  List<PullRequest> getAll(String namespace, String name);

  /**
   * Check if the branch exists in the repository
   *
   * @param repository
   * @param branch
   */
  void checkBranch(Repository repository, String branch);

  /**
   * Return the repository wuth the given name and namespace
   *
   * @param namespace
   * @param name
   * @return the repository wuth the given name and namespace
   */
  Repository getRepository(String namespace, String name);

}
