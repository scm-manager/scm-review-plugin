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

import com.cloudogu.scm.review.CurrentUserResolver;
import com.cloudogu.scm.review.PermissionCheck;
import com.cloudogu.scm.review.PullRequestMediaType;
import com.cloudogu.scm.review.PullRequestResourceLinks;
import com.cloudogu.scm.review.comment.api.CommentRootResource;
import com.cloudogu.scm.review.events.ChannelId;
import com.cloudogu.scm.review.pullrequest.dto.PullRequestCheckResultDto;
import com.cloudogu.scm.review.pullrequest.dto.PullRequestChangeDto;
import com.cloudogu.scm.review.pullrequest.dto.PullRequestDto;
import com.cloudogu.scm.review.pullrequest.dto.PullRequestMapper;
import com.cloudogu.scm.review.pullrequest.dto.RejectPullRequestDto;
import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import com.cloudogu.scm.review.pullrequest.service.PullRequestChangeService;
import com.cloudogu.scm.review.pullrequest.service.PullRequestRejectedEvent;
import com.cloudogu.scm.review.pullrequest.service.PullRequestService;
import com.cloudogu.scm.review.workflow.EngineResultResource;
import com.google.common.base.Strings;
import de.otto.edison.hal.HalRepresentation;
import de.otto.edison.hal.Link;
import de.otto.edison.hal.Links;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.constraints.NotEmpty;
import jakarta.ws.rs.QueryParam;
import sonia.scm.api.v2.resources.ErrorDto;
import sonia.scm.repository.NamespaceAndName;
import sonia.scm.repository.Repository;
import sonia.scm.sse.Channel;
import sonia.scm.sse.ChannelRegistry;
import sonia.scm.sse.Registration;
import sonia.scm.sse.SseResponse;
import sonia.scm.web.VndMediaType;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseEventSink;

import java.util.List;

import static de.otto.edison.hal.Links.linkingTo;
import static sonia.scm.ScmConstraintViolationException.Builder.doThrow;

public class PullRequestResource {

  private final PullRequestMapper mapper;
  private final PullRequestService service;
  private final Provider<CommentRootResource> commentResourceProvider;
  private final Provider<EngineResultResource> engineResultResourceProvider;
  private final ChannelRegistry channelRegistry;
  private final PullRequestChangeService pullRequestChangeService;

  @Inject
  public PullRequestResource(PullRequestMapper mapper, PullRequestService service, Provider<CommentRootResource> commentResourceProvider, Provider<EngineResultResource> engineResultResourceProvider, ChannelRegistry channelRegistry, PullRequestChangeService pullRequestChangeService) {
    this.mapper = mapper;
    this.service = service;
    this.commentResourceProvider = commentResourceProvider;
    this.engineResultResourceProvider = engineResultResourceProvider;
    this.channelRegistry = channelRegistry;
    this.pullRequestChangeService = pullRequestChangeService;
  }

  @Path("comments/")
  public CommentRootResource comments() {
    return commentResourceProvider.get();
  }

  @Path("workflow/")
  public EngineResultResource workflowResults() {
    return engineResultResourceProvider.get();
  }

  @GET
  @Path("")
  @Produces(PullRequestMediaType.PULL_REQUEST)
  @Operation(
    summary = "Get pull requests",
    description = "Returns a single pull request by id.",
    tags = "Pull Request",
    operationId = "review_get_pull_requests"
  )
  @ApiResponse(
    responseCode = "200",
    description = "success",
    content = @Content(
      mediaType = PullRequestMediaType.PULL_REQUEST,
      schema = @Schema(implementation = PullRequestDto.class)
    )
  )
  @ApiResponse(responseCode = "401", description = "not authenticated / invalid credentials")
  @ApiResponse(responseCode = "403", description = "not authorized, the current user does not have the \"readPullRequest\" privilege")
  @ApiResponse(
    responseCode = "500",
    description = "internal server error",
    content = @Content(
      mediaType = VndMediaType.ERROR_TYPE,
      schema = @Schema(implementation = ErrorDto.class)
    )
  )
  public PullRequestDto get(@Context UriInfo uriInfo, @PathParam("namespace") String namespace, @PathParam("name") String name, @PathParam("pullRequestId") String pullRequestId) {
    Repository repository = service.getRepository(namespace, name);
    PermissionCheck.checkRead(repository);
    return mapper.using(uriInfo).map(service.get(namespace, name, pullRequestId), repository);
  }

