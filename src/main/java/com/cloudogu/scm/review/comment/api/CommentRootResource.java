/*
 * Copyright (c) 2020 - present Cloudogu GmbH
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Affero General Public License as published by the Free
 * Software Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see https://www.gnu.org/licenses/.
 */

package com.cloudogu.scm.review.comment.api;

import com.cloudogu.scm.review.PermissionCheck;
import com.cloudogu.scm.review.PullRequestResourceLinks;
import com.cloudogu.scm.review.RepositoryResolver;
import com.cloudogu.scm.review.comment.service.Comment;
import com.cloudogu.scm.review.comment.service.CommentService;
import com.cloudogu.scm.review.comment.service.PullRequestImageService;
import com.cloudogu.scm.review.pullrequest.dto.BranchRevisionResolver;
import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import com.cloudogu.scm.review.pullrequest.service.PullRequestService;
import de.otto.edison.hal.Embedded;
import de.otto.edison.hal.HalRepresentation;
import de.otto.edison.hal.Links;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.apache.shiro.authz.AuthorizationException;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;
import sonia.scm.api.v2.resources.ErrorDto;
import sonia.scm.repository.NamespaceAndName;
import sonia.scm.repository.Repository;
import sonia.scm.web.VndMediaType;
import sonia.scm.web.api.DtoValidator;

import java.io.IOException;
import java.net.URI;
import java.util.List;

import static com.cloudogu.scm.review.comment.api.RevisionChecker.checkRevision;
import static de.otto.edison.hal.Link.link;

public class CommentRootResource {


  private final CommentMapper mapper;
  private final RepositoryResolver repositoryResolver;
  private final CommentService service;
  private final Provider<CommentResource> commentResourceProvider;
  private final CommentPathBuilder commentPathBuilder;
  private final PullRequestService pullRequestService;
  private final BranchRevisionResolver branchRevisionResolver;
  private final PullRequestImageService imageService;

  @Inject
  public CommentRootResource(CommentMapper mapper, RepositoryResolver repositoryResolver, CommentService service, Provider<CommentResource> commentResourceProvider, CommentPathBuilder commentPathBuilder, PullRequestService pullRequestService, BranchRevisionResolver branchRevisionResolver, PullRequestImageService imageService) {
    this.mapper = mapper;
    this.repositoryResolver = repositoryResolver;
    this.service = service;
    this.commentResourceProvider = commentResourceProvider;
    this.commentPathBuilder = commentPathBuilder;
    this.pullRequestService = pullRequestService;
    this.branchRevisionResolver = branchRevisionResolver;
    this.imageService = imageService;
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
                         @QueryParam("sourceRevision") String expectedSourceRevision,
                         @QueryParam("targetRevision") String expectedTargetRevision,
                         @Valid @NotNull CommentDto commentDto) {
    String commentId = createComment(namespace, name, pullRequestId, expectedSourceRevision, expectedTargetRevision, commentDto);
    URI location = URI.create(commentPathBuilder.createCommentSelfUri(namespace, name, pullRequestId, commentId));
    return Response.created(location).build();
  }

  private String createComment(String namespace, String name, String pullRequestId, String expectedSourceRevision, String expectedTargetRevision, @Valid @NotNull CommentDto commentDto) {
    if (commentDto.isSystemComment()) {
      throw new AuthorizationException("Is is Forbidden to create a system comment.");
    }

    checkRevision(branchRevisionResolver, namespace, name, pullRequestId, expectedSourceRevision, expectedTargetRevision);
    Comment comment = mapper.map(commentDto);
    return service.add(namespace, name, pullRequestId, comment);
  }

  @GET
  @Path("/images/{fileHash}")
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  @Operation(summary = "Fetch an image related to the comment", description = "Fetch an image related to the comment", tags = "Pull Request Comment")
  @ApiResponse(responseCode = "200", description = "success")
  @ApiResponse(responseCode = "401", description = "not authenticated / invalid credentials")
  @ApiResponse(responseCode = "404", description = "not found, no comment or image with the specified id is available")
  @ApiResponse(
    responseCode = "500",
    description = "internal server error",
    content = @Content(
      mediaType = VndMediaType.ERROR_TYPE,
      schema = @Schema(implementation = ErrorDto.class)
    )
  )
  public Response getCommentImage(@PathParam("namespace") String namespace,
                                  @PathParam("name") String name,
                                  @PathParam("pullRequestId") String pullRequestId,
                                  @PathParam("fileHash") String fileHash) throws IOException {
    PullRequestImageService.ImageStream image = imageService.getPullRequestImage(new NamespaceAndName(namespace, name), pullRequestId, fileHash);
    return Response.ok(image.getImageStream().readAllBytes(), MediaType.valueOf(image.getFiletype())).build();
  }

  @POST
  @Path("/images")
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Operation(
    summary = "Create pull request comment and upload images with it",
    description = "Creates a new pull request comment and upload images with it.",
    tags = "Pull Request Comment"
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
  public Response createWithImage(@PathParam("namespace") String namespace,
                                  @PathParam("name") String name,
                                  @PathParam("pullRequestId") String pullRequestId,
                                  @QueryParam("sourceRevision") String expectedSourceRevision,
                                  @QueryParam("targetRevision") String expectedTargetRevision,
                                  @NotNull MultipartFormDataInput formInput) throws IOException {
    MultipartFormDataInputHelper<CommentWithImageDto> formDataHelper = new MultipartFormDataInputHelper<>(formInput);
    CommentWithImageDto commentDto = formDataHelper.extractJsonObject(CommentWithImageDto.class, "comment");
    DtoValidator.validate(commentDto);
    String commentId = createComment(namespace, name, pullRequestId, expectedSourceRevision, expectedTargetRevision, commentDto);

    formDataHelper.processFiles((fileHash, fileBody) ->
      imageService.createCommentImage(new NamespaceAndName(namespace, name), pullRequestId, commentId, fileHash, commentDto.getFiletypes().get(fileHash), fileBody)
    );

    URI location = URI.create(commentPathBuilder.createCommentSelfUri(namespace, name, pullRequestId, commentId));
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
      .toList();

    Links.Builder linkBuilder = Links.linkingTo().self(resourceLinks.pullRequestComments().all(namespace, name, pullRequestId));
    if (PermissionCheck.mayComment(repository)) {
      linkBuilder.single(link("create", resourceLinks.pullRequestComments().create(namespace, name, pullRequestId, revisions)));
      linkBuilder.single(link("createWithImages", resourceLinks.pullRequestComments().createWithImages(namespace, name, pullRequestId, revisions)));
    }

    return new HalRepresentation(linkBuilder.build(), Embedded.embedded("pullRequestComments", dtoList));
  }
}
