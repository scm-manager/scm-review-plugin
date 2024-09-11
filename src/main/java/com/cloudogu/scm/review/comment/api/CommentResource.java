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


import com.cloudogu.scm.review.PullRequestMediaType;
import com.cloudogu.scm.review.RepositoryResolver;
import com.cloudogu.scm.review.comment.service.Comment;
import com.cloudogu.scm.review.comment.service.CommentService;
import com.cloudogu.scm.review.comment.service.CommentTransition;
import com.cloudogu.scm.review.comment.service.ExecutedTransition;
import com.cloudogu.scm.review.comment.service.PullRequestImageService;
import com.cloudogu.scm.review.comment.service.Reply;
import com.cloudogu.scm.review.pullrequest.dto.BranchRevisionResolver;
import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;
import sonia.scm.NotFoundException;
import sonia.scm.api.v2.resources.ErrorDto;
import sonia.scm.repository.NamespaceAndName;
import sonia.scm.repository.Repository;
import sonia.scm.web.VndMediaType;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import sonia.scm.web.api.DtoValidator;

import java.io.IOException;
import java.util.Collection;

import static com.cloudogu.scm.review.comment.api.RevisionChecker.checkRevision;
import static java.net.URI.create;
import static sonia.scm.ContextEntry.ContextBuilder.entity;
import static sonia.scm.NotFoundException.notFound;

@OpenAPIDefinition(tags = {
  @Tag(name = "Pull Request Comment", description = "Pull request comment endpoints provided by the review-plugin")
})
public class CommentResource {

  private final CommentService service;
  private final RepositoryResolver repositoryResolver;
  private final CommentMapper commentMapper;
  private final ReplyMapper replyMapper;
  private final CommentPathBuilder commentPathBuilder;
  private final ExecutedTransitionMapper executedTransitionMapper;
  private final PullRequestImageService pullRequestImageService;
  private BranchRevisionResolver branchRevisionResolver;

  @Inject
  public CommentResource(CommentService service,
                         RepositoryResolver repositoryResolver,
                         CommentMapper commentMapper,
                         ReplyMapper replyMapper,
                         CommentPathBuilder commentPathBuilder,
                         ExecutedTransitionMapper executedTransitionMapper,
                         PullRequestImageService pullRequestImageService,
                         BranchRevisionResolver branchRevisionResolver) {
    this.repositoryResolver = repositoryResolver;
    this.service = service;
    this.commentMapper = commentMapper;
    this.replyMapper = replyMapper;
    this.commentPathBuilder = commentPathBuilder;
    this.executedTransitionMapper = executedTransitionMapper;
    this.pullRequestImageService = pullRequestImageService;
    this.branchRevisionResolver = branchRevisionResolver;
  }

  @GET
  @Path("")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(PullRequestMediaType.PULL_REQUEST)
  @Operation(summary = "Pull request comment", description = "Returns a single pull request comment.", tags = "Pull Request Comment")
  @ApiResponse(
    responseCode = "200",
    description = "success",
    content = @Content(
      mediaType = MediaType.APPLICATION_JSON,
      schema = @Schema(implementation = CommentDto.class)
    )
  )
  @ApiResponse(responseCode = "401", description = "not authenticated / invalid credentials")
  @ApiResponse(responseCode = "403", description = "not authorized, the current user does not have the \"commentPullRequest\" privilege")
  @ApiResponse(responseCode = "404", description = "not found, a comment with the given id is not available for this pull request")
  @ApiResponse(
    responseCode = "500",
    description = "internal server error",
    content = @Content(
      mediaType = VndMediaType.ERROR_TYPE,
      schema = @Schema(implementation = ErrorDto.class)
    )
  )
  public CommentDto getComment(@PathParam("namespace") String namespace,
                               @PathParam("name") String name,
                               @PathParam("pullRequestId") String pullRequestId,
                               @PathParam("commentId") String commentId,
                               @QueryParam("sourceRevision") String expectedSourceRevision,
                               @QueryParam("targetRevision") String expectedTargetRevision) {
    checkRevision(branchRevisionResolver, namespace, name, pullRequestId, expectedSourceRevision, expectedTargetRevision);
    Repository repository = repositoryResolver.resolve(new NamespaceAndName(namespace, name));
    BranchRevisionResolver.RevisionResult revisions = branchRevisionResolver.getRevisions(namespace, name, pullRequestId);
    Comment comment = service.get(namespace, name, pullRequestId, commentId);
    Collection<CommentTransition> possibleTransitions = service.possibleTransitions(namespace, name, pullRequestId, comment.getId());
    return commentMapper.map(comment, repository, pullRequestId, possibleTransitions, revisions);
  }

