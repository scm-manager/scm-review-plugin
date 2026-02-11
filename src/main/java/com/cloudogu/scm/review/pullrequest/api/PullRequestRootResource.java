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

package com.cloudogu.scm.review.pullrequest.api;

import com.cloudogu.scm.review.PagedCollections;
import com.cloudogu.scm.review.PermissionCheck;
import com.cloudogu.scm.review.PullRequestMediaType;
import com.cloudogu.scm.review.PullRequestResourceLinks;
import com.cloudogu.scm.review.config.service.BasePullRequestConfig;
import com.cloudogu.scm.review.config.service.ConfigService;
import com.cloudogu.scm.review.pullrequest.dto.DisplayedUserDto;
import com.cloudogu.scm.review.pullrequest.dto.PullRequestCheckResultDto;
import com.cloudogu.scm.review.pullrequest.dto.PullRequestDto;
import com.cloudogu.scm.review.pullrequest.dto.PullRequestMapper;
import com.cloudogu.scm.review.pullrequest.dto.PullRequestTemplateDto;
import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import com.cloudogu.scm.review.pullrequest.service.PullRequestCreator;
import com.cloudogu.scm.review.pullrequest.service.PullRequestService;
import com.google.common.base.Strings;
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
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
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
import sonia.scm.api.v2.resources.ErrorDto;
import sonia.scm.repository.Changeset;
import sonia.scm.repository.NamespaceAndName;
import sonia.scm.repository.Repository;
import sonia.scm.repository.api.RepositoryService;
import sonia.scm.repository.api.RepositoryServiceFactory;
import sonia.scm.user.UserDisplayManager;
import sonia.scm.web.VndMediaType;

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
import static sonia.scm.ContextEntry.ContextBuilder.entity;
import static sonia.scm.NotFoundException.notFound;

@OpenAPIDefinition(tags = {
  @Tag(name = "Pull Request", description = "Pull request endpoints provided by the review-plugin")
})
@Path(PullRequestRootResource.PULL_REQUESTS_PATH_V2)
public class PullRequestRootResource {

  public static final String PULL_REQUESTS_PATH_V2 = "v2/pull-requests";

  private final PullRequestMapper mapper;
  private final PullRequestService service;
  private final PullRequestCreator pullRequestCreator;
  private final RepositoryServiceFactory serviceFactory;
  private final Provider<PullRequestResource> pullRequestResourceProvider;
  private final ConfigService configService;

  private final UserDisplayManager userDisplayManager;

  @Inject
  public PullRequestRootResource(PullRequestMapper mapper, PullRequestService service, PullRequestCreator pullRequestCreator, RepositoryServiceFactory serviceFactory, Provider<PullRequestResource> pullRequestResourceProvider, ConfigService configService, UserDisplayManager userDisplayManager) {
    this.mapper = mapper;
    this.service = service;
    this.pullRequestCreator = pullRequestCreator;
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
      null,
      title,
      description,
      getDefaultReviewers(repository),
      config.getLabels(),
      config.getDefaultTasks(),
      config.isDeleteBranchOnMerge());
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
    String id = pullRequestCreator.openNewPullRequest(
      namespace,
      name,
      mapper.using(uriInfo).map(pullRequestDto),
      pullRequestDto.getInitialTasks()
    );
    URI location = uriInfo.getAbsolutePathBuilder().path(id).build();
    return Response.created(location).build();
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
    @DefaultValue("10") @QueryParam("pageSize") int pageSize,
    @QueryParam("source") String source,
    @QueryParam("target") String target
  ) {
    Repository repository = service.getRepository(namespace, name);
    PermissionCheck.checkRead(repository);
    int totalCount = service.count(namespace, name, pullRequestSelector);
    int pageCount = (int) Math.ceil((double) totalCount / pageSize);

    PullRequestResourceLinks resourceLinks = new PullRequestResourceLinks(uriInfo::getBaseUri);
    NumberedPaging paging = zeroBasedNumberedPaging(page, pageSize, totalCount);
    Links.Builder linkBuilder = PagedCollections.createPagedSelfLinks(paging, resourceLinks.pullRequestCollection()
      .all(namespace, name));
    if (PermissionCheck.mayCreate(repository)) {
      linkBuilder.single(link("create", resourceLinks.pullRequestCollection().create(namespace, name)));
    }

    if (totalCount == 0 || page >= pageCount) {
      return Response.ok(
        PagedCollections.createPagedCollection(
          createHalRepresentation(linkBuilder, Collections.emptyList()),
          page,
          0
        )).build();
    }

    List<PullRequest> pullRequests =
      service.getAll(
        namespace,
        name,
        new RequestParameters(pullRequestSelector, pullRequestSortSelector, page * pageSize, pageSize, source, target)
      );

    List<PullRequestDto> pullRequestDtos = pullRequests
      .stream()
      .map(pr -> mapper.using(uriInfo).map(pr, repository))
      .toList();

    return Response.ok(
      PagedCollections.createPagedCollection(
        createHalRepresentation(linkBuilder, pullRequestDtos),
        page,
        pageCount
      )).build();
  }

  private HalRepresentation createHalRepresentation(Links.Builder linkBuilder, List<PullRequestDto> pullRequestDtos) {
    return new HalRepresentation(linkBuilder.build(), Embedded.embedded("pullRequests", pullRequestDtos));
  }

  @GET
  @Path("{namespace}/{name}/check")
  @Produces(PullRequestMediaType.PULL_REQUEST_CHECK_RESULT)
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
  ) {
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
}
