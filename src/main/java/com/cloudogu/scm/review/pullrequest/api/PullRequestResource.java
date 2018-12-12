package com.cloudogu.scm.review.pullrequest.api;

import com.cloudogu.scm.review.comment.api.CommentRootResource;
import com.cloudogu.scm.review.pullrequest.dto.PullRequestDto;
import com.cloudogu.scm.review.pullrequest.dto.PullRequestMapper;
import com.cloudogu.scm.review.pullrequest.service.PullRequestService;
import com.webcohesion.enunciate.metadata.rs.ResponseCode;
import com.webcohesion.enunciate.metadata.rs.StatusCodes;
import com.webcohesion.enunciate.metadata.rs.TypeHint;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryPermissions;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

public class PullRequestResource {

  private final PullRequestMapper mapper;
  private final PullRequestService service;
  private final Provider<CommentRootResource> commentResourceProvider;

  @Inject
  public PullRequestResource(PullRequestMapper mapper, PullRequestService service, Provider<CommentRootResource> commentResourceProvider) {
    this.mapper = mapper;
    this.service = service;
    this.commentResourceProvider = commentResourceProvider;
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
    RepositoryPermissions.read(repository).check();
    return Response.ok(mapper.using(uriInfo).map(service.get(namespace, name, pullRequestId), repository)).build();
  }

  @PUT
  @Path("")
  @Consumes(MediaType.APPLICATION_JSON)
  @StatusCodes({
    @ResponseCode(code = 204, condition = "update success"),
    @ResponseCode(code = 400, condition = "Invalid body"),
    @ResponseCode(code = 401, condition = "not authenticated / invalid credentials"),
    @ResponseCode(code = 403, condition = "not authorized, the current user does not have the privilege to update"),
    @ResponseCode(code = 404, condition = "not found, no comment with the specified id is available"),
    @ResponseCode(code = 500, condition = "internal server error")
  })
  @TypeHint(TypeHint.NO_CONTENT.class)
  public Response update(@Context UriInfo uriInfo,
                         @PathParam("namespace") String namespace,
                         @PathParam("name") String name,
                         @PathParam("pullRequestId") String pullRequestId,
                         PullRequestDto pullRequestDto) {
    Repository repository = service.getRepository(namespace, name);
    RepositoryPermissions.push(repository).check();
    service.update(namespace, name, pullRequestId, pullRequestDto.getTitle(), pullRequestDto.getDescription());
    return Response.noContent().build();
  }
}
