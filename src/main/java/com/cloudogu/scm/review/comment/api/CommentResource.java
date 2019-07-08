package com.cloudogu.scm.review.comment.api;


import com.cloudogu.scm.review.RepositoryResolver;
import com.cloudogu.scm.review.comment.service.CommentService;
import com.cloudogu.scm.review.comment.service.PullRequestRootComment;
import com.webcohesion.enunciate.metadata.rs.ResponseCode;
import com.webcohesion.enunciate.metadata.rs.StatusCodes;
import com.webcohesion.enunciate.metadata.rs.TypeHint;
import sonia.scm.NotFoundException;
import sonia.scm.repository.NamespaceAndName;
import sonia.scm.repository.Repository;

import javax.inject.Inject;
import javax.validation.Valid;
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

import static java.net.URI.create;

public class CommentResource {

  private final CommentService service;
  private RepositoryResolver repositoryResolver;
  private final PullRequestCommentMapper commentMapper;
  private final ReplyMapper replyMapper;
  private final CommentPathBuilder commentPathBuilder;

  @Inject
  public CommentResource(CommentService service, RepositoryResolver repositoryResolver, PullRequestCommentMapper commentMapper, ReplyMapper replyMapper, CommentPathBuilder commentPathBuilder) {
    this.repositoryResolver = repositoryResolver;
    this.service = service;
    this.commentMapper = commentMapper;
    this.replyMapper = replyMapper;
    this.commentPathBuilder = commentPathBuilder;
  }

  @GET
  @Path("")
  @Produces(MediaType.APPLICATION_JSON)
  public Response get(@PathParam("namespace") String namespace,
                      @PathParam("name") String name,
                      @PathParam("pullRequestId") String pullRequestId,
                      @PathParam("commentId") String commentId) {
    Repository repository = repositoryResolver.resolve(new NamespaceAndName(namespace, name));
    PullRequestRootComment comment = service.get(namespace, name, pullRequestId, commentId);
    return Response.ok(commentMapper.map(comment, repository, pullRequestId)).build();
  }

  @DELETE
  @Path("")
  @StatusCodes({
    @ResponseCode(code = 204, condition = "delete success or nothing to delete"),
    @ResponseCode(code = 401, condition = "not authenticated / invalid credentials"),
    @ResponseCode(code = 403, condition = "not authorized, the current user is not allowed to delete the comment"),
    @ResponseCode(code = 500, condition = "internal server error")
  })
  @TypeHint(TypeHint.NO_CONTENT.class)
  public Response delete(@Context UriInfo uriInfo,
                         @PathParam("namespace") String namespace,
                         @PathParam("name") String name,
                         @PathParam("pullRequestId") String pullRequestId,
                         @PathParam("commentId") String commentId) {
    try {
      service.delete(namespace, name, pullRequestId, commentId);
      return Response.noContent().build();
    } catch (NotFoundException e) {
      return Response.noContent().build();
    }
  }

  @PUT
  @Path("")
  @Consumes(MediaType.APPLICATION_JSON)
  @StatusCodes({
    @ResponseCode(code = 204, condition = "update success"),
    @ResponseCode(code = 400, condition = "Invalid body, e.g. illegal change of namespace or name"),
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
                         @PathParam("commentId") String commentId,
                         @Valid PullRequestCommentDto pullRequestCommentDto) {
    service.modify(namespace, name, pullRequestId, commentId, commentMapper.map(pullRequestCommentDto));
    return Response.noContent().build();
  }

  @POST
  @Path("reply")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response reply(@Context UriInfo uriInfo,
                        @PathParam("namespace") String namespace,
                        @PathParam("name") String name,
                        @PathParam("pullRequestId") String pullRequestId,
                        @PathParam("commentId") String commentId,
                        @Valid ReplyDto replyDto) {
    String newId = service.reply(namespace, name, pullRequestId, commentId, replyMapper.map(replyDto));
    String newLocation = commentPathBuilder.createCommentSelfUri(namespace, name, pullRequestId, newId);
    return Response.created(create(newLocation)).build();
  }
}
