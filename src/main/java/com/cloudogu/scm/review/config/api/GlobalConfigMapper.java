package com.cloudogu.scm.review.config.api;

import com.cloudogu.scm.review.PermissionCheck;
import com.cloudogu.scm.review.config.service.GlobalPullRequestConfig;
import de.otto.edison.hal.Link;
import de.otto.edison.hal.Links;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.ObjectFactory;
import sonia.scm.api.v2.resources.LinkBuilder;

import javax.ws.rs.core.UriInfo;

@Mapper
public abstract class GlobalConfigMapper {

  public abstract GlobalPullRequestConfigDto map(GlobalPullRequestConfig globalPullRequestConfig, @Context UriInfo uriInfo);

  public abstract GlobalPullRequestConfig map(GlobalPullRequestConfigDto configDto);

  @ObjectFactory
  GlobalPullRequestConfigDto createForGlobal(@Context UriInfo uriInfo) {
    LinkBuilder linkBuilder = new LinkBuilder(uriInfo::getBaseUri, GlobalConfigResource.class);
    Links.Builder halLinks = new Links.Builder();

    halLinks.self(linkBuilder.method("getGlobalConfig").parameters().href());

    if (PermissionCheck.mayWriteGlobalConfig()) {
      halLinks.single(Link.link("update", linkBuilder.method("setGlobalConfig").parameters().href()));
    }
    return new GlobalPullRequestConfigDto(halLinks.build());
  }
}
