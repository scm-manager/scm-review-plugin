package com.cloudogu.scm.review;

import org.apache.shiro.SecurityUtils;
import sonia.scm.repository.NamespaceAndName;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.time.Instant;

@Path(PullRequestResource.PULL_REQUESTS_PATH_V2)
public class PullRequestResource {

  public static final String PULL_REQUESTS_PATH_V2 = "v2/pull-requests";

  private final PullRequestStoreFactory storeFactory;

  @Inject
  public PullRequestResource(PullRequestStoreFactory storeFactory) {
    this.storeFactory = storeFactory;
  }

  @POST
  @Path("{namespace}/{name}")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response create(@Context UriInfo uriInfo, @PathParam("namespace") String namespace, @PathParam("name") String name, @NotNull @Valid PullRequest pullRequest) {
    NamespaceAndName namespaceAndName = new NamespaceAndName(namespace, name);
    PullRequestStore store = storeFactory.create(namespaceAndName);

    String author = SecurityUtils.getSubject().getPrincipals().getPrimaryPrincipal().toString();
    pullRequest.setAuthor(author);
    pullRequest.setCreationDate(Instant.now());
    String id = store.add(pullRequest);
    URI location = uriInfo.getAbsolutePathBuilder().path(id).build();
    return Response.created(location).build();
  }

}
