package com.cloudogu.scm.review;

import sonia.scm.repository.NamespaceAndName;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;

@Path("v2/pull-requests")
public class PullRequestResource {

  private final PullRequestStoreFactory storeFactory;

  @Inject
  public PullRequestResource(PullRequestStoreFactory storeFactory) {
    this.storeFactory = storeFactory;
  }

  @POST
  @Path("{namespace}/{name}")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response create(@Context UriInfo uriInfo, @PathParam("namespace") String namespace, @PathParam("name") String name, PullRequest pullRequest) {
    NamespaceAndName namespaceAndName = new NamespaceAndName(namespace, name);
    PullRequestStore store = storeFactory.create(namespaceAndName);

    // TODO add author (current user)
    // TODO add creation date to pr
    // TODO validation of pull request

    String id = store.add(pullRequest);
    URI location = uriInfo.getAbsolutePathBuilder().path(id).build();
    return Response.created(location).build();
  }

}
