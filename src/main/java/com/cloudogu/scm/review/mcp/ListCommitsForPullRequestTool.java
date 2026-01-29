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

import com.cloudogu.mcp.ToolListCommits;
import com.cloudogu.mcp.ToolListCommitsFilterEnhancement;
import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import com.cloudogu.scm.review.pullrequest.service.PullRequestService;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import sonia.scm.NotFoundException;
import sonia.scm.plugin.Extension;
import sonia.scm.plugin.Requires;
import sonia.scm.repository.Repository;
import sonia.scm.repository.api.LogCommandBuilder;

import java.util.Optional;

import static java.util.Optional.of;

@Extension
@Requires("scm-mcp-plugin")
public class ListCommitsForPullRequestTool implements ToolListCommitsFilterEnhancement {

  private final PullRequestService pullRequestService;

  @Inject
  public ListCommitsForPullRequestTool(PullRequestService pullRequestService) {
    this.pullRequestService = pullRequestService;
  }

  @Override
  public Optional<Class<?>> getInputClass() {
    return of(ListCommitsForPullRequestInput.class);
  }

  @Override
  public String getNamespace() {
    return "pullRequest";
  }

  @Override
  public Optional<String> configure(Repository repository, LogCommandBuilder logCommandBuilder, ToolListCommits.CompositeInput input) {
    if (input.getExtensionInput(getNamespace()) instanceof ListCommitsForPullRequestInput prInput) {
      String pullRequestId = prInput.getId();
      String namespace = input.getBaseInput().getNamespace();
      String name = input.getBaseInput().getName();
      try {
        PullRequest pullRequest = pullRequestService.get(repository, pullRequestId);
        logCommandBuilder
          .setStartChangeset(pullRequest.getSource())
          .setAncestorChangeset(pullRequest.getTarget());
      } catch (NotFoundException e) {
          return of(String.format("No pull request with id %s found in repository %s/%s", pullRequestId, namespace, name));
      }
    }
    return Optional.empty();
  }
}

@Getter
@Setter(AccessLevel.PACKAGE)
@ToString
class ListCommitsForPullRequestInput {
  @NotBlank
  @JsonPropertyDescription("Find the commits fot the pull request with this ID.")
  private String id;
}
