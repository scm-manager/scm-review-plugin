package com.cloudogu.scm.review.pullrequest.api;

import com.cloudogu.scm.review.CurrentUserResolver;
import com.cloudogu.scm.review.PermissionCheck;
import com.cloudogu.scm.review.PullRequestResourceLinks;
import com.cloudogu.scm.review.comment.api.CommentRootResource;
import com.cloudogu.scm.review.comment.service.CommentService;
import com.cloudogu.scm.review.comment.service.SystemCommentType;
import com.cloudogu.scm.review.pullrequest.dto.PullRequestDto;
import com.cloudogu.scm.review.pullrequest.dto.PullRequestMapper;
import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import com.cloudogu.scm.review.pullrequest.service.PullRequestRejectedEvent;
import com.cloudogu.scm.review.pullrequest.service.PullRequestService;
import com.google.common.base.Strings;
import com.webcohesion.enunciate.metadata.rs.ResponseCode;
import com.webcohesion.enunciate.metadata.rs.StatusCodes;
import com.webcohesion.enunciate.metadata.rs.TypeHint;
import de.otto.edison.hal.HalRepresentation;
import de.otto.edison.hal.Link;
import de.otto.edison.hal.Links;
import sonia.scm.event.ScmEventBus;
import sonia.scm.repository.Repository;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.ws.rs.Consumes;
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

import static de.otto.edison.hal.Links.linkingTo;

public class PullRequestResource {

  private final PullRequestMapper mapper;
  private final PullRequestService service;
  private final Provider<CommentRootResource> commentResourceProvider;
  private final CommentService commentService;
  private final ScmEventBus eventBus;


  @Inject
  public PullRequestResource(PullRequestMapper mapper, PullRequestService service, Provider<CommentRootResource> commentResourceProvider, CommentService commentService, ScmEventBus eventBus) {
    this.mapper = mapper;
    this.service = service;
    this.commentResourceProvider = commentResourceProvider;
    this.commentService = commentService;
    this.eventBus = eventBus;
  }

  @Path("comments/")
  public CommentRootResource comments() {
    return commentResourceProvider.get();
  }

  @GET
  @Path("")
  @Produces(MediaType.APPLICATION_JSON)
  public Response get(@Context UriInfo uriInfo, @PathParam("namespace") String namespace, @PathParam("name") String name, @PathParam("pullRequestId") String pullRequestId) {
    Repository repository = service.getRepository(namespace, name);
    PermissionCheck.checkRead(repository);
    return Response.ok(mapper.using(uriInfo).map(service.get(namespace, name, pullRequestId), repository)).build();
  }

  @GET
  @Path("subscription")
  @StatusCodes({
    @ResponseCode(code = 200, condition = "success"),
    @ResponseCode(code = 400, condition = "Invalid body"),
    @ResponseCode(code = 401, condition = "not authenticated / invalid credentials"),
    @ResponseCode(code = 403, condition = "not authorized, the current user does not have the privilege to update"),
    @ResponseCode(code = 404, condition = "not found, no pull request with the specified id is available"),
    @ResponseCode(code = 500, condition = "internal server error")
  })
  @Produces(MediaType.APPLICATION_JSON)
  public Response getSubscription(@Context UriInfo uriInfo, @PathParam("namespace") String namespace, @PathParam("name") String name, @PathParam("pullRequestId") String pullRequestId) {
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
  @StatusCodes({
    @ResponseCode(code = 204, condition = "update success"),
    @ResponseCode(code = 400, condition = "Invalid body"),
    @ResponseCode(code = 401, condition = "not authenticated / invalid credentials"),
    @ResponseCode(code = 403, condition = "not authorized, the current user does not have the privilege to update"),
    @ResponseCode(code = 404, condition = "not found, no pull request with the specified id is available"),
    @ResponseCode(code = 500, condition = "internal server error")
  })
  @TypeHint(TypeHint.NO_CONTENT.class)
  public Response subscribe(@Context UriInfo uriInfo, @PathParam("namespace") String namespace, @PathParam("name") String name, @PathParam("pullRequestId") String pullRequestId) {
    Repository repository = service.getRepository(namespace, name);
    PermissionCheck.checkRead(repository);
    service.subscribe(repository, pullRequestId);
    return Response.accepted().build();
  }

  @POST
  @Path("unsubscribe")
  @StatusCodes({
    @ResponseCode(code = 204, condition = "update success"),
    @ResponseCode(code = 400, condition = "Invalid body"),
    @ResponseCode(code = 401, condition = "not authenticated / invalid credentials"),
    @ResponseCode(code = 403, condition = "not authorized, the current user does not have the privilege to update"),
    @ResponseCode(code = 404, condition = "not found, no pull request with the specified id is available"),
    @ResponseCode(code = 500, condition = "internal server error")
  })
  @TypeHint(TypeHint.NO_CONTENT.class)
  public Response unsubscribe(@Context UriInfo uriInfo, @PathParam("namespace") String namespace, @PathParam("name") String name, @PathParam("pullRequestId") String pullRequestId) {
    Repository repository = service.getRepository(namespace, name);
    PermissionCheck.checkRead(repository);
    service.unsubscribe(repository, pullRequestId);
    return Response.accepted().build();
  }

  @PUT
  @Path("")
  @Consumes(MediaType.APPLICATION_JSON)
  @StatusCodes({
    @ResponseCode(code = 204, condition = "update success"),
    @ResponseCode(code = 400, condition = "Invalid body"),
    @ResponseCode(code = 401, condition = "not authenticated / invalid credentials"),
    @ResponseCode(code = 403, condition = "not authorized, the current user does not have the privilege to update"),
    @ResponseCode(code = 404, condition = "not found, no pull request with the specified id is available"),
    @ResponseCode(code = 500, condition = "internal server error")
  })
  @TypeHint(TypeHint.NO_CONTENT.class)
  public Response update(@Context UriInfo uriInfo,
                         @PathParam("namespace") String namespace,
                         @PathParam("name") String name,
                         @PathParam("pullRequestId") String pullRequestId,
                         PullRequestDto pullRequestDto) {
    Repository repository = service.getRepository(namespace, name);
    if (!PermissionCheck.mayModifyPullRequest(repository, service.get(namespace, name, pullRequestId))) {
      return Response.status(Response.Status.FORBIDDEN).build();
    }
    service.update(repository, pullRequestId, pullRequestDto.getTitle(), pullRequestDto.getDescription());
    return Response.noContent().build();
  }

  @POST
  @Path("reject")
  public Response reject(@PathParam("namespace") String namespace, @PathParam("name") String name, @PathParam("pullRequestId") String pullRequestId) {
    Repository repository = service.getRepository(namespace, name);
    PullRequest pullRequest = service.get(repository, pullRequestId);
    service.reject(repository, pullRequest);
    commentService.addStatusChangedComment(repository, pullRequestId, SystemCommentType.REJECTED);
    eventBus.post(new PullRequestRejectedEvent(repository, pullRequest));
    return Response.noContent().build();
  }
}
