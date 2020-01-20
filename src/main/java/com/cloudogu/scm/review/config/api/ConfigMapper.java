package com.cloudogu.scm.review.config.api;

import com.cloudogu.scm.review.config.service.RepositoryPullRequestConfig;
import de.otto.edison.hal.Link;
import de.otto.edison.hal.Links;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.ObjectFactory;
import sonia.scm.api.v2.resources.LinkBuilder;
import sonia.scm.repository.Repository;

import javax.ws.rs.core.UriInfo;

@Mapper
public abstract class ConfigMapper {
  public abstract PullRequestConfigDto map(RepositoryPullRequestConfig repositoryPullRequestConfig, @Context Repository repository, @Context  UriInfo uriInfo);

  public abstract RepositoryPullRequestConfig map(PullRequestConfigDto repositoryPullRequestConfig);

  @ObjectFactory
  PullRequestConfigDto create(@Context Repository repository, @Context UriInfo uriInfo) {
    LinkBuilder linkBuilder = new LinkBuilder(uriInfo::getBaseUri, ConfigResource.class);
    Links links = new Links.Builder()
      .self(linkBuilder.method("getConfig").parameters(repository.getNamespace(), repository.getName()).href())
      .single(Link.link("update", linkBuilder.method("setConfig").parameters(repository.getNamespace(), repository.getName()).href()))
      .build();
    return new PullRequestConfigDto(links);
  }
}
