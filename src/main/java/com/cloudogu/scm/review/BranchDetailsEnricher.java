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

package com.cloudogu.scm.review;

import com.cloudogu.scm.review.pullrequest.dto.PullRequestMapper;
import com.cloudogu.scm.review.pullrequest.service.PullRequestService;
import de.otto.edison.hal.HalRepresentation;
import jakarta.inject.Inject;
import sonia.scm.api.v2.resources.Enrich;
import sonia.scm.api.v2.resources.HalAppender;
import sonia.scm.api.v2.resources.HalEnricher;
import sonia.scm.api.v2.resources.HalEnricherContext;
import sonia.scm.plugin.Extension;
import sonia.scm.repository.BranchDetails;
import sonia.scm.repository.Repository;

import java.util.List;
import java.util.stream.Collectors;

@Extension
@Enrich(BranchDetails.class)
public class BranchDetailsEnricher implements HalEnricher {

  private final PullRequestMapper mapper;
  private final PullRequestService service;

  @Inject
  public BranchDetailsEnricher(PullRequestMapper mapper, PullRequestService service) {
    this.mapper = mapper;
    this.service = service;
  }

  @Override
  public void enrich(HalEnricherContext context, HalAppender appender) {
    Repository repository = context.oneRequireByType(Repository.class);
    String branchName = context.oneRequireByType(BranchDetails.class).getBranchName();
    if (service.supportsPullRequests(repository)) {
      List<HalRepresentation> pullRequestDtos = getPullRequestDtos(repository, branchName);
      appender.appendEmbedded("pullRequests", pullRequestDtos);
    }
  }

  private List<HalRepresentation> getPullRequestDtos(Repository repository, String branchName) {
    return service.getAll(repository.getNamespace(), repository.getName())
      .stream()
      .filter(pr -> pr.getSource().equals(branchName) && pr.isInProgress())
      .map(mapper::map)
      .collect(Collectors.toList());
  }
}
