/*
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

package com.cloudogu.scm.review.pullrequest.landingpage;

import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import com.cloudogu.scm.review.pullrequest.service.PullRequestService;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryManager;
import sonia.scm.repository.api.Command;
import sonia.scm.repository.api.RepositoryService;
import sonia.scm.repository.api.RepositoryServiceFactory;

import javax.inject.Inject;
import java.util.List;
import java.util.stream.Stream;

import static com.cloudogu.scm.review.pullrequest.service.PullRequestStatus.OPEN;

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

  void findOpenPullRequests(RepositoryAndPullRequestConsumer forEachPullRequest) {
    repositoryManager.getAll().stream().filter(this::supportsPullRequests).forEach(
      repository ->
        forEachPullRequest.accept(
          repository,
          allPullRequestsFor(repository).stream().filter(pr -> pr.getStatus() == OPEN)
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
