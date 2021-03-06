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
import com.cloudogu.scm.review.PermissionCheck;
import com.cloudogu.scm.review.PullRequestMediaType;
import com.cloudogu.scm.review.PullRequestResourceLinks;
import com.cloudogu.scm.review.pullrequest.dto.PullRequestCheckResultDto;
import com.cloudogu.scm.review.pullrequest.dto.PullRequestDto;
import com.cloudogu.scm.review.pullrequest.dto.PullRequestMapper;
import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import com.cloudogu.scm.review.pullrequest.service.PullRequestService;
import com.cloudogu.scm.review.pullrequest.service.PullRequestStatus;
import de.otto.edison.hal.HalRepresentation;
import de.otto.edison.hal.Links;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import sonia.scm.ScmConstraintViolationException;
import sonia.scm.api.v2.resources.ErrorDto;
import sonia.scm.repository.ChangesetPagingResult;
import sonia.scm.repository.Repository;
import sonia.scm.repository.api.RepositoryService;
import sonia.scm.repository.api.RepositoryServiceFactory;
import sonia.scm.user.User;
import sonia.scm.web.VndMediaType;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.validation.Valid;
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
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static com.cloudogu.scm.review.HalRepresentations.createCollection;
import static com.cloudogu.scm.review.pullrequest.dto.PullRequestCheckResultDto.PullRequestCheckStatus.BRANCHES_NOT_DIFFER;
import static com.cloudogu.scm.review.pullrequest.dto.PullRequestCheckResultDto.PullRequestCheckStatus.PR_ALREADY_EXISTS;
import static com.cloudogu.scm.review.pullrequest.dto.PullRequestCheckResultDto.PullRequestCheckStatus.PR_VALID;
import static java.util.Optional.ofNullable;
import static sonia.scm.AlreadyExistsException.alreadyExists;
import static sonia.scm.ContextEntry.ContextBuilder.entity;

@OpenAPIDefinition(tags = {
  @Tag(name = "Pull Request", description = "Pull request endpoints provided by the review-plugin")
})
@Path(PullRequestRootResource.PULL_REQUESTS_PATH_V2)
public class PullRequestRootResource {

  public static final String PULL_REQUESTS_PATH_V2 = "v2/pull-requests";

  private final PullRequestMapper mapper;
  private final PullRequestService service;
  private final RepositoryServiceFactory serviceFactory;
  private final Provider<PullRequestResource> pullRequestResourceProvider;

  @Inject
  public PullRequestRootResource(PullRequestMapper mapper, PullRequestService service, RepositoryServiceFactory serviceFactory, Provider<PullRequestResource> pullRequestResourceProvider) {
    this.mapper = mapper;
    this.service = service;
    this.serviceFactory = serviceFactory;
    this.pullRequestResourceProvider = pullRequestResourceProvider;
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

    String source = pullRequestDto.getSource();
    String target = pullRequestDto.getTarget();

    service.get(repository, source, target, PullRequestStatus.OPEN)
      .ifPresent(pullRequest -> {
        throw alreadyExists(entity(repository).in("pull request", pullRequest.getId()).in(repository));
      });

    service.checkBranch(repository, source);
    service.checkBranch(repository, target);

    verifyBranchesDiffer(source, target);

    User user = CurrentUserResolver.getCurrentUser();
    pullRequestDto.setStatus(PullRequestStatus.OPEN);
    PullRequest pullRequest = mapper.using(uriInfo).map(pullRequestDto);
    pullRequest.setAuthor(user.getId());

    String id = service.add(repository, pullRequest);
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
  public HalRepresentation getAll(@Context UriInfo uriInfo, @PathParam("namespace") String namespace, @PathParam("name") String name, @QueryParam("status") @DefaultValue("OPEN") PullRequestSelector pullRequestSelector) {
    Repository repository = service.getRepository(namespace, name);
    PermissionCheck.checkRead(repository);
    List<PullRequestDto> pullRequestDtos = service.getAll(namespace, name)
      .stream()
      .filter(pullRequestSelector)
      .map(pr -> mapper.using(uriInfo).map(pr, repository))
      .sorted(Comparator.comparing(this::getLastModification).reversed())
      .collect(Collectors.toList());

    PullRequestResourceLinks resourceLinks = new PullRequestResourceLinks(uriInfo::getBaseUri);
    boolean permission = PermissionCheck.mayCreate(repository);
    return
      createCollection(permission, resourceLinks.pullRequestCollection().all(namespace, name), resourceLinks.pullRequestCollection().create(namespace, name), pullRequestDtos, "pullRequests");
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
    @QueryParam("source") String source,
    @QueryParam("target") String target
  ) throws IOException {
    Repository repository = service.getRepository(namespace, name);
    PermissionCheck.checkCreate(repository);

    service.checkBranch(repository, source);
    service.checkBranch(repository, target);

    return checkIfPullRequestIsValid(uriInfo, repository, source, target);
  }

  private PullRequestCheckResultDto checkIfPullRequestIsValid(UriInfo uriInfo, Repository repository, String source, String target) throws IOException {
    Links links = createCheckResultLinks(uriInfo, repository, source, target);

    try {
      verifyBranchesDiffer(source, target);
    } catch (ScmConstraintViolationException e) {
      return BRANCHES_NOT_DIFFER.create(links);
    }

    if (service.get(repository, source, target, PullRequestStatus.OPEN).isPresent()) {
      return PR_ALREADY_EXISTS.create(links);
    }

    try (RepositoryService repositoryService = serviceFactory.create(repository)) {
      ChangesetPagingResult changesets = repositoryService
        .getLogCommand()
        .setStartChangeset(source)
        .setAncestorChangeset(target)
        .setPagingLimit(1)
        .getChangesets();

      if (changesets == null || changesets.getChangesets() == null || changesets.getChangesets().isEmpty()) {
        return BRANCHES_NOT_DIFFER.create(links);
      }
    }

    return PR_VALID.create(links);
  }

  private Links createCheckResultLinks(UriInfo uriInfo, Repository repository, String source, String target) {
    PullRequestResourceLinks pullRequestResourceLinks = new PullRequestResourceLinks(uriInfo::getBaseUri);
    String checkLink = String.format(
      "%s?source=%s&target=%s",
      pullRequestResourceLinks.pullRequestCollection().check(repository.getNamespace(), repository.getName()), source, target
    );

    return Links.linkingTo().self(checkLink).build();
  }

  private Instant getLastModification(PullRequestDto pr) {
    return ofNullable(pr.getLastModified()).orElse(pr.getCreationDate());
  }

  private void verifyBranchesDiffer(String source, String target) {
    ScmConstraintViolationException.Builder
      .doThrow()
      .violation("source branch and target branch must differ", "pullRequest", "source")
      .violation("source branch and target branch must differ", "pullRequest", "target")
      .when(source.equals(target));
  }
}
