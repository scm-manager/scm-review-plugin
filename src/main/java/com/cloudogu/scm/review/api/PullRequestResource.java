package com.cloudogu.scm.review.api;

import com.cloudogu.scm.review.service.DefaultPullRequestService;
import com.cloudogu.scm.review.service.PullRequestService;
import com.cloudogu.scm.review.service.PullRequestStatus;
import de.otto.edison.hal.Embedded;
import de.otto.edison.hal.HalRepresentation;
import de.otto.edison.hal.Link;
import de.otto.edison.hal.Links;
import org.apache.shiro.SecurityUtils;
import sonia.scm.ScmConstraintViolationException;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryPermissions;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static sonia.scm.AlreadyExistsException.alreadyExists;
import static sonia.scm.ContextEntry.ContextBuilder.entity;

@Path(PullRequestResource.PULL_REQUESTS_PATH_V2)
public class PullRequestResource {

  public static final String PULL_REQUESTS_PATH_V2 = "v2/pull-requests";

  private final PullRequestToPullRequestDtoMapper mapper;
  private final PullRequestService service;

  @Inject
  public PullRequestResource(DefaultPullRequestService pullRequestService) {
    this.service = pullRequestService;
    this.mapper = new PullRequestToPullRequestDtoMapperImpl();
  }

  @POST
  @Path("{namespace}/{name}")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response create(@Context UriInfo uriInfo, @PathParam("namespace") String namespace, @PathParam("name") String name, @NotNull @Valid PullRequestDto pullRequestDto) {

    Repository repository = service.getRepository(namespace, name);
    pullRequestDto.setStatus(PullRequestStatus.OPEN);
    service.get(repository, pullRequestDto.getSource(), pullRequestDto.getTarget(), pullRequestDto.getStatus())
      .ifPresent(pullRequest -> {
        throw alreadyExists(entity("pull request", pullRequest.getId()).in(repository));
      });
    service.checkBranch(repository, pullRequestDto.getSource());
    service.checkBranch(repository, pullRequestDto.getTarget());

    verifyBranchesDiffer(pullRequestDto.getSource(), pullRequestDto.getTarget());
    pullRequestDto.setAuthor(SecurityUtils.getSubject().getPrincipals().getPrimaryPrincipal().toString());
    Instant now = Instant.now();
    pullRequestDto.setCreationDate(now);
    pullRequestDto.setLastModified(now);
    String id = service.add(repository, mapper.map(pullRequestDto));
    URI location = uriInfo.getAbsolutePathBuilder().path(id).build();
    return Response.created(location).build();
  }

  @GET
  @Path("{namespace}/{name}/{id}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response get(@Context UriInfo uriInfo, @PathParam("namespace") String namespace, @PathParam("name") String name, @PathParam("id") String id) {
    return Response.ok(mapper.map(service.get(namespace, name, id), uriInfo.getAbsolutePathBuilder().build()))
      .build();
  }

  @GET
  @Path("{namespace}/{name}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getAll(@Context UriInfo uriInfo, @PathParam("namespace") String namespace, @PathParam("name") String name, @QueryParam("status") @DefaultValue("ALL") PullRequestStatusDto pullRequestStatusDto) {
    Repository repository = service.getRepository(namespace, name);

    List<PullRequestDto> pullRequestDtos = service.getAll(namespace, name)
      .stream()
      .filter(pullRequest -> pullRequestStatusDto == PullRequestStatusDto.ALL || pullRequest.getStatus().equals(PullRequestStatus.valueOf(pullRequestStatusDto.name())))
      .map(pr -> mapper.map(pr, uriInfo.getAbsolutePathBuilder().path(pr.getId()).build()))
      .sorted(Comparator.comparing(PullRequestDto::getLastModified).reversed())
      .collect(Collectors.toList());

    return Response.ok(createCollection(uriInfo, repository, pullRequestDtos)).build();
  }

  private HalRepresentation createCollection(UriInfo uriInfo, Repository repository, List<PullRequestDto> pullRequestDtos) {
    String href = uriInfo.getAbsolutePath().toASCIIString();

    Links.Builder builder = Links.linkingTo().self(href);

    if (RepositoryPermissions.push(repository.getId()).isPermitted()) {
      builder.single(Link.link("create", href));
    }

    return new HalRepresentation(builder.build(), Embedded.embedded("pullRequests", pullRequestDtos));
  }


  private void verifyBranchesDiffer(String source, String target) {
    ScmConstraintViolationException.Builder
      .doThrow()
      .violation("source branch and target branch must differ", "pullRequest", "source")
      .violation("source branch and target branch must differ", "pullRequest", "target")
      .when(source.equals(target));
  }
}