  @GET
  @Path("replies/{replyId}")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(summary = "Pull request comment reply", description = "Returns a single pull request comment reply.", tags = "Pull Request Comment")
  @ApiResponse(
    responseCode = "200",
    description = "success",
    content = @Content(
      mediaType = MediaType.APPLICATION_JSON,
      schema = @Schema(implementation = ReplyDto.class)
    )
  )
  @ApiResponse(responseCode = "401", description = "not authenticated / invalid credentials")
  @ApiResponse(responseCode = "403", description = "not authorized, the current user does not have the \"commentPullRequest\" privilege")
  @ApiResponse(responseCode = "404", description = "not found, a reply with the given id is not available for this pull request")
  @ApiResponse(
    responseCode = "500",
    description = "internal server error",
    content = @Content(
      mediaType = VndMediaType.ERROR_TYPE,
      schema = @Schema(implementation = ErrorDto.class)
    )
  )
  public ReplyDto getReply(@PathParam("namespace") String namespace,
                           @PathParam("name") String name,
                           @PathParam("pullRequestId") String pullRequestId,
                           @PathParam("commentId") String commentId,
                           @PathParam("replyId") String replyId,
                           @QueryParam("sourceRevision") String expectedSourceRevision,
                           @QueryParam("targetRevision") String expectedTargetRevision) {
    checkRevision(branchRevisionResolver, namespace, name, pullRequestId, expectedSourceRevision, expectedTargetRevision);
    Repository repository = repositoryResolver.resolve(new NamespaceAndName(namespace, name));
    BranchRevisionResolver.RevisionResult revisions = branchRevisionResolver.getRevisions(namespace, name, pullRequestId);
    Comment comment = service.get(namespace, name, pullRequestId, commentId);
    Reply reply = service.getReply(namespace, name, pullRequestId, commentId, replyId);
    return replyMapper.map(reply, repository, pullRequestId, comment, revisions);
  }

  @DELETE
  @Path("")
  @Operation(summary = "Delete pull request comment", description = "Deletes a pull request comment.", tags = "Pull Request Comment")
  @ApiResponse(responseCode = "204", description = "delete success or nothing to delete")
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
  public void deleteComment(@Context UriInfo uriInfo,
                            @PathParam("namespace") String namespace,
                            @PathParam("name") String name,
                            @PathParam("pullRequestId") String pullRequestId,
                            @PathParam("commentId") String commentId,
                            @QueryParam("sourceRevision") String expectedSourceRevision,
                            @QueryParam("targetRevision") String expectedTargetRevision) {
    checkRevision(branchRevisionResolver, namespace, name, pullRequestId, expectedSourceRevision, expectedTargetRevision);
    try {
      service.delete(namespace, name, pullRequestId, commentId);
    } catch (NotFoundException e) {
      // this is idempotent
    }
  }

  @DELETE
  @Path("replies/{replyId}")
  @Operation(summary = "Delete pull request comment reply", description = "Deletes a pull request comment reply.", tags = "Pull Request Comment")
  @ApiResponse(responseCode = "204", description = "delete success or nothing to delete")
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
  public void deleteReply(@Context UriInfo uriInfo,
                          @PathParam("namespace") String namespace,
                          @PathParam("name") String name,
                          @PathParam("pullRequestId") String pullRequestId,
                          @PathParam("commentId") String commentId,
                          @PathParam("replyId") String replyId,
                          @QueryParam("sourceRevision") String expectedSourceRevision,
                          @QueryParam("targetRevision") String expectedTargetRevision) {
    checkRevision(branchRevisionResolver, namespace, name, pullRequestId, expectedSourceRevision, expectedTargetRevision);
    try {
      service.delete(namespace, name, pullRequestId, replyId);
    } catch (NotFoundException e) {
      // this is idempotent
    }
  }

