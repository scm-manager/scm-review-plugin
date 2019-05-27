package com.cloudogu.scm.review.comment.api;

import com.cloudogu.scm.review.PermissionCheck;
import com.cloudogu.scm.review.RepositoryResolver;
import com.cloudogu.scm.review.comment.dto.PullRequestCommentDto;
import com.cloudogu.scm.review.comment.dto.PullRequestCommentMapper;
import com.cloudogu.scm.review.comment.service.CommentService;
import com.cloudogu.scm.review.comment.service.PullRequestComment;
import com.google.common.collect.Maps;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.AuthorizationException;
import sonia.scm.repository.NamespaceAndName;
import sonia.scm.repository.Repository;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.validation.Valid;
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
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.cloudogu.scm.review.HalRepresentations.createCollection;

public class CommentRootResource {


  private final PullRequestCommentMapper mapper;
  private final RepositoryResolver repositoryResolver;
  private final CommentService service;
  private final Provider<CommentResource> commentResourceProvider;
  private final CommentPathBuilder commentPathBuilder;


  @Inject
  public CommentRootResource(PullRequestCommentMapper mapper, RepositoryResolver repositoryResolver, CommentService service, Provider<CommentResource> commentResourceProvider, CommentPathBuilder commentPathBuilder) {
    this.mapper = mapper;
    this.repositoryResolver = repositoryResolver;
    this.service = service;
    this.commentResourceProvider = commentResourceProvider;
    this.commentPathBuilder = commentPathBuilder;
  }


  @Path("{commentId}")
  public CommentResource getCommentResource() {
    return commentResourceProvider.get();
  }

  @POST
  @Path("")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response create(@PathParam("namespace") String namespace,
                         @PathParam("name") String name,
                         @PathParam("pullRequestId") String pullRequestId,
                         @Valid @NotNull PullRequestCommentDto pullRequestCommentDto) {
    if (pullRequestCommentDto.isSystemComment()){
      throw new AuthorizationException("Is is Forbidden to create a system comment.");
    }
    PermissionCheck.checkComment(repositoryResolver.resolve(new NamespaceAndName(namespace, name)));
    Repository repository = repositoryResolver.resolve(new NamespaceAndName(namespace, name));
    PermissionCheck.checkComment(repository);

    PullRequestComment comment = mapper.map(pullRequestCommentDto);
    comment.setDate(Instant.now());
    comment.setAuthor(SecurityUtils.getSubject().getPrincipals().getPrimaryPrincipal().toString());
    String id = service.add(repository,  pullRequestId, comment);
    URI location = URI.create(commentPathBuilder.createCommentSelfUri(namespace, name, pullRequestId, id));
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
    PermissionCheck.checkRead(repository);
    List<PullRequestComment> list = service.getAll(namespace, name, pullRequestId);
    Set<String> parentsIds = list
      .stream()
      .filter(comment -> comment.getParentId() != null)
      .map(PullRequestComment::getParentId)
      .collect(Collectors.toSet());
    List<PullRequestCommentDto> dtoList = list
      .stream()
      .map(comment -> {
        URI self = URI.create(commentPathBuilder.createCommentSelfUri(namespace, name, pullRequestId, comment.getId()));
        URI update = URI.create(commentPathBuilder.createUpdateCommentUri(namespace, name, pullRequestId, comment.getId()));
        URI delete = URI.create(commentPathBuilder.createDeleteCommentUri(namespace, name, pullRequestId, comment.getId()));
        URI reply = URI.create(commentPathBuilder.createReplyCommentUri(namespace, name, pullRequestId, comment.getId()));
        Map<String, URI> uriMap = Maps.newHashMap();
        uriMap.put("self", self);
        if (!comment.isSystemComment() && PermissionCheck.mayModifyComment(repository, service.get(namespace, name, pullRequestId, comment.getId()))) {
          uriMap.put("update", update);
          if (!parentsIds.contains(comment.getId())) {
            uriMap.put("delete", delete);
          } else {
            uriMap.put("reply", reply);
          }
        }
        return mapper.map(comment, uriMap);
      })
      .collect(Collectors.toList());
    boolean permission = PermissionCheck.mayComment(repository);
    return Response.ok(createCollection(uriInfo, permission, dtoList, "pullRequestComments")).build();
  }

}
