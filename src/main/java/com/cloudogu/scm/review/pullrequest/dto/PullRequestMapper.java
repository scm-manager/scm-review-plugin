package com.cloudogu.scm.review.pullrequest.dto;

import com.cloudogu.scm.review.PullRequestResourceLinks;
import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import de.otto.edison.hal.Links;
import org.mapstruct.AfterMapping;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import sonia.scm.api.v2.resources.BaseMapper;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryPermissions;

import javax.ws.rs.core.UriInfo;
import java.net.URI;

import static de.otto.edison.hal.Link.link;
import static de.otto.edison.hal.Links.linkingTo;

@Mapper
public abstract class PullRequestMapper extends BaseMapper<PullRequest, PullRequestDto> {


  private PullRequestResourceLinks pullRequestResourceLinks = new PullRequestResourceLinks(() -> URI.create("/"));

  @Mapping(target = "attributes", ignore = true) // We do not map HAL attributes
  public abstract PullRequestDto map(PullRequest pullRequest, @Context Repository repository);

  public abstract PullRequest map(PullRequestDto dto);

  public PullRequestMapper using(UriInfo uriInfo) {
    pullRequestResourceLinks = new PullRequestResourceLinks(uriInfo::getBaseUri);
    return this;
  }

  @AfterMapping
  protected void appendLinks(@MappingTarget PullRequestDto target, @Context Repository repository) {
    Links.Builder linksBuilder = linkingTo().self(pullRequestResourceLinks.pullRequest().self(repository.getNamespace(), repository.getName(), target.getId()));
    if (RepositoryPermissions.push(repository).isPermitted()) {
      linksBuilder.single(link("update", pullRequestResourceLinks.pullRequest().update(repository.getNamespace(), repository.getName(), target.getId())));
    }
    if (RepositoryPermissions.read(repository).isPermitted()) {
      linksBuilder.single(link("createComment", pullRequestResourceLinks.pullRequestComments().create(repository.getNamespace(), repository.getName(), target.getId())));
      linksBuilder.single(link("comments", pullRequestResourceLinks.pullRequestComments().all(repository.getNamespace(), repository.getName(), target.getId())));
    }
    target.add(linksBuilder.build());
  }
}
