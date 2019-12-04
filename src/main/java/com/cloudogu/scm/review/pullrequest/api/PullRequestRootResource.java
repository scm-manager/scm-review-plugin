package com.cloudogu.scm.review.pullrequest.api;

import com.cloudogu.scm.review.CurrentUserResolver;
import com.cloudogu.scm.review.PermissionCheck;
import com.cloudogu.scm.review.PullRequestMediaType;
import com.cloudogu.scm.review.PullRequestResourceLinks;
import com.cloudogu.scm.review.pullrequest.dto.PullRequestDto;
import com.cloudogu.scm.review.pullrequest.dto.PullRequestMapper;
import com.cloudogu.scm.review.pullrequest.dto.PullRequestSelector;
import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import com.cloudogu.scm.review.pullrequest.service.PullRequestService;
import com.cloudogu.scm.review.pullrequest.service.PullRequestStatus;
import sonia.scm.ScmConstraintViolationException;
import sonia.scm.repository.Repository;
import sonia.scm.user.User;

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
import java.net.URI;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static com.cloudogu.scm.review.HalRepresentations.createCollection;
import static java.util.Optional.ofNullable;
import static sonia.scm.AlreadyExistsException.alreadyExists;
import static sonia.scm.ContextEntry.ContextBuilder.entity;

@Path(PullRequestRootResource.PULL_REQUESTS_PATH_V2)
public class PullRequestRootResource {

  public static final String PULL_REQUESTS_PATH_V2 = "v2/pull-requests";

  private final PullRequestMapper mapper;
  private final PullRequestService service;
  private final Provider<PullRequestResource> pullRequestResourceProvider;

  @Inject
  public PullRequestRootResource(PullRequestMapper mapper, PullRequestService service, Provider<PullRequestResource> pullRequestResourceProvider) {
    this.mapper = mapper;
    this.service = service;
    this.pullRequestResourceProvider = pullRequestResourceProvider;
  }

  @Path("{namespace}/{name}/{pullRequestId}")
  public PullRequestResource getPullRequestResource() {
    return pullRequestResourceProvider.get();
  }

  @POST
  @Path("{namespace}/{name}")
  @Consumes(PullRequestMediaType.PULL_REQUEST)
  public Response create(@Context UriInfo uriInfo, @PathParam("namespace") String namespace, @PathParam("name") String name, @NotNull @Valid PullRequestDto pullRequestDto) {

    Repository repository = service.getRepository(namespace, name);
    PermissionCheck.checkCreate(repository);
    pullRequestDto.setStatus(PullRequestStatus.OPEN);

    String source = pullRequestDto.getSource();
    String target = pullRequestDto.getTarget();

    service.get(repository, source, target, pullRequestDto.getStatus())
      .ifPresent(pullRequest -> {
        throw alreadyExists(entity("pull request", pullRequest.getId()).in(repository));
      });
    service.checkBranch(repository, source);
    service.checkBranch(repository, source);

    verifyBranchesDiffer(source, target);

    User user = CurrentUserResolver.getCurrentUser();
    PullRequest pullRequest = mapper.using(uriInfo).map(pullRequestDto);
    pullRequest.setAuthor(user.getId());

    String id = service.add(repository, pullRequest);
    URI location = uriInfo.getAbsolutePathBuilder().path(id).build();
    return Response.created(location).build();
  }

  @GET
  @Path("{namespace}/{name}")
  @Produces(PullRequestMediaType.PULL_REQUEST)
  public Response getAll(@Context UriInfo uriInfo, @PathParam("namespace") String namespace, @PathParam("name") String name, @QueryParam("status") @DefaultValue("OPEN") PullRequestSelector pullRequestSelector) {
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
    return Response.ok(
      createCollection(permission, resourceLinks.pullRequestCollection().all(namespace, name), resourceLinks.pullRequestCollection().create(namespace, name), pullRequestDtos, "pullRequests")).build();
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