  @PUT
  @Path("")
  @Consumes(MediaType.APPLICATION_JSON)
  @Operation(summary = "Update pull request comment", description = "Modifies a pull request comment.", tags = "Pull Request Comment")
  @ApiResponse(responseCode = "204", description = "update success")
  @ApiResponse(responseCode = "400", description = "Invalid body, e.g. illegal change of namespace or name")
  @ApiResponse(responseCode = "401", description = "not authenticated / invalid credentials")
  @ApiResponse(responseCode = "403", description = "not authorized, the current user does not have the \"commentPullRequest\" privilege")
  @ApiResponse(responseCode = "404", description = "not found, no comment with the specified id is available")
  @ApiResponse(
    responseCode = "500",
    description = "internal server error",
    content = @Content(
      mediaType = VndMediaType.ERROR_TYPE,
      schema = @Schema(implementation = ErrorDto.class)
    )
  )
  public void updateComment(@Context UriInfo uriInfo,
                            @PathParam("namespace") String namespace,
                            @PathParam("name") String name,
                            @PathParam("pullRequestId") String pullRequestId,
                            @PathParam("commentId") String commentId,
                            @QueryParam("sourceRevision") String expectedSourceRevision,
                            @QueryParam("targetRevision") String expectedTargetRevision,
                            @Valid CommentDto commentDto) {
    checkRevision(branchRevisionResolver, namespace, name, pullRequestId, expectedSourceRevision, expectedTargetRevision);
    service.modifyComment(namespace, name, pullRequestId, commentId, commentMapper.map(commentDto));
  }

  @PUT
  @Path("/images")
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Operation(summary = "Update pull request comment and upload an images with it", description = "Modifies a pull request comment and upload images with it.", tags = "Pull Request Comment")
  @ApiResponse(responseCode = "204", description = "update success")
  @ApiResponse(responseCode = "400", description = "Invalid body, e.g. illegal change of namespace or name")
  @ApiResponse(responseCode = "401", description = "not authenticated / invalid credentials")
  @ApiResponse(responseCode = "403", description = "not authorized, the current user does not have the \"commentPullRequest\" privilege")
  @ApiResponse(responseCode = "404", description = "not found, no comment with the specified id is available")
  @ApiResponse(
    responseCode = "500",
    description = "internal server error",
    content = @Content(
      mediaType = VndMediaType.ERROR_TYPE,
      schema = @Schema(implementation = ErrorDto.class)
    )
  )
  public void updateCommentWithImage(@PathParam("namespace") String namespace,
                                   @PathParam("name") String name,
                                   @PathParam("pullRequestId") String pullRequestId,
                                   @PathParam("commentId") String commentId,
                                   @QueryParam("sourceRevision") String expectedSourceRevision,
                                   @QueryParam("targetRevision") String expectedTargetRevision,
                                   @NotNull MultipartFormDataInput formInput) throws IOException {
    MultipartFormDataInputHelper<CommentWithImageDto> formDataHelper = new MultipartFormDataInputHelper<>(formInput);
    CommentWithImageDto commentDto = formDataHelper.extractJsonObject(CommentWithImageDto.class, "comment");
    DtoValidator.validate(commentDto);
    checkRevision(branchRevisionResolver, namespace, name, pullRequestId, expectedSourceRevision, expectedTargetRevision);
    service.modifyComment(namespace, name, pullRequestId, commentId, commentMapper.map(commentDto));

    formDataHelper.processFiles((fileHash, fileBody) ->
      pullRequestImageService.createCommentImage(new NamespaceAndName(namespace, name), pullRequestId, commentId, fileHash, commentDto.getFiletypes().get(fileHash), fileBody)
    );
  }