  @GET
  @Path("changes/")
  @Produces(PullRequestMediaType.PULL_REQUEST)
  @Operation(
    summary = "Get history of changes of the pull requests",
    description = "Returns the list of changes of the pull request",
    tags = "Pull Request",
    operationId = "review_get_pull_request_history"
  )
  @ApiResponse(
    responseCode = "200",
    description = "success",
    content = @Content(
      mediaType = MediaType.APPLICATION_JSON,
      schema = @Schema(implementation = HalRepresentation.class)
    )
  )
  @ApiResponse(responseCode = "401", description = "not authenticated / invalid credentials")
  @ApiResponse(responseCode = "403", description = "not authorized, the current user does not have the \"readPullRequest\" privilege")
  @ApiResponse(
    responseCode = "500",
    description = "internal server error",
    content = @Content(
      mediaType = VndMediaType.ERROR_TYPE,
      schema = @Schema(implementation = ErrorDto.class)
    )
  )
  public Response getChanges(@PathParam("namespace") String namespace, @PathParam("name") String name, @PathParam("pullRequestId") String pullRequestId) {
    PermissionCheck.checkRead(service.getRepository(namespace, name));
    List<PullRequestChangeDto> changes = pullRequestChangeService.getAllChangesOfPullRequest(
      new NamespaceAndName(namespace, name), pullRequestId
    ).stream().map(PullRequestChangeDto::mapToDto).toList();
    return Response.ok(changes).build();
  }

  @POST
  @Path("approve")
  @Operation(summary = "Approve pull request", description = "Approves a pull request by id.", tags = "Pull Request")
  @ApiResponse(responseCode = "204", description = "update success")
  @ApiResponse(responseCode = "401", description = "not authenticated / invalid credentials")
  @ApiResponse(responseCode = "403", description = "not authorized, the current user does not have the \"commentPullRequest\" privilege")
  @ApiResponse(responseCode = "404", description = "not found, no pull request with the specified id is available")
  @ApiResponse(
    responseCode = "500",
    description = "internal server error",
    content = @Content(
      mediaType = VndMediaType.ERROR_TYPE,
      schema = @Schema(implementation = ErrorDto.class)
    )
  )
  public void approve(@Context UriInfo uriInfo, @PathParam("namespace") String namespace, @PathParam("name") String name, @PathParam("pullRequestId") String pullRequestId) {
    service.approve(new NamespaceAndName(namespace, name), pullRequestId);
  }

  @POST
  @Path("disapprove")
  @Operation(summary = "Disapprove pull request", description = "Disapproves an already approved pull request by id.", tags = "Pull Request")
  @ApiResponse(responseCode = "204", description = "update success")
  @ApiResponse(responseCode = "401", description = "not authenticated / invalid credentials")
  @ApiResponse(responseCode = "403", description = "not authorized, the current user does not have the \"commentPullRequest\" privilege")
  @ApiResponse(responseCode = "404", description = "not found, no pull request with the specified id is available")
  @ApiResponse(
    responseCode = "500",
    description = "internal server error",
    content = @Content(
      mediaType = VndMediaType.ERROR_TYPE,
      schema = @Schema(implementation = ErrorDto.class)
    )
  )
  public void disapprove(@Context UriInfo uriInfo,
                         @PathParam("namespace") String namespace,
                         @PathParam("name") String name,
                         @PathParam("pullRequestId") String pullRequestId) {
    service.disapprove(new NamespaceAndName(namespace, name), pullRequestId);
  }

  @GET
  @Path("subscription")
  @Produces(PullRequestMediaType.PULL_REQUEST)
  @Operation(summary = "Evaluates which subscription link should be used", hidden = true)
  @ApiResponse(responseCode = "200", description = "success")
  @ApiResponse(responseCode = "401", description = "not authenticated / invalid credentials")
  @ApiResponse(responseCode = "403", description = "not authorized, the current user does not have the \"readPullRequest\" privilege")
  @ApiResponse(
    responseCode = "500",
    description = "internal server error",
    content = @Content(
      mediaType = VndMediaType.ERROR_TYPE,
      schema = @Schema(implementation = ErrorDto.class)
    )
  )
  public Response getSubscription(
    @Context UriInfo uriInfo,
    @PathParam("namespace") String namespace,
    @PathParam("name") String name,
    @PathParam("pullRequestId") String pullRequestId
  ) {
    Repository repository = service.getRepository(namespace, name);
    PermissionCheck.checkRead(repository);
    if (CurrentUserResolver.getCurrentUser() != null && Strings.isNullOrEmpty(CurrentUserResolver.getCurrentUser().getMail())) {
      return Response.ok().build();
    }
    if (service.isUserSubscribed(repository, pullRequestId)) {
      PullRequestResourceLinks resourceLinks = new PullRequestResourceLinks(uriInfo::getBaseUri);

      String unsubscribe = resourceLinks.pullRequest().unsubscribe(namespace, name, pullRequestId);

      Links.Builder linksBuilder = linkingTo().single(Link.link("unsubscribe", unsubscribe));
      return Response.ok(new HalRepresentation(linksBuilder.build())).build();
    } else {
      PullRequestResourceLinks resourceLinks = new PullRequestResourceLinks(uriInfo::getBaseUri);

      String subscribe = resourceLinks.pullRequest().subscribe(namespace, name, pullRequestId);

      Links.Builder linksBuilder = linkingTo().single(Link.link("subscribe", subscribe));
      return Response.ok(new HalRepresentation(linksBuilder.build())).build();
    }
  }

