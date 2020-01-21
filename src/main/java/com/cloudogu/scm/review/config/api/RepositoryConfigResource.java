package com.cloudogu.scm.review.config.api;

import com.cloudogu.scm.review.PermissionCheck;
import com.cloudogu.scm.review.config.service.ConfigService;
import com.cloudogu.scm.review.pullrequest.api.PullRequestRootResource;
import sonia.scm.repository.NamespaceAndName;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryManager;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

import static sonia.scm.ContextEntry.ContextBuilder.entity;
import static sonia.scm.NotFoundException.notFound;

@Path(PullRequestRootResource.PULL_REQUESTS_PATH_V2)
public class RepositoryConfigResource {

  private final ConfigService configService;
  private final RepositoryConfigMapper repositoryConfigMapper;
  private final RepositoryManager repositoryManager;

  @Inject
  public RepositoryConfigResource(ConfigService configService, RepositoryConfigMapper repositoryConfigMapper, RepositoryManager repositoryManager) {
    this.configService = configService;
    this.repositoryConfigMapper = repositoryConfigMapper;
    this.repositoryManager = repositoryManager;
  }

  @GET
  @Path("{namespace}/{name}/config")
  @Produces(MediaType.APPLICATION_JSON)
  public PullRequestConfigDto getRepositoryConfig(@Context UriInfo uriInfo, @PathParam("namespace") String namespace, @PathParam("name") String name) {
    Repository repository = repositoryManager.get(new NamespaceAndName(namespace, name));
    if (repository == null) {
      throw notFound(entity(new NamespaceAndName(namespace, name)));
    }
    PermissionCheck.checkConfigure(repository);
    return repositoryConfigMapper.map(configService.getRepositoryPullRequestConfig(repository), repository, uriInfo);
  }

  @PUT
  @Path("{namespace}/{name}/config")
  @Consumes(MediaType.APPLICATION_JSON)
  public void setRepositoryConfig(@PathParam("namespace") String namespace, @PathParam("name") String name, @Valid PullRequestConfigDto configDto) {
    Repository repository = repositoryManager.get(new NamespaceAndName(namespace, name));
    if (repository == null) {
      throw notFound(entity(new NamespaceAndName(namespace, name)));
    }
    PermissionCheck.checkConfigure(repository);
    configService.setRepositoryPullRequestConfig(repository, repositoryConfigMapper.map(configDto));
  }
}
