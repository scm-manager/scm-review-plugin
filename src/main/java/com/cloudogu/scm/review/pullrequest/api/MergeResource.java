package com.cloudogu.scm.review.pullrequest.api;

import sonia.scm.repository.api.MergeStrategy;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

@Path(MergeResource.MERGE_PATH_V2)
public class MergeResource {

  static final String MERGE_PATH_V2 = "v2/merge";

  public MergeResource() {
  }

  @POST
  @Path("")
  public Response merge(@PathParam("namespace") String namespace, @PathParam("name") String name, @QueryParam("strategy") MergeStrategy strategy) {
    return Response.noContent().build();
  }

}
