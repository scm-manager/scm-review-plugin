package com.cloudogu.scm.review;

import org.apache.shiro.SecurityUtils;
import sonia.scm.ScmConstraintViolationException;
import sonia.scm.repository.NamespaceAndName;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryPermissions;

import javax.inject.Inject;
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
import java.time.Instant;
import java.util.Comparator;
import java.util.stream.Collectors;

@Path(PullRequestResource.PULL_REQUESTS_PATH_V2)
public class PullRequestResource {

  public static final String PULL_REQUESTS_PATH_V2 = "v2/pull-requests";

  private final RepositoryResolver repositoryResolver;
  private final BranchResolver branchResolver;
  private final PullRequestStoreFactory storeFactory;
  private final PullRequestToPullRequestDtoMapper mapper;

  @Inject
  public PullRequestResource(RepositoryResolver repositoryResolver, BranchResolver branchResolver, PullRequestStoreFactory storeFactory) {
    this.repositoryResolver = repositoryResolver;
    this.branchResolver = branchResolver;
    this.storeFactory = storeFactory;
    this.mapper = new PullRequestToPullRequestDtoMapperImpl();
  }

  @POST
  @Path("{namespace}/{name}")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response create(@Context UriInfo uriInfo, @PathParam("namespace") String namespace, @PathParam("name") String name, @NotNull @Valid PullRequestDto pullRequestDto) {

    Repository repository = getRepository(namespace, name);

    verifyBranchExists(repository, pullRequestDto.getSource());
    verifyBranchExists(repository, pullRequestDto.getTarget());

    verifyBranchesDiffer(pullRequestDto.getSource(), pullRequestDto.getTarget());

    PullRequestStore store = storeFactory.create(repository);

    String author = SecurityUtils.getSubject().getPrincipals().getPrimaryPrincipal().toString();
    pullRequestDto.setAuthor(author);
    pullRequestDto.setStatus(PullRequestStatus.OPEN);
    Instant now = Instant.now();
    pullRequestDto.setCreationDate(now);
    pullRequestDto.setLastModified(now);
    String id = store.add(mapper.map(pullRequestDto));
    URI location = uriInfo.getAbsolutePathBuilder().path(id).build();
    return Response.created(location).build();
  }

  @GET
  @Path("{namespace}/{name}/{id}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response get(@Context UriInfo uriInfo, @PathParam("namespace") String namespace, @PathParam("name") String name, @PathParam("id") String id) {
    PullRequestStore pullRequestStore = storeFactory.create(getRepository(namespace, name));
    PullRequest pullRequest = pullRequestStore.get(id);
    URI location = uriInfo.getAbsolutePathBuilder().build();
    return Response.ok(mapper.map(pullRequest, location)).build();
  }

  @GET
  @Path("{namespace}/{name}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getAll(@Context UriInfo uriInfo, @PathParam("namespace") String namespace, @PathParam("name") String name, @QueryParam("status") PullRequestStatusDto pullRequestStatusDto) {
    return Response.ok(storeFactory.create(getRepository(namespace, name)).getAll()
      .stream()
      .filter(pullRequest -> pullRequestStatusDto == null || pullRequestStatusDto == PullRequestStatusDto.ALL || pullRequest.getStatus().equals(PullRequestStatus.valueOf(pullRequestStatusDto.name())))
      .map(pr -> mapper.map(pr, uriInfo.getAbsolutePathBuilder().path(pr.getId()).build()))
      .sorted(Comparator.comparing(PullRequestDto::getLastModified).reversed())
      .collect(Collectors.toList())
    ).build();
  }

  private Repository getRepository(String namespace, String name) {
    NamespaceAndName namespaceAndName = new NamespaceAndName(namespace, name);
    Repository repository = repositoryResolver.resolve(namespaceAndName);
    RepositoryPermissions.read(repository).check();
    return repository;
  }

  private void verifyBranchExists(Repository repository, String branchName) {
    branchResolver.resolve(repository, branchName);
  }

  private void verifyBranchesDiffer(String source, String target) {
    ScmConstraintViolationException.Builder
      .doThrow()
      .violation("source branch and target branch must differ", "pullRequest", "source")
      .violation("source branch and target branch must differ", "pullRequest", "target")
      .when(source.equals(target));
  }
}
