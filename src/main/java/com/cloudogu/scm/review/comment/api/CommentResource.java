package com.cloudogu.scm.review.comment.api;


import com.cloudogu.scm.review.PullRequestMediaType;
import com.cloudogu.scm.review.RepositoryResolver;
import com.cloudogu.scm.review.comment.service.Comment;
import com.cloudogu.scm.review.comment.service.CommentService;
import com.cloudogu.scm.review.comment.service.CommentTransition;
import com.cloudogu.scm.review.comment.service.ExecutedTransition;
import com.cloudogu.scm.review.comment.service.Reply;
import com.cloudogu.scm.review.pullrequest.dto.BranchRevisionResolver;
import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import sonia.scm.NotFoundException;
import sonia.scm.api.v2.resources.ErrorDto;
import sonia.scm.repository.NamespaceAndName;
import sonia.scm.repository.Repository;
import sonia.scm.web.VndMediaType;

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
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.Collection;

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
  private BranchRevisionResolver branchRevisionResolver;

  @Inject
  public CommentResource(CommentService service,
                         RepositoryResolver repositoryResolver,
                         CommentMapper commentMapper,
                         ReplyMapper replyMapper,
                         CommentPathBuilder commentPathBuilder,
                         ExecutedTransitionMapper executedTransitionMapper,
                         BranchRevisionResolver branchRevisionResolver) {
    this.repositoryResolver = repositoryResolver;
    this.service = service;
    this.commentMapper = commentMapper;
    this.replyMapper = replyMapper;
    this.commentPathBuilder = commentPathBuilder;
    this.executedTransitionMapper = executedTransitionMapper;
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
                               @PathParam("commentId") String commentId) {
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
                           @PathParam("replyId") String replyId) {
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
                            @PathParam("commentId") String commentId) {
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
                          @PathParam("replyId") String replyId) {
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
                            @Valid CommentDto commentDto) {
    service.modifyComment(namespace, name, pullRequestId, commentId, commentMapper.map(commentDto));
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
                          @Valid ReplyDto replyDto) {
    service.modifyReply(namespace, name, pullRequestId, replyId, replyMapper.map(replyDto));
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
    RevisionChecker.checkRevision(branchRevisionResolver, namespace, name, pullRequestId, expectedSourceRevision, expectedTargetRevision);
    String newId = service.reply(namespace, name, pullRequestId, commentId, replyMapper.map(replyDto));
    String newLocation = commentPathBuilder.createReplySelfUri(namespace, name, pullRequestId, commentId, newId);
    return Response.created(create(newLocation)).build();
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
                                                     @PathParam("transitionId") String transitionId) {
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
                            @Valid TransitionDto transitionDto) {
    ExecutedTransition<CommentTransition> executedTransition = service.transform(namespace, name, pullRequestId, commentId, CommentTransition.valueOf(transitionDto.getName()));
    return Response.created(create(commentPathBuilder.createExecutedTransitionUri(namespace, name, pullRequestId, commentId, executedTransition.getId()))).build();
  }
}