  @POST
  @Path("subscribe")
  @Operation(summary = "Subscribe", description = "Subscribes current user to a pull request.", tags = "Pull Request")
  @ApiResponse(responseCode = "204", description = "update success")
  @ApiResponse(responseCode = "401", description = "not authenticated / invalid credentials")
  @ApiResponse(responseCode = "403", description = "not authorized, the current user does not have the \"modifyPullRequest\" privilege")
  @ApiResponse(
    responseCode = "500",
    description = "internal server error",
    content = @Content(
      mediaType = VndMediaType.ERROR_TYPE,
      schema = @Schema(implementation = ErrorDto.class)
    )
  )
  public void subscribe(
    @Context UriInfo uriInfo,
    @PathParam("namespace") String namespace,
    @PathParam("name") String name,
    @PathParam("pullRequestId") String pullRequestId
  ) {
    Repository repository = service.getRepository(namespace, name);
    PermissionCheck.checkRead(repository);
    service.subscribe(repository, pullRequestId);
  }

  @POST
  @Path("unsubscribe")
  @Operation(summary = "Unsubscribe", description = "Unsubscribes current user from a pull request.", tags = "Pull Request")
  @ApiResponse(responseCode = "204", description = "update success")
  @ApiResponse(responseCode = "401", description = "not authenticated / invalid credentials")
  @ApiResponse(responseCode = "403", description = "not authorized, the current user does not have the \"modifyPullRequest\" privilege")
  @ApiResponse(
    responseCode = "500",
    description = "internal server error",
    content = @Content(
      mediaType = VndMediaType.ERROR_TYPE,
      schema = @Schema(implementation = ErrorDto.class)
    )
  )
  public void unsubscribe(@Context UriInfo uriInfo,
                          @PathParam("namespace") String namespace,
                          @PathParam("name") String name,
                          @PathParam("pullRequestId") String pullRequestId) {
    Repository repository = service.getRepository(namespace, name);
    PermissionCheck.checkRead(repository);
    service.unsubscribe(repository, pullRequestId);
  }

  @POST
  @Path("review-mark/{path: .*}")
  @Operation(summary = "Mark as reviewed", description = "Marks a diff in a pull request as reviewed.", tags = "Pull Request")
  @ApiResponse(responseCode = "204", description = "update success")
  @ApiResponse(responseCode = "401", description = "not authenticated / invalid credentials")
  @ApiResponse(responseCode = "403", description = "not authorized, the current user does not have the \"modifyPullRequest\" privilege")
  @ApiResponse(responseCode = "404", description = "not found, no pull request with the specified id is available")
  @ApiResponse(
    responseCode = "500",
    description = "internal server error",
    content = @Content(
      mediaType = VndMediaType.ERROR_TYPE,
      schema = @Schema(implementation = ErrorDto.class)
    )
  )
  public void markAsReviewed(@Context UriInfo uriInfo,
                             @PathParam("namespace") String namespace,
                             @PathParam("name") String name,
                             @PathParam("pullRequestId") String pullRequestId,
                             @PathParam("path") String path) {
    Repository repository = service.getRepository(namespace, name);
    service.markAsReviewed(repository, pullRequestId, path);
  }

