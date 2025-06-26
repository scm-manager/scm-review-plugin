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

package com.cloudogu.scm.review.pullrequest.api;

import com.cloudogu.scm.review.PullRequestMediaType;
import com.cloudogu.scm.review.PullRequestResourceLinks;
import com.cloudogu.scm.review.pullrequest.dto.MergeCheckResultDto;
import com.cloudogu.scm.review.pullrequest.dto.MergeCommitDto;
import com.cloudogu.scm.review.pullrequest.dto.MergeConflictResultDto;
import com.cloudogu.scm.review.pullrequest.dto.MergeStrategyInfoDto;
import com.cloudogu.scm.review.pullrequest.service.MergeCheckResult;
import com.cloudogu.scm.review.pullrequest.service.MergeService;
import de.otto.edison.hal.Links;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.UriInfo;
import org.apache.commons.lang.StringUtils;
import sonia.scm.api.v2.resources.ErrorDto;
import sonia.scm.repository.NamespaceAndName;
import sonia.scm.repository.api.MergeStrategy;
import sonia.scm.repository.spi.MergeConflictResult;
import sonia.scm.user.DisplayUser;
import sonia.scm.web.VndMediaType;

import java.util.List;

import static java.lang.String.format;

@Path(MergeResource.MERGE_PATH_V2)
public class MergeResource {

  static final String MERGE_PATH_V2 = "v2/merge";
  private final MergeService service;

  @Inject
  public MergeResource(MergeService service) {
    this.service = service;
  }

  @POST
  @Path("{namespace}/{name}/{pullRequestId}")
  @Consumes(PullRequestMediaType.MERGE_COMMAND)
  @Operation(summary = "Merge pull request", description = "Merges pull request with selected strategy.", tags = "Pull Request")
  @ApiResponse(responseCode = "204", description = "update success")
  @ApiResponse(responseCode = "401", description = "not authenticated / invalid credentials")
  @ApiResponse(responseCode = "403", description = "not authorized, the current user does not have the \"mergePullRequest\" privilege")
  @ApiResponse(responseCode = "404", description = "not found, no pull request with the specified id is available")
  @ApiResponse(
    responseCode = "500",
    description = "internal server error",
    content = @Content(
      mediaType = VndMediaType.ERROR_TYPE,
      schema = @Schema(implementation = ErrorDto.class)
    )
  )
  public void merge(
    @PathParam("namespace") String namespace,
    @PathParam("name") String name,
    @PathParam("pullRequestId") String pullRequestId,
    @QueryParam("strategy") MergeStrategy strategy,
    @NotNull @Valid MergeCommitDto mergeCommitDto
  ) {
    NamespaceAndName namespaceAndName = new NamespaceAndName(namespace, name);
    service.merge(namespaceAndName, pullRequestId, mergeCommitDto, strategy, false);
  }

  @POST
  @Path("{namespace}/{name}/{pullRequestId}/emergency")
  @Consumes(PullRequestMediaType.MERGE_COMMAND)
  @Operation(summary = "Merge pull request", description = "Merges pull request with selected strategy as emergency merge.", tags = "Pull Request")
  @ApiResponse(responseCode = "204", description = "update success")
  @ApiResponse(responseCode = "401", description = "not authenticated / invalid credentials")
  @ApiResponse(responseCode = "403", description = "not authorized, the current user does not have the \"performEmergencyMerge\" privilege")
  @ApiResponse(responseCode = "404", description = "not found, no pull request with the specified id is available")
  @ApiResponse(
    responseCode = "500",
    description = "internal server error",
    content = @Content(
      mediaType = VndMediaType.ERROR_TYPE,
      schema = @Schema(implementation = ErrorDto.class)
    )
  )
  public void emergencyMerge(
    @PathParam("namespace") String namespace,
    @PathParam("name") String name,
    @PathParam("pullRequestId") String pullRequestId,
    @QueryParam("strategy") MergeStrategy strategy,
    @NotNull @Valid MergeCommitDto mergeCommitDto
  ) {
    NamespaceAndName namespaceAndName = new NamespaceAndName(namespace, name);
    service.merge(namespaceAndName, pullRequestId, mergeCommitDto, strategy, true);
  }

  @POST
  @Path("{namespace}/{name}/{pullRequestId}/merge-check")
  @Produces(PullRequestMediaType.MERGE_CHECK_RESULT)
  @Operation(summary = "Check pull request merge", description = "Checks if the pull request can be merged.", tags = "Pull Request")
  @ApiResponse(
    responseCode = "200",
    description = "update success",
    content = @Content(
      mediaType = PullRequestMediaType.MERGE_CHECK_RESULT,
      schema = @Schema(implementation = MergeCheckResultDto.class)
    )
  )
  @ApiResponse(responseCode = "401", description = "not authenticated / invalid credentials")
  @ApiResponse(responseCode = "403", description = "not authorized, the current user does not have the \"readPullRequest\" privilege")
  @ApiResponse(responseCode = "404", description = "not found, no pull request with the specified id is available")
  @ApiResponse(
    responseCode = "500",
    description = "internal server error",
    content = @Content(
      mediaType = VndMediaType.ERROR_TYPE,
      schema = @Schema(implementation = ErrorDto.class)
    )
  )
  public MergeCheckResultDto check(
    @PathParam("namespace") String namespace,
    @PathParam("name") String name,
    @PathParam("pullRequestId") String pullRequestId,
    @Context UriInfo uriInfo
  ) {
    NamespaceAndName namespaceAndName = new NamespaceAndName(namespace, name);
    MergeCheckResult mergeCheckResult = service.checkMerge(namespaceAndName, pullRequestId);
    String checkLink = new PullRequestResourceLinks(uriInfo::getBaseUri).mergeLinks().check(namespace, name, pullRequestId);
    return new MergeCheckResultDto(
      Links.linkingTo().self(checkLink).build(),
      mergeCheckResult.hasConflicts(),
      mergeCheckResult.getMergeObstacles(),
      mergeCheckResult.getReasons()
    );
  }

