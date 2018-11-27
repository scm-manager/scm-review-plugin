package com.cloudogu.scm.review;

import org.apache.shiro.SecurityUtils;
import sonia.scm.ScmConstraintViolationException;
import sonia.scm.repository.NamespaceAndName;
import sonia.scm.repository.Repository;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;

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
  public Response create(@Context UriInfo uriInfo, @PathParam("namespace") String namespace, @PathParam("name") String name, @NotNull @Valid PullRequest pullRequest) {
    NamespaceAndName namespaceAndName = new NamespaceAndName(namespace, name);
    Repository repository = repositoryResolver.resolve(namespaceAndName);

    verifyBranchExists(repository, pullRequest.getSource());
    verifyBranchExists(repository, pullRequest.getTarget());

    verifyBranchesDiffer(pullRequest.getSource(), pullRequest.getTarget());

    PullRequestStore store = storeFactory.create(repository);

    String author = SecurityUtils.getSubject().getPrincipals().getPrimaryPrincipal().toString();
    pullRequest.setAuthor(author);
    String id = store.add(repository, pullRequest);
    URI location = uriInfo.getAbsolutePathBuilder().path(id).build();
    return Response.created(location).build();
  }

  @GET
  @Path("{namespace}/{name}/{id}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response get(@Context UriInfo uriInfo, @PathParam("namespace") String namespace, @PathParam("name") String name, @PathParam("id") String id) {
    NamespaceAndName namespaceAndName = new NamespaceAndName(namespace, name);
    Repository repository = repositoryResolver.resolve(namespaceAndName);

    PullRequestStore pullRequestStore = storeFactory.create(repository);
    PullRequest pullRequest = pullRequestStore.get(repository, id);
    URI location = uriInfo.getAbsolutePathBuilder().build();
    return Response.ok(mapper.map(pullRequest, location)).build();

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
