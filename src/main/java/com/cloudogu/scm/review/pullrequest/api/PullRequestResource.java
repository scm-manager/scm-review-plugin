/**
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
package com.cloudogu.scm.review.pullrequest.api;

import com.cloudogu.scm.review.CurrentUserResolver;
import com.cloudogu.scm.review.PermissionCheck;
import com.cloudogu.scm.review.PullRequestMediaType;
import com.cloudogu.scm.review.PullRequestResourceLinks;
import com.cloudogu.scm.review.comment.api.CommentRootResource;
import com.cloudogu.scm.review.events.Channel;
import com.cloudogu.scm.review.events.ChannelRegistry;
import com.cloudogu.scm.review.events.Registration;
import com.cloudogu.scm.review.pullrequest.dto.PullRequestDto;
import com.cloudogu.scm.review.pullrequest.dto.PullRequestMapper;
import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import com.cloudogu.scm.review.pullrequest.service.PullRequestRejectedEvent;
import com.cloudogu.scm.review.pullrequest.service.PullRequestService;
import com.google.common.base.Strings;
import de.otto.edison.hal.HalRepresentation;
import de.otto.edison.hal.Link;
import de.otto.edison.hal.Links;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import sonia.scm.api.v2.resources.ErrorDto;
import sonia.scm.repository.NamespaceAndName;
import sonia.scm.repository.Repository;
import sonia.scm.web.VndMediaType;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.sse.Sse;
import javax.ws.rs.sse.SseEventSink;

import static de.otto.edison.hal.Links.linkingTo;

public class PullRequestResource {

  private final PullRequestMapper mapper;
  private final PullRequestService service;
  private final Provider<CommentRootResource> commentResourceProvider;
  private final ChannelRegistry channelRegistry;

  @Inject
  public PullRequestResource(PullRequestMapper mapper, PullRequestService service, Provider<CommentRootResource> commentResourceProvider, ChannelRegistry channelRegistry) {
    this.mapper = mapper;
    this.service = service;
    this.commentResourceProvider = commentResourceProvider;
    this.channelRegistry = channelRegistry;
  }

  @Path("comments/")
  public CommentRootResource comments() {
    return commentResourceProvider.get();
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

  @GET
  @Path("events")
  @Operation(summary = "Register SSE", hidden = true)
  @Produces(MediaType.SERVER_SENT_EVENTS)
  public void events(@Context Sse sse, @Context SseEventSink eventSink, @BeanParam EventSubscriptionRequest request) {
    Repository repository = service.getRepository(request.getNamespace(), request.getName());
    PermissionCheck.checkRead(repository);
    PullRequest pullRequest = service.get(repository, request.getPullRequestId());

    Channel channel = channelRegistry.channel(repository, pullRequest);
    channel.register(new Registration(
      sse,
      eventSink,
      request.getSessionId()
    ));
  }
}
