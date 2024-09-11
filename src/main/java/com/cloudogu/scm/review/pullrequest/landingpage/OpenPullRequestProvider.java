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

package com.cloudogu.scm.review.pullrequest.landingpage;

import com.cloudogu.scm.review.PermissionCheck;
import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import com.cloudogu.scm.review.pullrequest.service.PullRequestService;
import com.cloudogu.scm.review.pullrequest.service.PullRequestStatus;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryManager;
import sonia.scm.repository.api.Command;
import sonia.scm.repository.api.RepositoryService;
import sonia.scm.repository.api.RepositoryServiceFactory;

import jakarta.inject.Inject;
import java.util.List;
import java.util.stream.Stream;

import static com.cloudogu.scm.review.pullrequest.service.PullRequestStatus.DRAFT;
import static com.cloudogu.scm.review.pullrequest.service.PullRequestStatus.OPEN;
import static java.util.Arrays.asList;

class OpenPullRequestProvider {

  private final RepositoryServiceFactory serviceFactory;
  private final PullRequestService pullRequestService;
  private final RepositoryManager repositoryManager;

  @Inject
  OpenPullRequestProvider(RepositoryServiceFactory serviceFactory, PullRequestService pullRequestService, RepositoryManager repositoryManager) {
    this.serviceFactory = serviceFactory;
    this.pullRequestService = pullRequestService;
    this.repositoryManager = repositoryManager;
  }

  void findOpenAndDraftPullRequests(RepositoryAndPullRequestConsumer forEachPullRequest) {
    findPullRequestsWithStatus(forEachPullRequest, OPEN, DRAFT);
  }

  void findOpenPullRequests(RepositoryAndPullRequestConsumer forEachPullRequest) {
    findPullRequestsWithStatus(forEachPullRequest, OPEN);
  }

  void findPullRequestsWithStatus(RepositoryAndPullRequestConsumer forEachPullRequest, PullRequestStatus... status) {
    repositoryManager.getAll()
      .stream()
      .filter(PermissionCheck::mayRead)
      .filter(this::supportsPullRequests)
      .forEach(
        repository ->
          forEachPullRequest.accept(
            repository,
            allPullRequestsFor(repository).stream().filter(pr -> asList(status).contains(pr.getStatus()))
          )
      );
  }

  private List<PullRequest> allPullRequestsFor(Repository repository) {
    return pullRequestService.getAll(repository.getNamespace(), repository.getName());
  }

  private boolean supportsPullRequests(Repository repository) {
    try (RepositoryService repositoryService = serviceFactory.create(repository)) {
      return repositoryService.isSupported(Command.MERGE);
    }
  }

  @FunctionalInterface
  interface RepositoryAndPullRequestConsumer {
    void accept(Repository repository, Stream<PullRequest> forEachPullRequest);
  }
}
