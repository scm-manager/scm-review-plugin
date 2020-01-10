package com.cloudogu.scm.review.pullrequest.api;

import com.cloudogu.scm.review.pullrequest.dto.MergeCommitDto;
import com.cloudogu.scm.review.pullrequest.service.MergeService;
import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import com.webcohesion.enunciate.metadata.rs.ResponseCode;
import com.webcohesion.enunciate.metadata.rs.StatusCodes;
import sonia.scm.ConcurrentModificationException;
import sonia.scm.repository.NamespaceAndName;
import sonia.scm.repository.api.MergeDryRunCommandResult;
import sonia.scm.repository.api.MergeStrategy;
import sonia.scm.repository.spi.MergeConflictResult;

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

import static sonia.scm.ContextEntry.ContextBuilder.entity;

@Path(MergeResource.MERGE_PATH_V2)
public class MergeResource {

  static final String MERGE_PATH_V2 = "v2/merge";
  private final MergeService service;

  @Inject
  public MergeResource(MergeService service) {
    this.service = service;
  }

  @POST
  @Path("{namespace}/{name}/{pullRequestId}")
  @Consumes("application/vnd.scmm-mergeCommand+json")
  @Produces("application/json")
  public void merge(
    @PathParam("namespace") String namespace,
    @PathParam("name") String name,
    @PathParam("pullRequestId") String pullRequestId,
    @QueryParam("strategy") MergeStrategy strategy,
    @NotNull @Valid MergeCommitDto mergeCommitDto
  ) {
    NamespaceAndName namespaceAndName = new NamespaceAndName(namespace, name);
    service.merge(namespaceAndName, pullRequestId, mergeCommitDto, strategy);
  }

  @POST
  @Path("{namespace}/{name}/{pullRequestId}/dry-run")
  @StatusCodes({
    @ResponseCode(code = 204, condition = "merge can be done automatically"),
    @ResponseCode(code = 401, condition = "not authenticated / invalid credentials"),
    @ResponseCode(code = 409, condition = "The branches can not be merged automatically due to conflicts"),
    @ResponseCode(code = 500, condition = "internal server error")
  })
  public void dryRun(
    @PathParam("namespace") String namespace,
    @PathParam("name") String name,
    @PathParam("pullRequestId") String pullRequestId
  ) {
    NamespaceAndName namespaceAndName = new NamespaceAndName(namespace, name);
    MergeDryRunCommandResult mergeDryRunCommandResult = service.dryRun(namespaceAndName, pullRequestId);
    if (!mergeDryRunCommandResult.isMergeable()) {
      throw new ConcurrentModificationException(entity(PullRequest.class, pullRequestId).in(namespaceAndName).build());
    }
  }

  @POST
  @Path("{namespace}/{name}/{pullRequestId}/conflicts")
  @Produces("application/vnd.scmm-mergeConflictsResult+json")
  public MergeConflictResult conflicts(
    @PathParam("namespace") String namespace,
    @PathParam("name") String name,
    @PathParam("pullRequestId") String pullRequestId
  ) {
    return service.conflicts(new NamespaceAndName(namespace, name), pullRequestId);
  }

  @GET
  @Path("{namespace}/{name}/{pullRequestId}/commit-message")
  @Produces("text/plain")
  @StatusCodes({
    @ResponseCode(code = 200, condition = "squash commit message was created"),
    @ResponseCode(code = 401, condition = "not authenticated / invalid credentials"),
    @ResponseCode(code = 500, condition = "internal server error")
  })
  public String createDefaultCommitMessage(
    @PathParam("namespace") String namespace,
    @PathParam("name") String name,
    @PathParam("pullRequestId") String pullRequestId,
    @QueryParam("strategy") MergeStrategy strategy
  ) {
    return service.createDefaultCommitMessage(new NamespaceAndName(namespace, name), pullRequestId, strategy);
  }
}
