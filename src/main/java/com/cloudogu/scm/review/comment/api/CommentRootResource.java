package com.cloudogu.scm.review.comment.api;

import com.cloudogu.scm.review.RepositoryResolver;
import com.cloudogu.scm.review.comment.dto.PullRequestCommentDto;
import com.cloudogu.scm.review.comment.dto.PullRequestCommentMapper;
import com.cloudogu.scm.review.comment.dto.PullRequestCommentMapperImpl;
import com.cloudogu.scm.review.comment.service.CommentService;
import com.cloudogu.scm.review.comment.service.PullRequestComment;
import org.apache.shiro.SecurityUtils;
import sonia.scm.repository.NamespaceAndName;
import sonia.scm.repository.Repository;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import static com.cloudogu.scm.review.HalRepresentations.createCollection;

public class CommentRootResource {


  private final PullRequestCommentMapper mapper;
  private final RepositoryResolver repositoryResolver;
  private final CommentService service;
  private final Provider<CommentResource> commentResourceProvider;


  @Inject
  public CommentRootResource(RepositoryResolver repositoryResolver, CommentService service, Provider<CommentResource> commentResourceProvider) {
    this.repositoryResolver = repositoryResolver;
    this.mapper = new PullRequestCommentMapperImpl();
    this.service = service;
    this.commentResourceProvider = commentResourceProvider;
  }


  @Path("{commentId}")
  public CommentResource getCommentResource() {
    return commentResourceProvider.get();
  }

  @POST
  @Path("")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response create(@Context UriInfo uriInfo,
                         @PathParam("namespace") String namespace,
                         @PathParam("name") String name,
                         @PathParam("pullRequestId") String pullRequestId,
                         @NotNull PullRequestCommentDto pullRequestCommentDto) {

    pullRequestCommentDto.setDate(Instant.now());
    pullRequestCommentDto.setAuthor(SecurityUtils.getSubject().getPrincipals().getPrimaryPrincipal().toString());

    String id = service.add(namespace, name, pullRequestId, mapper.map(pullRequestCommentDto));
    URI location = uriInfo.getAbsolutePathBuilder().path(id).build();
    return Response.created(location).build();
  }

  @GET
  @Path("")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getAll(@Context UriInfo uriInfo,
                         @PathParam("namespace") String namespace,
                         @PathParam("name") String name,
                         @PathParam("pullRequestId") String pullRequestId) {

    Repository repository = repositoryResolver.resolve(new NamespaceAndName(namespace, name));
    List<PullRequestComment> list = service.getAll(namespace, name, pullRequestId);
    List<PullRequestCommentDto> dtoList = list.stream()
      .map(mapper::map)
      .collect(Collectors.toList());
    return Response.ok(createCollection(uriInfo, repository.getId(), dtoList, "pullRequestComments")).build();
  }
}
