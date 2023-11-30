/*
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
package com.cloudogu.scm.review.pullrequest.api;

import com.cloudogu.scm.review.CurrentUserResolver;
import com.cloudogu.scm.review.PagedCollections;
import com.cloudogu.scm.review.PermissionCheck;
import com.cloudogu.scm.review.PullRequestMediaType;
import com.cloudogu.scm.review.PullRequestResourceLinks;
import com.cloudogu.scm.review.comment.service.Comment;
import com.cloudogu.scm.review.comment.service.CommentService;
import com.cloudogu.scm.review.comment.service.CommentType;
import com.cloudogu.scm.review.config.service.BasePullRequestConfig;
import com.cloudogu.scm.review.config.service.ConfigService;
import com.cloudogu.scm.review.pullrequest.dto.DisplayedUserDto;
import com.cloudogu.scm.review.pullrequest.dto.PullRequestCheckResultDto;
import com.cloudogu.scm.review.pullrequest.dto.PullRequestDto;
import com.cloudogu.scm.review.pullrequest.dto.PullRequestMapper;
import com.cloudogu.scm.review.pullrequest.dto.PullRequestTemplateDto;
import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import com.cloudogu.scm.review.pullrequest.service.PullRequestService;
import com.cloudogu.scm.review.pullrequest.service.PullRequestStatus;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import de.otto.edison.hal.Embedded;
import de.otto.edison.hal.HalRepresentation;
import de.otto.edison.hal.Links;
import de.otto.edison.hal.paging.NumberedPaging;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import sonia.scm.api.v2.resources.ErrorDto;
import sonia.scm.repository.Changeset;
import sonia.scm.repository.NamespaceAndName;
import sonia.scm.repository.Repository;
import sonia.scm.repository.api.RepositoryService;
import sonia.scm.repository.api.RepositoryServiceFactory;
import sonia.scm.user.User;
import sonia.scm.user.UserDisplayManager;
import sonia.scm.web.VndMediaType;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
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
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.cloudogu.scm.review.pullrequest.PullRequestUtil.PullRequestTitleAndDescription;
import static com.cloudogu.scm.review.pullrequest.PullRequestUtil.determineTitleAndDescription;
import static de.otto.edison.hal.Link.link;
import static de.otto.edison.hal.Links.linkingTo;
import static de.otto.edison.hal.paging.NumberedPaging.zeroBasedNumberedPaging;
import static sonia.scm.AlreadyExistsException.alreadyExists;
import static sonia.scm.ContextEntry.ContextBuilder.entity;
import static sonia.scm.NotFoundException.notFound;
import static sonia.scm.ScmConstraintViolationException.Builder.doThrow;

@OpenAPIDefinition(tags = {
  @Tag(name = "Pull Request", description = "Pull request endpoints provided by the review-plugin")
})
@Path(PullRequestRootResource.PULL_REQUESTS_PATH_V2)
public class PullRequestRootResource {

  public static final String PULL_REQUESTS_PATH_V2 = "v2/pull-requests";

  private final PullRequestMapper mapper;
  private final PullRequestService service;
  private final CommentService commentService;
  private final RepositoryServiceFactory serviceFactory;
  private final Provider<PullRequestResource> pullRequestResourceProvider;
  private final ConfigService configService;

  private final UserDisplayManager userDisplayManager;

  @Inject
  public PullRequestRootResource(PullRequestMapper mapper, PullRequestService service, CommentService commentService, RepositoryServiceFactory serviceFactory, Provider<PullRequestResource> pullRequestResourceProvider, ConfigService configService, UserDisplayManager userDisplayManager) {
    this.mapper = mapper;
    this.service = service;
    this.commentService = commentService;
    this.serviceFactory = serviceFactory;
    this.pullRequestResourceProvider = pullRequestResourceProvider;
    this.configService = configService;
    this.userDisplayManager = userDisplayManager;
  }

  @GET
  @Path("{namespace}/{name}/template")
  @Produces(MediaType.APPLICATION_JSON)
  public PullRequestTemplateDto getPullRequestTemplate(@Context UriInfo uriInfo, @PathParam("namespace") String namespace, @PathParam("name") String name, @QueryParam("source") String source, @QueryParam("target") String target) throws IOException {
    Repository repository = service.getRepository(namespace, name);
    if (repository == null) {
      throw notFound(entity(new NamespaceAndName(namespace, name)));
    }
    String description = "";
    String title = "";
    if (!Strings.isNullOrEmpty(source) && !Strings.isNullOrEmpty(target)) {
      try (RepositoryService repositoryService = serviceFactory.create(repository)) {
        List<Changeset> changesets = repositoryService.getLogCommand().setAncestorChangeset(target).setStartChangeset(source).getChangesets().getChangesets();
        if (changesets.size() == 1) {
          PullRequestTitleAndDescription titleAndDescription = determineTitleAndDescription(changesets.get(0).getDescription());
          description = titleAndDescription.getDescription();
          title = titleAndDescription.getTitle();
        }
      }
    }
    PullRequestResourceLinks pullRequestResourceLinks = new PullRequestResourceLinks(uriInfo::getBaseUri);
    BasePullRequestConfig config = configService.evaluateConfig(repository);
    return new PullRequestTemplateDto(
      linkingTo().self(pullRequestResourceLinks.pullRequestCollection().template(namespace, name)).build(),
      null, title, description, getDefaultReviewers(repository),
      config.getLabels(), config.getDefaultTasks());
  }

  private Set<DisplayedUserDto> getDefaultReviewers(Repository repository) {
    return configService
      .evaluateConfig(repository)
      .getDefaultReviewers()
      .stream()
      .map(this::getDisplayedUserDto)
      .collect(Collectors.toSet());
  }

  private DisplayedUserDto getDisplayedUserDto(String userId) {
    return userDisplayManager.get(userId)
      .map(displayUser -> new DisplayedUserDto(displayUser.getId(), displayUser.getDisplayName(), displayUser.getMail()))
      .orElse(new DisplayedUserDto(userId, userId, null));
  }

  @Path("{namespace}/{name}/{pullRequestId}")
  public PullRequestResource getPullRequestResource() {
    return pullRequestResourceProvider.get();
  }

  @POST
  @Path("{namespace}/{name}")
  @Consumes(PullRequestMediaType.PULL_REQUEST)
  @Operation(
    summary = "Create pull request",
    description = "Creates a new pull request.",
    tags = "Pull Request",
    operationId = "review_create_pull_request"
  )
  @ApiResponse(responseCode = "201", description = "create success")
  @ApiResponse(responseCode = "401", description = "not authenticated / invalid credentials")
  @ApiResponse(responseCode = "403", description = "not authorized, the current user does not have the \"createPullRequest\" privilege")
  @ApiResponse(responseCode = "409", description = "conflict, a similar pull request for these branches already exists")
  @ApiResponse(
    responseCode = "500",
    description = "internal server error",
    content = @Content(
      mediaType = VndMediaType.ERROR_TYPE,
      schema = @Schema(implementation = ErrorDto.class)
    )
  )
  public Response create(@Context UriInfo uriInfo, @PathParam("namespace") String namespace, @PathParam("name") String name, @NotNull @Valid PullRequestDto pullRequestDto) {
    Repository repository = service.getRepository(namespace, name);
    PermissionCheck.checkCreate(repository);

    if (pullRequestDto.getStatus() == null) {
      pullRequestDto.setStatus(PullRequestStatus.OPEN);
    } else {
      doThrow().violation("illegal status", "pullRequest", "status")
        .when(pullRequestDto.getStatus().isClosed());
    }

    String source = pullRequestDto.getSource();
    String target = pullRequestDto.getTarget();

    service.getInProgress(repository, source, target)
      .ifPresent(pullRequest -> {
        throw alreadyExists(entity(repository).in("pull request", pullRequest.getId()).in(repository));
      });

    service.checkBranch(repository, source);
    service.checkBranch(repository, target);

    verifyBranchesDiffer(source, target);

    User user = CurrentUserResolver.getCurrentUser();

    PullRequest pullRequest = mapper.using(uriInfo).map(pullRequestDto);
    pullRequest.setAuthor(user.getId());

    String id = service.add(repository, pullRequest);

    createInitialTasks(namespace, name, id, pullRequestDto.getInitialTasks());
    URI location = uriInfo.getAbsolutePathBuilder().path(id).build();
    return Response.created(location).build();
  }

  private void createInitialTasks(String namespace, String name, String id, List<String> initialTasks) {
    for (String task : initialTasks) {
      Comment comment = new Comment();
      comment.setComment(task);
      comment.setType(CommentType.TASK_TODO);
      commentService.add(namespace, name, id, comment);
    }
  }

  @GET
  @Path("{namespace}/{name}")
  @Produces(PullRequestMediaType.PULL_REQUEST_COLLECTION)
  @Operation(
    summary = "Collection of pull requests",
    description = "Returns a list of pull requests by status.",
    tags = "Pull Request",
    operationId = "review_get_pull_request_collection"
  )
  @ApiResponse(
    responseCode = "200",
    description = "success",
    content = @Content(
      mediaType = PullRequestMediaType.PULL_REQUEST_COLLECTION,
      schema = @Schema(implementation = HalRepresentation.class)
    )
  )
  @ApiResponse(responseCode = "401", description = "not authenticated / invalid credentials")
  @ApiResponse(responseCode = "403", description = "not authorized, the current user does not have the \"readPullRequest\" privilege")
  @ApiResponse(
    responseCode = "500",
    description = "internal server error",
    content = @Content(
      mediaType = VndMediaType.ERROR_TYPE,
      schema = @Schema(implementation = ErrorDto.class)
    )
  )
  public Response getAll(
    @Context UriInfo uriInfo,
    @PathParam("namespace") String namespace,
    @PathParam("name") String name,
    @QueryParam("status") @DefaultValue("IN_PROGRESS") PullRequestSelector pullRequestSelector,
    @QueryParam("sortBy") @DefaultValue("LAST_MOD_DESC") PullRequestSortSelector pullRequestSortSelector,
    @DefaultValue("0") @QueryParam("page") int page,
    @DefaultValue("10") @QueryParam("pageSize") int pageSize
  ) {
    Repository repository = service.getRepository(namespace, name);
    PermissionCheck.checkRead(repository);
    List<PullRequestDto> pullRequestDtos = service.getAll(namespace, name)
      .stream()
      .filter(pullRequestSelector)
      .map(pr -> mapper.using(uriInfo).map(pr, repository))
      .sorted(pullRequestSortSelector.compare())
      .collect(Collectors.toList());

    PullRequestResourceLinks resourceLinks = new PullRequestResourceLinks(uriInfo::getBaseUri);
    NumberedPaging paging = zeroBasedNumberedPaging(page, pageSize, pullRequestDtos.size());
    Links.Builder linkBuilder = PagedCollections.createPagedSelfLinks(paging, resourceLinks.pullRequestCollection()
      .all(namespace, name));

    if (PermissionCheck.mayCreate(repository)) {
      linkBuilder.single(link("create", resourceLinks.pullRequestCollection().create(namespace, name)));
    }
    List<List<PullRequestDto>> pagedPullRequestDtos = Lists.partition(pullRequestDtos, pageSize);
    if (pullRequestDtos.isEmpty() || page >= pagedPullRequestDtos.size()) {
      return Response.ok(
        PagedCollections.createPagedCollection(
          createHalRepresentation(linkBuilder, Collections.emptyList()),
          page,
          0
        )).build();
    }

    return Response.ok(
      PagedCollections.createPagedCollection(
        createHalRepresentation(linkBuilder, pagedPullRequestDtos.get(page)),
        page,
        pagedPullRequestDtos.size()
      )).build();
  }

  public Response getAll(
    UriInfo uriInfo,
    String namespace,
    String name,
    PullRequestSelector pullRequestSelector,
    PullRequestSortSelector pullRequestSortSelector
  ) {
    return getAll(uriInfo, namespace, name, pullRequestSelector, pullRequestSortSelector, 0, 9999);
  }

  private HalRepresentation createHalRepresentation(Links.Builder linkBuilder, List<PullRequestDto> pullRequestDtos) {
    return new HalRepresentation(linkBuilder.build(), Embedded.embedded("pullRequests", pullRequestDtos));
  }

  @GET
  @Path("{namespace}/{name}/check")
  @Produces(PullRequestMediaType.PULL_REQUEST)
  @Operation(
    summary = "Checks pull request",
    description = "Checks if new pull request can be created.",
    tags = "Pull Request",
    operationId = "review_check_pull_request"
  )
  @ApiResponse(responseCode = "200", description = "Returns pull request check result")
  @ApiResponse(responseCode = "400", description = "Invalid request / the provided source branch or target branch may not exist")
  @ApiResponse(responseCode = "401", description = "not authenticated / invalid credentials")
  @ApiResponse(responseCode = "403", description = "not authorized, the current user does not have the \"createPullRequest\" privilege")
  @ApiResponse(
    responseCode = "500",
    description = "internal server error",
    content = @Content(
      mediaType = VndMediaType.ERROR_TYPE,
      schema = @Schema(implementation = ErrorDto.class)
    )
  )
  public PullRequestCheckResultDto check(
    @Context UriInfo uriInfo,
    @PathParam("namespace") String namespace,
    @PathParam("name") String name,
    @NotEmpty @QueryParam("source") String source,
    @NotEmpty @QueryParam("target") String target
  ) throws IOException {
    Repository repository = service.getRepository(namespace, name);
    PermissionCheck.checkCreate(repository);

    service.checkBranch(repository, source);
    service.checkBranch(repository, target);

    return service.checkIfPullRequestIsValid(repository, source, target)
      .create(createCheckResultLinks(uriInfo, repository, source, target));
  }

  private Links createCheckResultLinks(UriInfo uriInfo, Repository repository, String source, String target) {
    PullRequestResourceLinks pullRequestResourceLinks = new PullRequestResourceLinks(uriInfo::getBaseUri);
    String checkLink = String.format(
      "%s?source=%s&target=%s",
      pullRequestResourceLinks.pullRequestCollection().check(repository.getNamespace(), repository.getName()), source, target
    );

    return Links.linkingTo().self(checkLink).build();
  }

  private void verifyBranchesDiffer(String source, String target) {
    doThrow()
      .violation("source branch and target branch must differ", "pullRequest", "source")
      .violation("source branch and target branch must differ", "pullRequest", "target")
      .when(source.equals(target));
  }
}
