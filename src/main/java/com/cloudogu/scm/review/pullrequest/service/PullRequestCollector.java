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

import jakarta.inject.Inject;
import sonia.scm.repository.Repository;

import java.util.List;
import java.util.stream.Collectors;

public class PullRequestCollector {

  private final PullRequestService pullRequestService;

  @Inject
  public PullRequestCollector(PullRequestService pullRequestService) {
    this.pullRequestService = pullRequestService;
  }

  public List<PullRequest> collectAffectedPullRequests(Repository repository, List<String> affectedBranches) {
    return pullRequestService.getAll(repository.getNamespace(), repository.getName())
      .stream()
      .filter(PullRequest::isInProgress)
      .filter(pr -> affectedBranches.contains(pr.getSource()) || affectedBranches.contains(pr.getTarget()))
      .collect(Collectors.toList());
  }
}