  @DELETE
  @Path("review-mark/{path: .*}")
  @Operation(summary = "Unmark as reviewed", description = "Unmarks a diff in a pull request which was before marked as reviewed.", tags = "Pull Request")
  @ApiResponse(responseCode = "204", description = "update success")
  @ApiResponse(responseCode = "401", description = "not authenticated / invalid credentials")
  @ApiResponse(responseCode = "403", description = "not authorized, the current user does not have the \"modifyPullRequest\" privilege")
  @ApiResponse(responseCode = "404", description = "not found, no pull request with the specified id is available")
  @ApiResponse(
    responseCode = "500",
    description = "internal server error",
    content = @Content(
      mediaType = VndMediaType.ERROR_TYPE,
      schema = @Schema(implementation = ErrorDto.class)
    )
  )
  public void markAsNotReviewed(@Context UriInfo uriInfo,
                                @PathParam("namespace") String namespace,
                                @PathParam("name") String name,
                                @PathParam("pullRequestId") String pullRequestId,
                                @PathParam("path") String path) {
    Repository repository = service.getRepository(namespace, name);
    service.markAsNotReviewed(repository, pullRequestId, path);
  }

  @PUT
  @Path("")
  @Consumes(PullRequestMediaType.PULL_REQUEST)
  @Operation(
    summary = "Update pull request",
    description = "Modifies a pull request.",
    tags = "Pull Request",
    operationId = "review_put_pull_request"
  )
  @ApiResponse(responseCode = "204", description = "update success")
  @ApiResponse(responseCode = "400", description = "Invalid body")
  @ApiResponse(responseCode = "401", description = "not authenticated / invalid credentials")
  @ApiResponse(responseCode = "403", description = "not authorized, the current user does not have the \"modifyPullRequest\" privilege")
  @ApiResponse(responseCode = "404", description = "not found, no pull request with the specified id is available")
  @ApiResponse(
    responseCode = "500",
    description = "internal server error",
    content = @Content(
      mediaType = VndMediaType.ERROR_TYPE,
      schema = @Schema(implementation = ErrorDto.class)
    )
  )
  public Response update(@Context UriInfo uriInfo,
                         @PathParam("namespace") String namespace,
                         @PathParam("name") String name,
                         @PathParam("pullRequestId") String pullRequestId,
                         PullRequestDto pullRequestDto) {
    Repository repository = service.getRepository(namespace, name);
    if (!PermissionCheck.mayModifyPullRequest(repository, service.get(namespace, name, pullRequestId))) {
      return Response.status(Response.Status.FORBIDDEN).build();
    }

    doThrow().violation("illegal status", "pullRequest", "status")
      .when(service.get(repository, pullRequestId).isClosed());

    PullRequest pullRequest = mapper.map(pullRequestDto);

    service.update(repository, pullRequestId, pullRequest);
    return Response.noContent().build();
  }

  @POST
  @Path("reject")
  @Operation(summary = "Reject pull request", description = "Rejects a pull request.", tags = "Pull Request")
  @ApiResponse(responseCode = "204", description = "update success")
  @ApiResponse(responseCode = "401", description = "not authenticated / invalid credentials")
  @ApiResponse(responseCode = "403", description = "not authorized, the current user does not have the \"modifyPullRequest\" privilege")
  @ApiResponse(responseCode = "404", description = "not found, no pull request with the specified id is available")
  @ApiResponse(
    responseCode = "500",
    description = "internal server error",
    content = @Content(
      mediaType = VndMediaType.ERROR_TYPE,
      schema = @Schema(implementation = ErrorDto.class)
    )
  )
  public void reject(@PathParam("namespace") String namespace, @PathParam("name") String name, @PathParam("pullRequestId") String pullRequestId) {
    Repository repository = service.getRepository(namespace, name);
    service.reject(repository, pullRequestId, PullRequestRejectedEvent.RejectionCause.REJECTED_BY_USER);
  }

  @POST
  @Path("rejectWithMessage")
  @Operation(summary = "Reject pull request", description = "Rejects a pull request.", tags = "Pull Request")
  @ApiResponse(responseCode = "204", description = "update success")
  @ApiResponse(responseCode = "401", description = "not authenticated / invalid credentials")
  @ApiResponse(responseCode = "403", description = "not authorized, the current user does not have the \"modifyPullRequest\" privilege")
  @ApiResponse(responseCode = "404", description = "not found, no pull request with the specified id is available")
  @ApiResponse(
    responseCode = "500",
    description = "internal server error",
    content = @Content(
      mediaType = VndMediaType.ERROR_TYPE,
      schema = @Schema(implementation = ErrorDto.class)
    )
  )
  public void rejectWithMessage(@PathParam("namespace") String namespace, @PathParam("name") String name, @PathParam("pullRequestId") String pullRequestId, RejectPullRequestDto reject) {
    Repository repository = service.getRepository(namespace, name);
    service.reject(repository, pullRequestId, reject.getMessage());
  }

