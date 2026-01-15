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

import com.cloudogu.mcp.OkResultRenderer;
import com.cloudogu.mcp.ToolResult;
import com.cloudogu.mcp.TypedTool;
import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import com.cloudogu.scm.review.pullrequest.service.PullRequestCreator;
import com.cloudogu.scm.review.pullrequest.service.PullRequestStatus;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.constraints.NotEmpty;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import sonia.scm.plugin.Extension;
import sonia.scm.plugin.Requires;

import java.util.List;

import static java.util.Collections.emptyList;

@Slf4j
@Extension
@Requires("scm-mcp-plugin")
class CreatePullRequestTool implements TypedTool<CreatePullRequestInput> {

  private final PullRequestCreator pullRequestCreator;

  @Inject
  CreatePullRequestTool(PullRequestCreator pullRequestCreator) {
    this.pullRequestCreator = pullRequestCreator;
  }

  @Override
  public Class<? extends CreatePullRequestInput> getInputClass() {
    return CreatePullRequestInput.class;
  }

  @Override
  public ToolResult execute(CreatePullRequestInput input) {
    PullRequest pullRequest = new PullRequest(null, input.getSourceBranch(), input.getTargetBranch());
    pullRequest.setStatus(input.isCreateAsDraft() ? PullRequestStatus.DRAFT : PullRequestStatus.OPEN);
    try {
      String id = pullRequestCreator.openNewPullRequest(
        input.getRepositoryNamespace(),
        input.getRepositoryName(),
        pullRequest,
        input.getInitialTasks()
      );
      return OkResultRenderer.success("Created new pull request with id " + id).render();
    } catch (ConstraintViolationException e) {
      return ToolResult.error("The code on the branches must differ (ie. there has to be at least one commit on the source branch, that is not on the target branch).");
    }
  }

  @Override
  public String getName() {
    return "create-pull-request";
  }

  @Override
  public String getDescription() {
    return "Use this to create new pull requests in a repository.";
  }
}

@Getter
@Setter(AccessLevel.PACKAGE)
@ToString
class CreatePullRequestInput {
  @NotEmpty
  @JsonPropertyDescription("Create the pull requests in the repository with this namespace.")
  private String repositoryNamespace;
  @NotEmpty
  @JsonPropertyDescription("Create the pull requests in the repository with this name.")
  private String repositoryName;
  @NotEmpty
  @JsonPropertyDescription("Title of the new pull request.")
  private String title;
  @JsonPropertyDescription("The description of the new pull request. This can be formatted using markdown.")
  private String description;
  @JsonPropertyDescription("The source branch of the new pull request.")
  private String sourceBranch;
  @JsonPropertyDescription("The target branch of the new pull request.")
  private String targetBranch;
  @JsonPropertyDescription("A new pull request can be marked as a 'draft' to show, that it is not yet ready to be merged.")
  private boolean createAsDraft = false;
  @JsonPropertyDescription("Initial tasks for the new pull request. Tasks can be formatted using markdown")
  private List<String> initialTasks = emptyList();
}
