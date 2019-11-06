package com.cloudogu.scm.review.pullrequest.api;

import com.cloudogu.scm.review.pullrequest.dto.MergeCommitDto;
import com.cloudogu.scm.review.pullrequest.service.MergeService;
import sonia.scm.api.v2.resources.MergeResultToDtoMapper;
import sonia.scm.repository.NamespaceAndName;
import sonia.scm.repository.api.MergeCommandResult;
import sonia.scm.repository.api.MergeStrategy;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

@Path(MergeResource.MERGE_PATH_V2)
public class MergeResource {

  static final String MERGE_PATH_V2 = "v2/merge";
  private final MergeService service;
  private MergeResultToDtoMapper mergeResultToDtoMapper;

  @Inject
  public MergeResource(MergeService service, MergeResultToDtoMapper mergeResultToDtoMapper) {
    this.service = service;
    this.mergeResultToDtoMapper = mergeResultToDtoMapper;
  }

  @POST
  @Path("{namespace}/{name}")
  @Consumes("application/vnd.scmm-mergeCommand+json")
  public Response merge(
    @PathParam("namespace") String namespace,
    @PathParam("name") String name,
    @QueryParam("strategy") MergeStrategy strategy,
    @NotNull @Valid MergeCommitDto mergeCommitDto
  ) {
    MergeCommandResult result = service.merge(new NamespaceAndName(namespace, name), mergeCommitDto, strategy);
    if (result.isSuccess()) {
      return Response.noContent().build();
    } else {
      return Response.status(409).entity(mergeResultToDtoMapper.map(result)).build();
    }
  }
}
