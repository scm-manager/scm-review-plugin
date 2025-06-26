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

package com.cloudogu.scm.review.workflow;

import com.cloudogu.scm.review.PullRequestResourceLinks;
import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import com.cloudogu.scm.review.pullrequest.service.PullRequestService;
import de.otto.edison.hal.Links;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.UriInfo;
import sonia.scm.api.v2.resources.ErrorDto;
import sonia.scm.repository.Repository;
import sonia.scm.web.VndMediaType;

import java.util.List;
import java.util.stream.Collectors;

public class EngineResultResource {

  public static final String WORKFLOW_RESULT_MEDIA_TYPE = VndMediaType.PREFIX + "workflowResult" + VndMediaType.SUFFIX;

  private final Engine engine;
  private final PullRequestService pullRequestService;

  @Inject
  public EngineResultResource(Engine engine, PullRequestService pullRequestService) {
    this.engine = engine;
    this.pullRequestService = pullRequestService;
  }

  @GET
  @Path("")
  @Produces(WORKFLOW_RESULT_MEDIA_TYPE)
  @Operation(
    summary = "Workflow engine result",
    description = "Returns the result of the workflow checks for the given pull request.",
    tags = "Workflow Engine",
    operationId = "review_get_repository_workflow_result"
  )
  @ApiResponse(
    responseCode = "200",
    description = "success",
    content = @Content(
      mediaType = WORKFLOW_RESULT_MEDIA_TYPE,
      schema = @Schema(implementation = ResultListDto.class)
    )
  )
  @ApiResponse(responseCode = "401", description = "not authenticated / invalid credentials")
  @ApiResponse(responseCode = "403", description = "not authorized, the current user does not have the \"repository:readWorkflowConfig\" privilege")
  @ApiResponse(responseCode = "404", description = "either repository or pull request not found")
  @ApiResponse(
    responseCode = "500",
    description = "internal server error",
    content = @Content(
      mediaType = VndMediaType.ERROR_TYPE,
      schema = @Schema(implementation = ErrorDto.class)
    )
  )
  public ResultListDto getResult(@Context UriInfo uriInfo,
                                 @PathParam("namespace") String namespace,
                                 @PathParam("name") String name,
                                 @PathParam("pullRequestId") String pullRequestId) {
    Repository repository = pullRequestService.getRepository(namespace, name);
    PullRequest pullRequest = pullRequestService.get(namespace, name, pullRequestId);

    final Links.Builder linksBuilder = new Links.Builder();
    linksBuilder.self(new PullRequestResourceLinks(uriInfo::getBaseUri).workflowEngineLinks().results(repository.getNamespace(), repository.getName(), pullRequestId));
    List<Result> ruleResults = engine.validate(repository, pullRequest).getRuleResults();
    return new ResultListDto(linksBuilder.build(), ruleResults.stream().map(this::createDto).collect(Collectors.toList()));
  }

  private ResultDto createDto(Result result) {
    return new ResultDto(result.getRule().getSimpleName(), result.isFailed(), result.getContext());
  }
}
