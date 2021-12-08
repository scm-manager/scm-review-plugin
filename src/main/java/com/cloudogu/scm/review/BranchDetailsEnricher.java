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

package com.cloudogu.scm.review;

import com.cloudogu.scm.review.pullrequest.dto.PullRequestMapper;
import com.cloudogu.scm.review.pullrequest.service.PullRequestService;
import com.cloudogu.scm.review.pullrequest.service.PullRequestStatus;
import de.otto.edison.hal.HalRepresentation;
import sonia.scm.api.v2.resources.Enrich;
import sonia.scm.api.v2.resources.HalAppender;
import sonia.scm.api.v2.resources.HalEnricher;
import sonia.scm.api.v2.resources.HalEnricherContext;
import sonia.scm.plugin.Extension;
import sonia.scm.repository.BranchDetails;
import sonia.scm.repository.Repository;

import javax.inject.Inject;
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
      .filter(pr -> pr.getSource().equals(branchName) && pr.getStatus() == PullRequestStatus.OPEN)
      .map(mapper::map)
      .collect(Collectors.toList());
  }
}
