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
import com.webcohesion.enunciate.metadata.rs.ResponseCode;
import com.webcohesion.enunciate.metadata.rs.StatusCodes;
import com.webcohesion.enunciate.metadata.rs.TypeHint;
import de.otto.edison.hal.HalRepresentation;
import de.otto.edison.hal.Link;
import de.otto.edison.hal.Links;
import sonia.scm.repository.NamespaceAndName;
import sonia.scm.repository.Repository;

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
  public PullRequestDto get(@Context UriInfo uriInfo, @PathParam("namespace") String namespace, @PathParam("name") String name, @PathParam("pullRequestId") String pullRequestId) {
    Repository repository = service.getRepository(namespace, name);
    PermissionCheck.checkRead(repository);
    return mapper.using(uriInfo).map(service.get(namespace, name, pullRequestId), repository);
  }

  @POST
  @Path("approve")
  @StatusCodes({
    @ResponseCode(code = 204, condition = "update success"),
    @ResponseCode(code = 400, condition = "Invalid body"),
    @ResponseCode(code = 401, condition = "not authenticated / invalid credentials"),
    @ResponseCode(code = 403, condition = "not authorized, the current user does not have the privilege to update"),
    @ResponseCode(code = 404, condition = "not found, no pull request with the specified id is available"),
    @ResponseCode(code = 500, condition = "internal server error")
  })
  @TypeHint(TypeHint.NO_CONTENT.class)
  public void approve(@Context UriInfo uriInfo, @PathParam("namespace") String namespace, @PathParam("name") String name, @PathParam("pullRequestId") String pullRequestId) {
    service.approve(new NamespaceAndName(namespace, name), pullRequestId);
  }

  @POST
  @Path("disapprove")
  @StatusCodes({
    @ResponseCode(code = 204, condition = "update success"),
    @ResponseCode(code = 400, condition = "Invalid body"),
    @ResponseCode(code = 401, condition = "not authenticated / invalid credentials"),
    @ResponseCode(code = 403, condition = "not authorized, the current user does not have the privilege to update"),
    @ResponseCode(code = 404, condition = "not found, no pull request with the specified id is available"),
    @ResponseCode(code = 500, condition = "internal server error")
  })
  @TypeHint(TypeHint.NO_CONTENT.class)
  public void disapprove(@Context UriInfo uriInfo, @PathParam("namespace") String namespace, @PathParam("name") String name, @PathParam("pullRequestId") String pullRequestId) {
    service.disapprove(new NamespaceAndName(namespace, name), pullRequestId);
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
  @Produces(PullRequestMediaType.PULL_REQUEST)
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
  public void subscribe(@Context UriInfo uriInfo, @PathParam("namespace") String namespace, @PathParam("name") String name, @PathParam("pullRequestId") String pullRequestId) {
    Repository repository = service.getRepository(namespace, name);
    PermissionCheck.checkRead(repository);
    service.subscribe(repository, pullRequestId);
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
  public void unsubscribe(@Context UriInfo uriInfo, @PathParam("namespace") String namespace, @PathParam("name") String name, @PathParam("pullRequestId") String pullRequestId) {
    Repository repository = service.getRepository(namespace, name);
    PermissionCheck.checkRead(repository);
    service.unsubscribe(repository, pullRequestId);
  }

  @POST
  @Path("review-mark/{path: .*}")
  @StatusCodes({
    @ResponseCode(code = 204, condition = "update success"),
    @ResponseCode(code = 401, condition = "not authenticated / invalid credentials"),
    @ResponseCode(code = 403, condition = "not authorized, the current user does not have the privilege to update"),
    @ResponseCode(code = 404, condition = "not found, no pull request with the specified id is available"),
    @ResponseCode(code = 500, condition = "internal server error")
  })
  @TypeHint(TypeHint.NO_CONTENT.class)
  public void markAsReviewed(@Context UriInfo uriInfo, @PathParam("namespace") String namespace, @PathParam("name") String name, @PathParam("pullRequestId") String pullRequestId, @PathParam("path") String path) {
    Repository repository = service.getRepository(namespace, name);
    service.markAsReviewed(repository, pullRequestId, path);
  }

  @DELETE
  @Path("review-mark/{path: .*}")
  @StatusCodes({
    @ResponseCode(code = 204, condition = "update success"),
    @ResponseCode(code = 401, condition = "not authenticated / invalid credentials"),
    @ResponseCode(code = 403, condition = "not authorized, the current user does not have the privilege to update"),
    @ResponseCode(code = 404, condition = "not found, no pull request with the specified id is available"),
    @ResponseCode(code = 500, condition = "internal server error")
  })
  @TypeHint(TypeHint.NO_CONTENT.class)
  public void markAsNotReviewed(@Context UriInfo uriInfo, @PathParam("namespace") String namespace, @PathParam("name") String name, @PathParam("pullRequestId") String pullRequestId, @PathParam("path") String path) {
    Repository repository = service.getRepository(namespace, name);
    service.markAsNotReviewed(repository, pullRequestId, path);
  }

  @PUT
  @Path("")
  @Consumes(PullRequestMediaType.PULL_REQUEST)
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
    PullRequest pullRequest = mapper.map(pullRequestDto);
    service.update(repository, pullRequestId, pullRequest);
    return Response.noContent().build();
  }

  @POST
  @Path("reject")
  public void reject(@PathParam("namespace") String namespace, @PathParam("name") String name, @PathParam("pullRequestId") String pullRequestId) {
    Repository repository = service.getRepository(namespace, name);
    service.reject(repository, pullRequestId, PullRequestRejectedEvent.RejectionCause.REJECTED_BY_USER);
  }

  @GET
  @Path("events")
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