  @POST
  @Path("{namespace}/{name}/{pullRequestId}/conflicts")
  @Produces(PullRequestMediaType.MERGE_CONFLICT_RESULT)
  @Operation(summary = "Get merge conflicts", description = "Returns merge conflicts for pull request.", tags = "Pull Request")
  @ApiResponse(
    responseCode = "200",
    description = "success",
    content = @Content(
      mediaType = PullRequestMediaType.MERGE_CONFLICT_RESULT,
      schema = @Schema(implementation = MergeConflictResultDto.class)
    )
  )
  @ApiResponse(responseCode = "401", description = "not authenticated / invalid credentials")
  @ApiResponse(responseCode = "403", description = "not authorized, the current user does not have the \"readPullRequest\" privilege")
  @ApiResponse(responseCode = "404", description = "not found, no pull request with the specified id is available")
  @ApiResponse(
    responseCode = "500",
    description = "internal server error",
    content = @Content(
      mediaType = VndMediaType.ERROR_TYPE,
      schema = @Schema(implementation = ErrorDto.class)
    )
  )
  public MergeConflictResultDto conflicts(
    @PathParam("namespace") String namespace,
    @PathParam("name") String name,
    @PathParam("pullRequestId") String pullRequestId,
    @Context UriInfo uriInfo
  ) {
    String conflictsLink = new PullRequestResourceLinks(uriInfo::getBaseUri).mergeLinks().conflicts(namespace, name, pullRequestId);
    List<MergeConflictResult.SingleMergeConflict> conflicts = service.conflicts(new NamespaceAndName(namespace, name), pullRequestId).getConflicts();
    return new MergeConflictResultDto(
      Links.linkingTo().self(conflictsLink).build(),
      conflicts
    );
  }

  @Deprecated
  @GET
  @Path("{namespace}/{name}/{pullRequestId}/commit-message")
  @Produces("text/plain")
  @Operation(
    summary = "Get default merge message",
    description = "Returns the default merge commit message by the selected strategy.",
    tags = "Pull Request"
  )
  @ApiResponse(
    responseCode = "200",
    description = "commit message was created",
    content = @Content(
      schema = @Schema(type = "string")
    )
  )
  @ApiResponse(responseCode = "401", description = "not authenticated / invalid credentials")
  @ApiResponse(
    responseCode = "500",
    description = "internal server error",
    content = @Content(
      mediaType = VndMediaType.ERROR_TYPE,
      schema = @Schema(implementation = ErrorDto.class)
    )
  )
  public String createDefaultCommitMessage(
    @PathParam("namespace") String namespace,
    @PathParam("name") String name,
    @PathParam("pullRequestId") String pullRequestId,
    @QueryParam("strategy") MergeStrategy strategy
  ) {
    return service.createCommitDefaults(new NamespaceAndName(namespace, name), pullRequestId, strategy).getCommitMessage();
  }

  @GET
  @Path("{namespace}/{name}/{pullRequestId}/merge-strategy-info")
  @Produces(PullRequestMediaType.MERGE_STRATEGY_INFO)
  @Operation(
    summary = "Get commit message information",
    description = "Returns commit message information for the given merge strategy",
    tags = "Pull Request"
  )
  @ApiResponse(
    responseCode = "200",
    description = "commit message was created",
    content = @Content(
      schema = @Schema(implementation = MergeStrategyInfoDto.class)
    )
  )
  @ApiResponse(responseCode = "401", description = "not authenticated / invalid credentials")
  @ApiResponse(
    responseCode = "500",
    description = "internal server error",
    content = @Content(
      mediaType = VndMediaType.ERROR_TYPE,
      schema = @Schema(implementation = ErrorDto.class)
    )
  )
  public MergeStrategyInfoDto getMergeStrategyInfo(
    @PathParam("namespace") String namespace,
    @PathParam("name") String name,
    @PathParam("pullRequestId") String pullRequestId,
    @QueryParam("strategy") MergeStrategy strategy
  ) {
    MergeService.CommitDefaults commitDefaults = service.createCommitDefaults(new NamespaceAndName(namespace, name), pullRequestId, strategy);
    DisplayUser commitAuthor = commitDefaults.getCommitAuthor();
    return new MergeStrategyInfoDto(
      service.isCommitMessageDisabled(strategy),
      commitDefaults.getCommitMessage(),
      service.createMergeCommitMessageHint(strategy),
      renderCommitAuthorIfPresent(commitAuthor)
    );
  }

  private String renderCommitAuthorIfPresent(DisplayUser commitAuthor) {
    if (commitAuthor == null) {
      return null;
    }
    return StringUtils.isEmpty(commitAuthor.getMail()) ? commitAuthor.getDisplayName() : format("%s <%s>", commitAuthor.getDisplayName(), commitAuthor.getMail());
  }
}