  @POST
  @Path("convert-to-pr")
  @Operation(summary = "Convert draft to pull request", description = "Converts a draft pull request to a pull request.", tags = "Pull Request")
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
  public void convertToPR(@PathParam("namespace") String namespace, @PathParam("name") String name, @PathParam("pullRequestId") String pullRequestId) {
    Repository repository = service.getRepository(namespace, name);
    service.convertToPR(repository, pullRequestId);
  }

  @POST
  @Path("reopen")
  @Operation(summary = "Reopen pull request", description = "Reopens a rejected pull request.", tags = "Pull Request")
  @ApiResponse(responseCode = "204", description = "update success")
  @ApiResponse(responseCode = "401", description = "not authenticated / invalid credentials")
  @ApiResponse(responseCode = "403", description = "not authorized, the current user does not have the \"modifyPullRequest\" or the \"createPullRequest\" privilege")
  @ApiResponse(responseCode = "404", description = "not found, no pull request with the specified id is available")
  @ApiResponse(
    responseCode = "500",
    description = "internal server error",
    content = @Content(
      mediaType = VndMediaType.ERROR_TYPE,
      schema = @Schema(implementation = ErrorDto.class)
    )
  )
  public void reopen(@PathParam("namespace") String namespace, @PathParam("name") String name, @PathParam("pullRequestId") String pullRequestId) {
    Repository repository = service.getRepository(namespace, name);
    service.reopen(repository, pullRequestId);
  }

  @GET
  @Path("events")
  @SseResponse
  @Operation(summary = "Register SSE", hidden = true)
  @Produces(MediaType.SERVER_SENT_EVENTS)
  public void events(@Context Sse sse, @Context SseEventSink eventSink, @BeanParam EventSubscriptionRequest request) {
    Repository repository = service.getRepository(request.getNamespace(), request.getName());
    PermissionCheck.checkRead(repository);
    PullRequest pullRequest = service.get(repository, request.getPullRequestId());

    Channel channel = channelRegistry.channel(new ChannelId(repository, pullRequest));
    channel.register(new Registration(
      request.getSessionId(),
      sse,
      eventSink
    ));
  }

  @GET
  @Path("check")
  @Produces(PullRequestMediaType.PULL_REQUEST_CHECK_RESULT)
  @Operation(
    summary = "Checks new target for existing pull request",
    description = "Checks if the target branch of a pull request can be changed to the given new branch.",
    tags = "Pull Request",
    operationId = "review_check_new_target_branch"
  )
  @ApiResponse(responseCode = "200", description = "Returns pull request check result")
  @ApiResponse(responseCode = "400", description = "Invalid request / the provided source branch or target branch may not exist")
  @ApiResponse(responseCode = "401", description = "not authenticated / invalid credentials")
  @ApiResponse(responseCode = "403", description = "not authorized, the current user does not have the \"createPullRequest\" privilege")
  @ApiResponse(
    responseCode = "500",
    description = "internal server error",
    content = @Content(
      mediaType = VndMediaType.ERROR_TYPE,
      schema = @Schema(implementation = ErrorDto.class)
    )
  )
  public PullRequestCheckResultDto check(
    @Context UriInfo uriInfo,
    @PathParam("namespace") String namespace,
    @PathParam("name") String name,
    @PathParam("pullRequestId") String pullRequestId,
    @NotEmpty @QueryParam("target") String target
  ) {
    Repository repository = service.getRepository(namespace, name);
    PermissionCheck.checkCreate(repository);

    service.checkBranch(repository, target);

    PullRequest pullRequest = service.get(repository, pullRequestId);

    return service.checkIfPullRequestIsValid(repository, pullRequest, target)
      .create(createCheckResultLinks(uriInfo, repository, pullRequest, target));
  }

  private Links createCheckResultLinks(UriInfo uriInfo, Repository repository, PullRequest pullRequest, String target) {
    PullRequestResourceLinks pullRequestResourceLinks = new PullRequestResourceLinks(uriInfo::getBaseUri);
    String checkLink = String.format(
      "%s?target=%s",
      pullRequestResourceLinks.pullRequest().check(repository.getNamespace(), repository.getName(), pullRequest.getId()), target
    );

    return Links.linkingTo().self(checkLink).build();
  }
}
