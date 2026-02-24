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

package com.cloudogu.scm.review.mcp;

import com.cloudogu.mcp.ToolDiffExtensionPoint;
import com.cloudogu.mcp.ToolDiffInput;
import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import com.cloudogu.scm.review.pullrequest.service.PullRequestService;
import jakarta.inject.Inject;
import sonia.scm.plugin.Extension;
import sonia.scm.plugin.Requires;
import sonia.scm.repository.api.DiffResultCommandBuilder;

@Extension
@Requires("scm-mcp-plugin")
public class PullRequestDiffExtension implements ToolDiffExtensionPoint {

  private final PullRequestService pullRequestService;

  @Inject
  public PullRequestDiffExtension(PullRequestService pullRequestService) {
    this.pullRequestService = pullRequestService;
  }

  @Override
  public String usageDescription() {
    return "Pull Request: Use a '#' prefix followed by the ID (e.g., '#42').";
  }

  @Override
  public boolean canHandleExpression(ToolDiffInput input) {
    return input.getDiffTarget().matches("#[0-9]+");
  }

  @Override
  public void handleExpression(ToolDiffInput input, DiffResultCommandBuilder diffResultCommandBuilder) {
    String pullRequestId = input.getDiffTarget().substring(1);

    PullRequest pullRequest = pullRequestService.get(input.getNamespace(), input.getName(), pullRequestId);

    diffResultCommandBuilder.setAncestorChangeset(pullRequest.getTarget());
    diffResultCommandBuilder.setRevision(pullRequest.getSource());
  }
}
