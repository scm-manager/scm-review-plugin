/**
 * MIT License
 *
 * Copyright (c) 2020-present Cloudogu GmbH and Contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.cloudogu.scm.review.comment.api;

import com.cloudogu.scm.review.PermissionCheck;
import com.cloudogu.scm.review.PullRequestMediaType;
import com.cloudogu.scm.review.PullRequestResourceLinks;
import com.cloudogu.scm.review.RepositoryResolver;
import com.cloudogu.scm.review.comment.service.Comment;
import com.cloudogu.scm.review.comment.service.CommentService;
import com.cloudogu.scm.review.pullrequest.dto.BranchRevisionResolver;
import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import com.cloudogu.scm.review.pullrequest.service.PullRequestService;
import de.otto.edison.hal.HalRepresentation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.apache.shiro.authz.AuthorizationException;
import sonia.scm.api.v2.resources.ErrorDto;
import sonia.scm.repository.NamespaceAndName;
import sonia.scm.repository.Repository;
import sonia.scm.web.VndMediaType;

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
  public CommentResource getCommentResource() {
    return commentResourceProvider.get();
  }

  @POST
  @Path("")
  @Consumes(MediaType.APPLICATION_JSON)
  @Operation(
    summary = "Create pull request comment",
    description = "Creates a new pull request comment.",
    tags = "Pull Request Comment",
    operationId = "review_create_comment"
  )
  @ApiResponse(responseCode = "201", description = "create success")
  @ApiResponse(responseCode = "400", description = "Invalid body, e.g. illegal change of namespace or name")
  @ApiResponse(responseCode = "401", description = "not authenticated / invalid credentials")
  @ApiResponse(responseCode = "403", description = "not authorized, the current user does not have the \"commentPullRequest\" privilege")
  @ApiResponse(
    responseCode = "500",
    description = "internal server error",
    content = @Content(
      mediaType = VndMediaType.ERROR_TYPE,
      schema = @Schema(implementation = ErrorDto.class)
    )
  )
  public Response create(@PathParam("namespace") String namespace,
                         @PathParam("name") String name,
                         @PathParam("pullRequestId") String pullRequestId,
                         @PathParam("commentId") String commentId,
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
  @Operation(
    summary = "Get all pull request comments",
    description = "Returns all pull request comments.",
    tags = "Pull Request Comment",
    operationId = "review_get_comments"
  )
  @ApiResponse(
    responseCode = "200",
    description = "success",
    content = @Content(
      mediaType = MediaType.APPLICATION_JSON,
      schema = @Schema(implementation = HalRepresentation.class)
    )
  )
  @ApiResponse(responseCode = "401", description = "not authenticated / invalid credentials")
  @ApiResponse(responseCode = "403", description = "not authorized, the current user does not have the \"commentPullRequest\" privilege")
  @ApiResponse(
    responseCode = "500",
    description = "internal server error",
    content = @Content(
      mediaType = VndMediaType.ERROR_TYPE,
      schema = @Schema(implementation = ErrorDto.class)
    )
  )
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
