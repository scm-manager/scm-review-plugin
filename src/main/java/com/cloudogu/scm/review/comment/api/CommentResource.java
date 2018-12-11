package com.cloudogu.scm.review.comment.api;


import com.cloudogu.scm.review.RepositoryResolver;
import com.cloudogu.scm.review.comment.dto.PullRequestCommentDto;
import com.cloudogu.scm.review.comment.dto.PullRequestCommentMapper;
import com.cloudogu.scm.review.comment.dto.PullRequestCommentMapperImpl;
import com.cloudogu.scm.review.comment.service.CommentService;
import com.cloudogu.scm.review.comment.service.PullRequestComment;
import com.webcohesion.enunciate.metadata.rs.ResponseCode;
import com.webcohesion.enunciate.metadata.rs.StatusCodes;
import com.webcohesion.enunciate.metadata.rs.TypeHint;
import sonia.scm.NotFoundException;
import sonia.scm.repository.NamespaceAndName;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryPermissions;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.time.Instant;

public class CommentResource {

  private final PullRequestCommentMapper mapper;
  private final CommentService service;
  private RepositoryResolver repositoryResolver;


  @Inject
  public CommentResource(CommentService service, RepositoryResolver repositoryResolver) {
    this.repositoryResolver = repositoryResolver;
    this.mapper = new PullRequestCommentMapperImpl();
    this.service = service;
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
                         @PathParam("commentId") int commentId) {
    Repository repository = repositoryResolver.resolve(new NamespaceAndName(namespace, name));
    RepositoryPermissions.read(repository).check();
    try {
      if (!service.modificationsAllowed(namespace, name, pullRequestId, commentId, repository)) {
        return Response.status(Response.Status.FORBIDDEN).build();
      }
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
                         @PathParam("commentId") int commentId,
                         PullRequestCommentDto pullRequestCommentDto) {
    Repository repository = repositoryResolver.resolve(new NamespaceAndName(namespace, name));
    RepositoryPermissions.read(repository).check();
    PullRequestComment comment = service.get(namespace, name, pullRequestId, commentId);
    String originalAuthor = comment.getAuthor();
    if (!service.modificationsAllowed(repository, comment)) {
      return Response.status(Response.Status.FORBIDDEN).build();
    }
    service.delete(namespace, name, pullRequestId, commentId);
    pullRequestCommentDto.setDate(Instant.now());
    pullRequestCommentDto.setAuthor(originalAuthor);
    service.add(namespace, name, pullRequestId, mapper.map(pullRequestCommentDto));
    return Response.accepted().build();
  }
}
