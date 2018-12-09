package com.cloudogu.scm.review.comment.api;


import com.cloudogu.scm.review.RepositoryResolver;
import com.cloudogu.scm.review.comment.dto.PullRequestCommentDto;
import com.cloudogu.scm.review.comment.dto.PullRequestCommentMapper;
import com.cloudogu.scm.review.comment.dto.PullRequestCommentMapperImpl;
import com.cloudogu.scm.review.comment.service.CommentService;
import com.cloudogu.scm.review.comment.service.PullRequestComment;
import org.apache.shiro.SecurityUtils;
import sonia.scm.NotFoundException;
import sonia.scm.repository.NamespaceAndName;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryPermissions;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
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


  @GET
  @Path("")
  @Produces(MediaType.APPLICATION_JSON)
  public Response get(@Context UriInfo uriInfo,
                      @PathParam("namespace") String namespace,
                      @PathParam("name") String name,
                      @PathParam("pullRequestId") String pullRequestId,
                      @PathParam("commentId") int commentId) {
    Repository repository = repositoryResolver.resolve(new NamespaceAndName(namespace, name));
    RepositoryPermissions.push(repository).check();
    PullRequestComment requestComment = service.get(namespace, name, pullRequestId, commentId);
    return Response.ok(mapper.map(requestComment,uriInfo.getAbsolutePathBuilder().build())).build();
  }

  @DELETE
  @Path("")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response delete(@Context UriInfo uriInfo,
                         @PathParam("namespace") String namespace,
                         @PathParam("name") String name,
                         @PathParam("pullRequestId") String pullRequestId,
                         @PathParam("commentId") int commentId) {
    Repository repository = repositoryResolver.resolve(new NamespaceAndName(namespace, name));
    RepositoryPermissions.push(repository).check();
    String currentUser = SecurityUtils.getSubject().getPrincipals().getPrimaryPrincipal().toString();
    try{
      PullRequestComment requestComment = service.get(namespace, name, pullRequestId, commentId);
      if (!currentUser.equals(requestComment.getAuthor())) {
        return Response.status(Response.Status.FORBIDDEN).build();
      }
      service.delete(namespace, name, pullRequestId, commentId);
      return Response.noContent().build();
    }catch(NotFoundException e){
      return Response.noContent().build();
    }
  }

  @PUT
  @Path("")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response update(@Context UriInfo uriInfo,
                         @PathParam("namespace") String namespace,
                         @PathParam("name") String name,
                         @PathParam("pullRequestId") String pullRequestId,
                         @PathParam("commentId") int commentId,
                         PullRequestCommentDto pullRequestCommentDto) {
    Repository repository = repositoryResolver.resolve(new NamespaceAndName(namespace, name));
    RepositoryPermissions.push(repository).check();
    String currentUser = SecurityUtils.getSubject().getPrincipals().getPrimaryPrincipal().toString();
    PullRequestComment requestComment = service.get(namespace, name, pullRequestId, commentId);
    if (!currentUser.equals(requestComment.getAuthor())) {
      return Response.status(Response.Status.FORBIDDEN).build();
    }
    service.delete(namespace, name, pullRequestId, commentId);
    pullRequestCommentDto.setDate(Instant.now());
    pullRequestCommentDto.setAuthor(SecurityUtils.getSubject().getPrincipals().getPrimaryPrincipal().toString());

    service.add(namespace, name, pullRequestId, mapper.map(pullRequestCommentDto));
    return Response.accepted().build();
  }
}
