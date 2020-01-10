package com.cloudogu.scm.review.comment.api;

import com.cloudogu.scm.review.PermissionCheck;
import com.cloudogu.scm.review.PullRequestResourceLinks;
import com.cloudogu.scm.review.RepositoryResolver;
import com.cloudogu.scm.review.comment.service.Comment;
import com.cloudogu.scm.review.comment.service.CommentService;
import com.cloudogu.scm.review.pullrequest.dto.BranchRevisionResolver;
import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import com.cloudogu.scm.review.pullrequest.service.PullRequestService;
import de.otto.edison.hal.HalRepresentation;
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
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

import static com.cloudogu.scm.review.HalRepresentations.createCollection;
import static com.cloudogu.scm.review.comment.api.RevisionChecker.checkRevision;

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
  public CommentResource getCommentResource(@PathParam("namespace") String namespace,
                                            @PathParam("name") String name,
                                            @PathParam("pullRequestId") String pullRequestId,
                                            @QueryParam("sourceRevision") String expectedSourceRevision,
                                            @QueryParam("targetRevision") String expectedTargetRevision) {
    checkRevision(branchRevisionResolver, namespace, name, pullRequestId, expectedSourceRevision, expectedTargetRevision);
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

    checkRevision(branchRevisionResolver, namespace, name, pullRequestId, expectedSourceRevision, expectedTargetRevision);
    Comment comment = mapper.map(commentDto);
    String id = service.add(namespace, name, pullRequestId, comment);
    URI location = URI.create(commentPathBuilder.createCommentSelfUri(namespace, name, pullRequestId, id));
    return Response.created(location).build();
  }


  @GET
  @Path("")
  @Produces(MediaType.APPLICATION_JSON)
  public HalRepresentation getAll(@Context UriInfo uriInfo,
                                  @PathParam("namespace") String namespace,
                                  @PathParam("name") String name,
                                  @PathParam("pullRequestId") String pullRequestId) {
    PullRequestResourceLinks resourceLinks = new PullRequestResourceLinks(uriInfo::getBaseUri);
    Repository repository = repositoryResolver.resolve(new NamespaceAndName(namespace, name));
    PullRequest pullRequest = pullRequestService.get(namespace, name, pullRequestId);
    BranchRevisionResolver.RevisionResult revisions = branchRevisionResolver.getRevisions(new NamespaceAndName(namespace, name), pullRequest);
    List<Comment> list = service.getAll(namespace, name, pullRequestId);
    List<CommentDto> dtoList = list
      .stream()
      .map(comment -> mapper.map(comment, repository, pullRequestId, service.possibleTransitions(namespace, name, pullRequestId, comment.getId()), revisions))
      .collect(Collectors.toList());
    boolean permission = PermissionCheck.mayComment(repository);
    return createCollection(
        permission,
        resourceLinks.pullRequestComments().all(namespace, name, pullRequestId),
        resourceLinks.pullRequestComments().create(namespace, name, pullRequestId, revisions),
        dtoList,
        "pullRequestComments");
  }
}
