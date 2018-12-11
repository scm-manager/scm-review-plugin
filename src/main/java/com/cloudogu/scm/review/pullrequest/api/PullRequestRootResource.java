package com.cloudogu.scm.review.pullrequest.api;

import com.cloudogu.scm.review.pullrequest.dto.PullRequestDto;
import com.cloudogu.scm.review.pullrequest.dto.PullRequestMapper;
import com.cloudogu.scm.review.pullrequest.dto.PullRequestMapperImpl;
import com.cloudogu.scm.review.pullrequest.dto.PullRequestStatusDto;
import com.cloudogu.scm.review.pullrequest.service.DefaultPullRequestService;
import com.cloudogu.scm.review.pullrequest.service.PullRequestService;
import com.cloudogu.scm.review.pullrequest.service.PullRequestStatus;
import org.apache.shiro.SecurityUtils;
import sonia.scm.ScmConstraintViolationException;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryPermissions;

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
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static com.cloudogu.scm.review.HalRepresentations.createCollection;
import static sonia.scm.AlreadyExistsException.alreadyExists;
import static sonia.scm.ContextEntry.ContextBuilder.entity;

@Path(PullRequestRootResource.PULL_REQUESTS_PATH_V2)
public class PullRequestRootResource {

  public static final String PULL_REQUESTS_PATH_V2 = "v2/pull-requests";

  private final PullRequestMapper mapper;
  private final PullRequestService service;
  private final Provider<PullRequestResource> pullRequestResourceProvider;

  @Inject
  public PullRequestRootResource(DefaultPullRequestService pullRequestService, Provider<PullRequestResource> pullRequestResourceProvider) {
    this.service = pullRequestService;
    this.pullRequestResourceProvider = pullRequestResourceProvider;
    this.mapper = new PullRequestMapperImpl();
  }

  @Path("{namespace}/{name}/{pullRequestId}")
  public PullRequestResource getPullRequestResource() {
    return pullRequestResourceProvider.get();
  }

  @POST
  @Path("{namespace}/{name}")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response create(@Context UriInfo uriInfo, @PathParam("namespace") String namespace, @PathParam("name") String name, @NotNull @Valid PullRequestDto pullRequestDto) {

    Repository repository = service.getRepository(namespace, name);
    RepositoryPermissions.push(repository).check();
    pullRequestDto.setStatus(PullRequestStatus.OPEN);
    service.get(repository, pullRequestDto.getSource(), pullRequestDto.getTarget(), pullRequestDto.getStatus())
      .ifPresent(pullRequest -> {
        throw alreadyExists(entity("pull request", pullRequest.getId()).in(repository));
      });
    service.checkBranch(repository, pullRequestDto.getSource());
    service.checkBranch(repository, pullRequestDto.getTarget());

    verifyBranchesDiffer(pullRequestDto.getSource(), pullRequestDto.getTarget());
    pullRequestDto.setAuthor(SecurityUtils.getSubject().getPrincipals().getPrimaryPrincipal().toString());
    Instant now = Instant.now();
    pullRequestDto.setCreationDate(now);
    pullRequestDto.setLastModified(now);
    String id = service.add(repository, mapper.using(uriInfo).map(pullRequestDto));
    URI location = uriInfo.getAbsolutePathBuilder().path(id).build();
    return Response.created(location).build();
  }


  @GET
  @Path("{namespace}/{name}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getAll(@Context UriInfo uriInfo, @PathParam("namespace") String namespace, @PathParam("name") String name, @QueryParam("status") @DefaultValue("ALL") PullRequestStatusDto pullRequestStatusDto) {
    Repository repository = service.getRepository(namespace, name);
    RepositoryPermissions.read(repository).check();
    List<PullRequestDto> pullRequestDtos = service.getAll(namespace, name)
      .stream()
      .filter(pullRequest -> pullRequestStatusDto == PullRequestStatusDto.ALL || pullRequest.getStatus().equals(PullRequestStatus.valueOf(pullRequestStatusDto.name())))
      .map(pr -> mapper.using(uriInfo).map(pr, repository))
      .sorted(Comparator.comparing(PullRequestDto::getLastModified).reversed())
      .collect(Collectors.toList());

    boolean permission = RepositoryPermissions.push(repository).isPermitted();
    return Response.ok(createCollection(uriInfo, permission, pullRequestDtos, "pullRequests")).build();
  }

  private void verifyBranchesDiffer(String source, String target) {
    ScmConstraintViolationException.Builder
      .doThrow()
      .violation("source branch and target branch must differ", "pullRequest", "source")
      .violation("source branch and target branch must differ", "pullRequest", "target")
      .when(source.equals(target));
  }
}
