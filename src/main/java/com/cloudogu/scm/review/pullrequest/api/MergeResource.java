package com.cloudogu.scm.review.pullrequest.api;

import com.cloudogu.scm.review.pullrequest.dto.PullRequestDto;
import com.cloudogu.scm.review.pullrequest.dto.PullRequestMapper;
import com.cloudogu.scm.review.pullrequest.service.MergeService;
import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import sonia.scm.api.v2.resources.MergeResultToDtoMapper;
import sonia.scm.repository.NamespaceAndName;
import sonia.scm.repository.api.MergeCommandResult;
import sonia.scm.repository.api.MergeStrategy;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

@Path(MergeResource.MERGE_PATH_V2)
public class MergeResource {

  static final String MERGE_PATH_V2 = "v2/merge";
  private final MergeService service;
  private final PullRequestMapper pullRequestMapper;
  private MergeResultToDtoMapper mergeResultToDtoMapper;

  @Inject
  public MergeResource(MergeService service, PullRequestMapper pullRequestMapper, MergeResultToDtoMapper mergeResultToDtoMapper) {
    this.service = service;
    this.pullRequestMapper = pullRequestMapper;
    this.mergeResultToDtoMapper = mergeResultToDtoMapper;
  }

  @POST
  @Path("{namespace}/{name}")
  public Response merge(@PathParam("namespace") String namespace, @PathParam("name") String name, @QueryParam("strategy") MergeStrategy strategy,  @NotNull @Valid PullRequestDto pullRequestDto) {
    PullRequest pullRequest = pullRequestMapper.map(pullRequestDto);
    MergeCommandResult result = service.merge(new NamespaceAndName(namespace, name), pullRequest, strategy);
    if (result.isSuccess()) {
      return Response.noContent().build();
    } else {
      return Response.status(409).entity(mergeResultToDtoMapper.map(result)).build();
    }
  }

}
