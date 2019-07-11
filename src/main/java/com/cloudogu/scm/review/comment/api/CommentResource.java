package com.cloudogu.scm.review.comment.api;


import com.cloudogu.scm.review.RepositoryResolver;
import com.cloudogu.scm.review.comment.service.CommentService;
import com.cloudogu.scm.review.comment.service.Comment;
import com.cloudogu.scm.review.comment.service.CommentTransition;
import com.cloudogu.scm.review.comment.service.Reply;
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

import java.util.Collection;
import java.util.List;

import static com.cloudogu.scm.review.HalRepresentations.createCollection;
import static java.net.URI.create;
import static java.util.stream.Collectors.toList;

public class CommentResource {

  private final CommentService service;
  private RepositoryResolver repositoryResolver;
  private final CommentMapper commentMapper;
  private final ReplyMapper replyMapper;
  private final CommentPathBuilder commentPathBuilder;
  private final TransitionMapper transitionMapper;

  @Inject
  public CommentResource(CommentService service, RepositoryResolver repositoryResolver, CommentMapper commentMapper, ReplyMapper replyMapper, CommentPathBuilder commentPathBuilder, TransitionMapper transitionMapper) {
    this.repositoryResolver = repositoryResolver;
    this.service = service;
    this.commentMapper = commentMapper;
    this.replyMapper = replyMapper;
    this.commentPathBuilder = commentPathBuilder;
    this.transitionMapper = transitionMapper;
  }

  @GET
  @Path("")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getComment(@PathParam("namespace") String namespace,
                             @PathParam("name") String name,
                             @PathParam("pullRequestId") String pullRequestId,
                             @PathParam("commentId") String commentId) {
    Repository repository = repositoryResolver.resolve(new NamespaceAndName(namespace, name));
    Comment comment = service.get(namespace, name, pullRequestId, commentId);
    Collection<CommentTransition> possibleTransitions = service.possibleTransitions(namespace, name, pullRequestId, comment.getId());
    return Response.ok(commentMapper.map(comment, repository, pullRequestId, possibleTransitions)).build();
  }

  @GET
  @Path("replies/{replyId}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getReply(@PathParam("namespace") String namespace,
                           @PathParam("name") String name,
                           @PathParam("pullRequestId") String pullRequestId,
                           @PathParam("commentId") String commentId,
                           @PathParam("replyId") String replyId) {
    Repository repository = repositoryResolver.resolve(new NamespaceAndName(namespace, name));
    Comment comment = service.get(namespace, name, pullRequestId, commentId);
    Reply reply = service.getReply(namespace, name, pullRequestId, commentId, replyId);
    return Response.ok(replyMapper.map(reply, repository, pullRequestId, comment)).build();
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
  public Response deleteComment(@Context UriInfo uriInfo,
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

  @DELETE
  @Path("replies/{replyId}")
  @StatusCodes({
    @ResponseCode(code = 204, condition = "delete success or nothing to delete"),
    @ResponseCode(code = 401, condition = "not authenticated / invalid credentials"),
    @ResponseCode(code = 403, condition = "not authorized, the current user is not allowed to delete the comment"),
    @ResponseCode(code = 500, condition = "internal server error")
  })
  @TypeHint(TypeHint.NO_CONTENT.class)
  public Response deleteReply(@Context UriInfo uriInfo,
                              @PathParam("namespace") String namespace,
                              @PathParam("name") String name,
                              @PathParam("pullRequestId") String pullRequestId,
                              @PathParam("commentId") String commentId,
                              @PathParam("replyId") String replyId) {
    try {
      service.delete(namespace, name, pullRequestId, replyId);
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
  public Response updateComment(@Context UriInfo uriInfo,
                                @PathParam("namespace") String namespace,
                                @PathParam("name") String name,
                                @PathParam("pullRequestId") String pullRequestId,
                                @PathParam("commentId") String commentId,
                                @Valid CommentDto commentDto) {
    service.modifyComment(namespace, name, pullRequestId, commentId, commentMapper.map(commentDto));
    return Response.noContent().build();
  }

  @PUT
  @Path("replies/{replyId}")
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
  public Response updateReply(@Context UriInfo uriInfo,
                              @PathParam("namespace") String namespace,
                              @PathParam("name") String name,
                              @PathParam("pullRequestId") String pullRequestId,
                              @PathParam("commentId") String commentId,
                              @PathParam("replyId") String replyId,
                              @Valid ReplyDto replyDto) {
    service.modifyReply(namespace, name, pullRequestId, replyId, replyMapper.map(replyDto));
    return Response.noContent().build();
  }

  @POST
  @Path("replies")
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

  @GET
  @Path("transitions")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getPossibleTransitions(@Context UriInfo uriInfo,
                                 @PathParam("namespace") String namespace,
                                 @PathParam("name") String name,
                                 @PathParam("pullRequestId") String pullRequestId,
                                 @PathParam("commentId") String commentId) {
    List<TransitionDto> transitions = service
      .possibleTransitions(namespace, name, pullRequestId, commentId)
      .stream()
      .map(t -> transitionMapper.map(t, namespace, name, pullRequestId, commentId))
      .collect(toList());
    return Response.ok(createCollection(uriInfo, false, transitions, "transitions")).build();
  }

  @POST
  @Path("transitions")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response transform(@Context UriInfo uriInfo,
                            @PathParam("namespace") String namespace,
                            @PathParam("name") String name,
                            @PathParam("pullRequestId") String pullRequestId,
                            @PathParam("commentId") String commentId,
                            @Valid TransitionDto transitionDto) {
    service.transform(namespace, name, pullRequestId, commentId, CommentTransition.valueOf(transitionDto.getName()));
    return Response.created(create(commentPathBuilder.createPossibleTransitionUri(namespace, name, pullRequestId, commentId))).build();
  }
}
