package com.cloudogu.scm.review.comment.api;

import com.cloudogu.scm.review.PermissionCheck;
import com.cloudogu.scm.review.RepositoryResolver;
import com.cloudogu.scm.review.comment.service.Comment;
import com.cloudogu.scm.review.comment.service.CommentService;
import com.cloudogu.scm.review.pullrequest.dto.BranchRevisionResolver;
import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import com.cloudogu.scm.review.pullrequest.service.PullRequestService;
import com.google.common.base.Strings;
import org.apache.shiro.authz.AuthorizationException;
import sonia.scm.ConcurrentModificationException;
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
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

import static com.cloudogu.scm.review.HalRepresentations.createCollection;

public class CommentRootResource {


  private final CommentMapper mapper;
  private final RepositoryResolver repositoryResolver;
  private final CommentService service;
  private final Provider<CommentResource> commentResourceProvider;
  private final CommentPathBuilder commentPathBuilder;
  private final PullRequestService pullRequestService;
  private final BranchRevisionResolver branchRevisionResolver;

  @Inject
  public CommentRootResource(CommentMapper mapper, RepositoryResolver repositoryResolver, CommentService service, Provider<CommentResource> commentResourceProvider, CommentPathBuilder commentPathBuilder, PullRequestService pullRequestService, BranchRevisionResolver branchRevisionResolver) {
    this.mapper = mapper;
    this.repositoryResolver = repositoryResolver;
    this.service = service;
    this.commentResourceProvider = commentResourceProvider;
    this.commentPathBuilder = commentPathBuilder;
    this.pullRequestService = pullRequestService;
    this.branchRevisionResolver = branchRevisionResolver;
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
                         @QueryParam("sourceRevision") String expectedSourceRevision,
                         @QueryParam("targetRevision") String expectedTargetRevision,
                         @Valid @NotNull CommentDto commentDto) {
    if (commentDto.isSystemComment()) {
      throw new AuthorizationException("Is is Forbidden to create a system comment.");
    }

    PullRequest pullRequest = pullRequestService.get(namespace, name, pullRequestId);
    if (!Strings.isNullOrEmpty(expectedSourceRevision) || !Strings.isNullOrEmpty(expectedTargetRevision)) {
      BranchRevisionResolver.RevisionResult revisions = branchRevisionResolver.getRevisions(new NamespaceAndName(namespace, name), pullRequest);
      String currentSourceRevision = revisions.getSourceRevision();
      String currentTargetRevision = revisions.getTargetRevision();
      if (revisionsMatchIfSpecified(expectedSourceRevision, expectedTargetRevision, currentSourceRevision, currentTargetRevision)) {
        throw new ConcurrentModificationException(PullRequest.class, pullRequestId);
      }
    }
    Comment comment = mapper.map(commentDto);
    String id = service.add(namespace, name, pullRequestId, comment);
    URI location = URI.create(commentPathBuilder.createCommentSelfUri(namespace, name, pullRequestId, id));
    return Response.created(location).build();
  }

  private boolean revisionsMatchIfSpecified(String expectedSourceRevision, String expectedTargetRevision, String currentSourceRevision, String currentTargetRevision) {
    return (!Strings.isNullOrEmpty(expectedSourceRevision) && !currentSourceRevision.equals(expectedSourceRevision)) ||
      (!Strings.isNullOrEmpty(expectedTargetRevision) && !currentTargetRevision.equals(expectedTargetRevision));
  }

  @GET
  @Path("")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getAll(@Context UriInfo uriInfo,
                         @PathParam("namespace") String namespace,
                         @PathParam("name") String name,
                         @PathParam("pullRequestId") String pullRequestId) {
    Repository repository = repositoryResolver.resolve(new NamespaceAndName(namespace, name));
    List<Comment> list = service.getAll(namespace, name, pullRequestId);
    List<CommentDto> dtoList = list
      .stream()
      .map(comment -> mapper.map(comment, repository, pullRequestId, service.possibleTransitions(namespace, name, pullRequestId, comment.getId())))
      .collect(Collectors.toList());
    boolean permission = PermissionCheck.mayComment(repository);
    return Response.ok(createCollection(uriInfo, permission, dtoList, "pullRequestComments")).build();
  }
}
