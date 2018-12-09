package com.cloudogu.scm.review.pullrequest.api;

import com.cloudogu.scm.review.comment.api.CommentRootResource;
import com.cloudogu.scm.review.pullrequest.dto.PullRequestMapper;
import com.cloudogu.scm.review.pullrequest.dto.PullRequestMapperImpl;
import com.cloudogu.scm.review.pullrequest.service.DefaultPullRequestService;
import com.cloudogu.scm.review.pullrequest.service.PullRequestService;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryPermissions;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

public class PullRequestResource {



  private final PullRequestMapper mapper;
  private final PullRequestService service;
  private final Provider<CommentRootResource> commentResourceProvider;

  @Inject
  public PullRequestResource(DefaultPullRequestService service, Provider<CommentRootResource> commentResourceProvider) {
    this.mapper = new PullRequestMapperImpl();
    this.service = service;
    this.commentResourceProvider = commentResourceProvider;
  }

  @Path("comments/")
  public CommentRootResource comments() {
    return commentResourceProvider.get();
  }


  @GET
  @Path("")
  @Produces(MediaType.APPLICATION_JSON)
  public Response get(@PathParam("namespace") String namespace, @PathParam("name") String name, @PathParam("pullRequestId") String pullRequestId) {
    Repository repository = service.getRepository(namespace, name);
    RepositoryPermissions.read(repository).check();
    return Response.ok(mapper.map(service.get(namespace, name, pullRequestId),repository)).build();
  }

}
