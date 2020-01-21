package com.cloudogu.scm.review.config.api;

import com.cloudogu.scm.review.config.service.PullRequestConfig;
import de.otto.edison.hal.Link;
import de.otto.edison.hal.Links;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.ObjectFactory;
import sonia.scm.api.v2.resources.LinkBuilder;
import sonia.scm.repository.Repository;

import javax.ws.rs.core.UriInfo;

@Mapper
public abstract class RepositoryConfigMapper {
 public abstract PullRequestConfigDto map(PullRequestConfig pullRequestConfig, @Context Repository repository, @Context  UriInfo uriInfo);

  public abstract PullRequestConfig map(PullRequestConfigDto repositoryPullRequestConfig);

  @ObjectFactory
  PullRequestConfigDto createForRepository(@Context Repository repository, @Context UriInfo uriInfo) {
    LinkBuilder linkBuilder = new LinkBuilder(uriInfo::getBaseUri, RepositoryConfigResource.class);
    Links links = new Links.Builder()
      .self(linkBuilder.method("getRepositoryConfig").parameters(repository.getNamespace(), repository.getName()).href())
      .single(Link.link("update", linkBuilder.method("setRepositoryConfig").parameters(repository.getNamespace(), repository.getName()).href()))
      .build();
    return new PullRequestConfigDto(links);
  }
}
