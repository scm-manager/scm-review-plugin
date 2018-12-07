package com.cloudogu.scm.review.comment.api;


import com.cloudogu.scm.review.comment.dto.PullRequestCommentDto;
import com.cloudogu.scm.review.comment.service.CommentService;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

public class CommentResource {


  private final CommentService service;

  @Inject
  public CommentResource(CommentService service){
    this.service = service;
  }

  @GET
  @Path("")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response get(@Context UriInfo uriInfo, @PathParam("namespace") String namespace, @PathParam("name") String name, @PathParam("pullRequestId") String pullRequestId, @PathParam("commentId") String commentId) {
    return Response.ok().build();
  }

  @DELETE
  @Path("")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response delete(@Context UriInfo uriInfo, @PathParam("namespace") String namespace, @PathParam("name") String name, @PathParam("pullRequestId") String pullRequestId, @PathParam("commentId") String commentId) {
    return Response.ok().build();
  }

  @PUT
  @Path("")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response update(@Context UriInfo uriInfo,
                         @PathParam("namespace") String namespace,
                         @PathParam("name") String name,
                         @PathParam("pullRequestId") String pullRequestId,
                         @PathParam("commentId") String commentId,
                         PullRequestCommentDto pullRequestCommentDto) {
    return Response.ok().build();
  }
}