  @PUT
  @Path("replies/{replyId}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Operation(summary = "Update pull request comment reply", description = "Modifies a pull request comment reply.", tags = "Pull Request Comment")
  @ApiResponse(responseCode = "204", description = "update success")
  @ApiResponse(responseCode = "400", description = "Invalid body, e.g. illegal change of namespace or name")
  @ApiResponse(responseCode = "401", description = "not authenticated / invalid credentials")
  @ApiResponse(responseCode = "403", description = "not authorized, the current user does not have the \"commentPullRequest\" privilege")
  @ApiResponse(responseCode = "404", description = "not found, no comment with the specified id is available")
  @ApiResponse(
    responseCode = "500",
    description = "internal server error",
    content = @Content(
      mediaType = VndMediaType.ERROR_TYPE,
      schema = @Schema(implementation = ErrorDto.class)
    )
  )
  public void updateReply(@Context UriInfo uriInfo,
                          @PathParam("namespace") String namespace,
                          @PathParam("name") String name,
                          @PathParam("pullRequestId") String pullRequestId,
                          @PathParam("commentId") String commentId,
                          @PathParam("replyId") String replyId,
                          @QueryParam("sourceRevision") String expectedSourceRevision,
                          @QueryParam("targetRevision") String expectedTargetRevision,
                          @Valid ReplyDto replyDto) {
    checkRevision(branchRevisionResolver, namespace, name, pullRequestId, expectedSourceRevision, expectedTargetRevision);
    service.modifyReply(namespace, name, pullRequestId, replyId, replyMapper.map(replyDto));
  }

  @PUT
  @Path("replies/{replyId}/images")
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Operation(summary = "Update pull request comment reply and upload an image with it", description = "Modifies a pull request comment reply and upload an image with it.", tags = "Pull Request Comment")
  @ApiResponse(responseCode = "204", description = "update success")
  @ApiResponse(responseCode = "400", description = "Invalid body, e.g. illegal change of namespace or name")
  @ApiResponse(responseCode = "401", description = "not authenticated / invalid credentials")
  @ApiResponse(responseCode = "403", description = "not authorized, the current user does not have the \"commentPullRequest\" privilege")
  @ApiResponse(responseCode = "404", description = "not found, no comment with the specified id is available")
  @ApiResponse(
    responseCode = "500",
    description = "internal server error",
    content = @Content(
      mediaType = VndMediaType.ERROR_TYPE,
      schema = @Schema(implementation = ErrorDto.class)
    )
  )
  public void updateReplyWithImage(@PathParam("namespace") String namespace,
                                 @PathParam("name") String name,
                                 @PathParam("pullRequestId") String pullRequestId,
                                 @PathParam("commentId") String commentId,
                                 @PathParam("replyId") String replyId,
                                 @QueryParam("sourceRevision") String expectedSourceRevision,
                                 @QueryParam("targetRevision") String expectedTargetRevision,
                                 @NotNull MultipartFormDataInput formInput) throws IOException {
    MultipartFormDataInputHelper<ReplyWithImageDto> formDataHelper = new MultipartFormDataInputHelper<>(formInput);
    ReplyWithImageDto replyDto = formDataHelper.extractJsonObject(ReplyWithImageDto.class, "comment");
    DtoValidator.validate(replyDto);
    checkRevision(branchRevisionResolver, namespace, name, pullRequestId, expectedSourceRevision, expectedTargetRevision);
    service.modifyReply(namespace, name, pullRequestId, replyId, replyMapper.map(replyDto));

    formDataHelper.processFiles((fileHash, fileBody) -> pullRequestImageService.createReplyImage(
      new NamespaceAndName(namespace, name), pullRequestId, commentId, replyId, fileHash, replyDto.getFiletypes().get(fileHash), fileBody
    ));
  }

  @POST
  @Path("replies")
  @Consumes(MediaType.APPLICATION_JSON)
  @Operation(summary = "Reply to pull request comment", description = "Creates a new pull request comment reply.", tags = "Pull Request Comment")
  @ApiResponse(responseCode = "204", description = "success")
  @ApiResponse(responseCode = "400", description = "Invalid body, e.g. illegal change of namespace or name")
  @ApiResponse(responseCode = "401", description = "not authenticated / invalid credentials")
  @ApiResponse(responseCode = "403", description = "not authorized, the current user does not have the \"commentPullRequest\" privilege")
  @ApiResponse(responseCode = "404", description = "not found, no comment with the specified id is available")
  @ApiResponse(
    responseCode = "500",
    description = "internal server error",
    content = @Content(
      mediaType = VndMediaType.ERROR_TYPE,
      schema = @Schema(implementation = ErrorDto.class)
    )
  )
  public Response reply(@Context UriInfo uriInfo,
                        @PathParam("namespace") String namespace,
                        @PathParam("name") String name,
                        @PathParam("pullRequestId") String pullRequestId,
                        @PathParam("commentId") String commentId,
                        @QueryParam("sourceRevision") String expectedSourceRevision,
                        @QueryParam("targetRevision") String expectedTargetRevision,
                        @Valid ReplyDto replyDto) {
    String newId = createReply(namespace, name, pullRequestId, commentId, expectedSourceRevision, expectedTargetRevision, replyDto);
    String newLocation = commentPathBuilder.createReplySelfUri(namespace, name, pullRequestId, commentId, newId);
    return Response.created(create(newLocation)).build();
  }

  private String createReply(String namespace, String name, String pullRequestId, String commentId, String expectedSourceRevision, String expectedTargetRevision, @Valid ReplyDto replyDto) {
    checkRevision(branchRevisionResolver, namespace, name, pullRequestId, expectedSourceRevision, expectedTargetRevision);
    return service.reply(namespace, name, pullRequestId, commentId, replyMapper.map(replyDto));
  }

  @POST
  @Path("replies/images")
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Operation(summary = "Reply to pull request comment and upload images with it.", description = "Creates a new pull request comment reply and upload images with it.", tags = "Pull Request Comment")
  @ApiResponse(responseCode = "204", description = "success")
  @ApiResponse(responseCode = "400", description = "Invalid body, e.g. illegal change of namespace or name")
  @ApiResponse(responseCode = "401", description = "not authenticated / invalid credentials")
  @ApiResponse(responseCode = "403", description = "not authorized, the current user does not have the \"commentPullRequest\" privilege")
  @ApiResponse(responseCode = "404", description = "not found, no comment with the specified id is available")
  @ApiResponse(
    responseCode = "500",
    description = "internal server error",
    content = @Content(
      mediaType = VndMediaType.ERROR_TYPE,
      schema = @Schema(implementation = ErrorDto.class)
    )
  )
  public Response replyWithImage(@PathParam("namespace") String namespace,
                                 @PathParam("name") String name,
                                 @PathParam("pullRequestId") String pullRequestId,
                                 @PathParam("commentId") String commentId,
                                 @QueryParam("sourceRevision") String expectedSourceRevision,
                                 @QueryParam("targetRevision") String expectedTargetRevision,
                                 @NotNull MultipartFormDataInput formInput) throws IOException {
    MultipartFormDataInputHelper<ReplyWithImageDto> formDataHelper = new MultipartFormDataInputHelper<>(formInput);
    ReplyWithImageDto replyDto = formDataHelper.extractJsonObject(ReplyWithImageDto.class, "comment");
    DtoValidator.validate(replyDto);
    String replyId = createReply(namespace, name, pullRequestId, commentId, expectedSourceRevision, expectedTargetRevision, replyDto);

    formDataHelper.processFiles((fileHash, fileBody) -> pullRequestImageService.createReplyImage(
      new NamespaceAndName(namespace, name), pullRequestId, commentId, replyId, fileHash, replyDto.getFiletypes().get(fileHash), fileBody
    ));

    String newLocation = commentPathBuilder.createReplySelfUri(namespace, name, pullRequestId, commentId, replyId);
    return Response.created(create(newLocation)).build();
  }

  @GET
  @Path("replies/images/{fileHash}")
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  @Operation(summary = "Fetch an image related to the reply", description = "Fetch an image related to the reply", tags = "Pull Request Comment")
  @ApiResponse(responseCode = "200", description = "success")
  @ApiResponse(responseCode = "401", description = "not authenticated / invalid credentials")
  @ApiResponse(responseCode = "404", description = "not found, no comment, reply or image with the specified id is available")
  @ApiResponse(
    responseCode = "500",
    description = "internal server error",
    content = @Content(
      mediaType = VndMediaType.ERROR_TYPE,
      schema = @Schema(implementation = ErrorDto.class)
    )
  )
  public Response getReplyImage(@PathParam("namespace") String namespace,
                                @PathParam("name") String name,
                                @PathParam("pullRequestId") String pullRequestId,
                                @PathParam("fileHash") String fileHash) throws IOException {
    PullRequestImageService.ImageStream image = pullRequestImageService.getPullRequestImage(new NamespaceAndName(namespace, name), pullRequestId, fileHash);
    return Response.ok(image.getImageStream().readAllBytes(), MediaType.valueOf(image.getFiletype())).build();
  }

  @GET
  @Path("transitions/{transitionId}")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(summary = "Get pull request comment transition", description = "Returns a single pull request comment transition.", tags = "Pull Request Comment")
  @ApiResponse(responseCode = "200", description = "success")
  @ApiResponse(responseCode = "401", description = "not authenticated / invalid credentials")
  @ApiResponse(responseCode = "403", description = "not authorized, the current user does not have the \"commentPullRequest\" privilege")
  @ApiResponse(responseCode = "404", description = "not found, no comment transition with the specified id is available")
  @ApiResponse(
    responseCode = "500",
    description = "internal server error",
    content = @Content(
      mediaType = VndMediaType.ERROR_TYPE,
      schema = @Schema(implementation = ErrorDto.class)
    )
  )
  public ExecutedTransitionDto getExecutedTransition(@Context UriInfo uriInfo,
                                                     @PathParam("namespace") String namespace,
                                                     @PathParam("name") String name,
                                                     @PathParam("pullRequestId") String pullRequestId,
                                                     @PathParam("commentId") String commentId,
                                                     @PathParam("transitionId") String transitionId,
                                                     @QueryParam("sourceRevision") String expectedSourceRevision,
                                                     @QueryParam("targetRevision") String expectedTargetRevision) {
    checkRevision(branchRevisionResolver, namespace, name, pullRequestId, expectedSourceRevision, expectedTargetRevision);
    Comment comment = service.get(namespace, name, pullRequestId, commentId);
    ExecutedTransition<?> executedTransition = comment
      .getExecutedTransitions()
      .stream()
      .filter(t -> transitionId.equals(t.getId()))
      .findFirst()
      .orElseThrow(() -> notFound(
        entity("transition", transitionId)
          .in(Comment.class, commentId)
          .in(PullRequest.class, pullRequestId)
          .in(new NamespaceAndName(namespace, name)))
      );
    return executedTransitionMapper.map(executedTransition, new NamespaceAndName(namespace, name), pullRequestId, comment);
  }

  @POST
  @Path("transitions")
  @Consumes(MediaType.APPLICATION_JSON)
  @Operation(summary = "Transform pull request comment", description = "Transforms a pull request comment.", tags = "Pull Request Comment")
  @ApiResponse(responseCode = "204", description = "update success")
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
  public Response transform(@Context UriInfo uriInfo,
                            @PathParam("namespace") String namespace,
                            @PathParam("name") String name,
                            @PathParam("pullRequestId") String pullRequestId,
                            @PathParam("commentId") String commentId,
                            @QueryParam("sourceRevision") String expectedSourceRevision,
                            @QueryParam("targetRevision") String expectedTargetRevision,
                            @Valid TransitionDto transitionDto) {
    checkRevision(branchRevisionResolver, namespace, name, pullRequestId, expectedSourceRevision, expectedTargetRevision);
    ExecutedTransition<CommentTransition> executedTransition = service.transform(namespace, name, pullRequestId, commentId, CommentTransition.valueOf(transitionDto.getName()));
    return Response.created(create(commentPathBuilder.createExecutedTransitionUri(namespace, name, pullRequestId, commentId, executedTransition.getId()))).build();
  }
}
