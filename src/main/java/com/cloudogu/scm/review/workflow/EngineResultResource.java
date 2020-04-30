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

package com.cloudogu.scm.review.workflow;

import com.cloudogu.scm.review.PullRequestResourceLinks;
import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import com.cloudogu.scm.review.pullrequest.service.PullRequestService;
import de.otto.edison.hal.Links;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import sonia.scm.api.v2.resources.ErrorDto;
import sonia.scm.repository.Repository;
import sonia.scm.web.VndMediaType;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import java.util.List;
import java.util.stream.Collectors;

@OpenAPIDefinition(tags = {
  @Tag(name = "Workflow Engine", description = "Workflow engine related endpoints provided by review-plugin")
})
@Path("")
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
