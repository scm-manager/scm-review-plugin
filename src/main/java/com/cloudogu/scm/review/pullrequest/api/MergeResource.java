package com.cloudogu.scm.review.pullrequest.api;

import com.cloudogu.scm.review.PullRequestMediaType;
import com.cloudogu.scm.review.PullRequestResourceLinks;
import com.cloudogu.scm.review.pullrequest.dto.MergeCheckResultDto;
import com.cloudogu.scm.review.pullrequest.dto.MergeCommitDto;
import com.cloudogu.scm.review.pullrequest.dto.MergeConflictResultDto;
import com.cloudogu.scm.review.pullrequest.service.MergeCheckResult;
import com.cloudogu.scm.review.pullrequest.service.MergeService;
import de.otto.edison.hal.Links;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import sonia.scm.api.v2.resources.ErrorDto;
import sonia.scm.repository.NamespaceAndName;
import sonia.scm.repository.api.MergeStrategy;
import sonia.scm.repository.spi.MergeConflictResult;
import sonia.scm.web.VndMediaType;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import java.util.List;

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
    service.merge(namespaceAndName, pullRequestId, mergeCommitDto, strategy);
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
      mergeCheckResult.getMergeObstacles()
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
    return service.createDefaultCommitMessage(new NamespaceAndName(namespace, name), pullRequestId, strategy);
  }
}
