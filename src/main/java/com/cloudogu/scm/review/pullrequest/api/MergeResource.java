package com.cloudogu.scm.review.pullrequest.api;

import com.cloudogu.scm.review.pullrequest.dto.MergeCommitDto;
import com.cloudogu.scm.review.pullrequest.service.MergeService;
import com.webcohesion.enunciate.metadata.rs.ResponseCode;
import com.webcohesion.enunciate.metadata.rs.StatusCodes;
import sonia.scm.ConcurrentModificationException;
import sonia.scm.api.v2.resources.MergeCommandDto;
import sonia.scm.api.v2.resources.MergeResultToDtoMapper;
import sonia.scm.repository.NamespaceAndName;
import sonia.scm.repository.api.MergeCommandResult;
import sonia.scm.repository.api.MergeDryRunCommandResult;
import sonia.scm.repository.api.MergeStrategy;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
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
  @Produces("application/json")
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

  @POST
  @Path("{namespace}/{name}/dry-run")
  @Consumes("application/vnd.scmm-mergeCommand+json")
  @StatusCodes({
    @ResponseCode(code = 204, condition = "merge can be done automatically"),
    @ResponseCode(code = 401, condition = "not authenticated / invalid credentials"),
    @ResponseCode(code = 409, condition = "The branches can not be merged automatically due to conflicts"),
    @ResponseCode(code = 500, condition = "internal server error")
  })
  public Response dryRun(
    @PathParam("namespace") String namespace,
    @PathParam("name") String name,
    @Valid MergeCommandDto mergeCommandDto
  ) {
    MergeDryRunCommandResult mergeDryRunCommandResult = service.dryRun(new NamespaceAndName(namespace, name), mergeCommandDto);
      if (mergeDryRunCommandResult.isMergeable()) {
        return Response.noContent().build();
      } else {
        throw new ConcurrentModificationException("revision", mergeCommandDto.getTargetRevision());
      }
    }
}
