package com.cloudogu.scm.review.config.api;

import com.cloudogu.scm.review.PermissionCheck;
import com.cloudogu.scm.review.config.service.ConfigService;
import com.cloudogu.scm.review.pullrequest.api.PullRequestRootResource;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

@Path(PullRequestRootResource.PULL_REQUESTS_PATH_V2)
public class GlobalConfigResource {

  private final ConfigService configService;
  private final GlobalConfigMapper globalConfigMapper;

  @Inject
  public GlobalConfigResource(ConfigService configService, GlobalConfigMapper globalConfigMapper) {
    this.configService = configService;
    this.globalConfigMapper = globalConfigMapper;
  }

  @GET
  @Path("config")
  @Produces(MediaType.APPLICATION_JSON)
  public GlobalPullRequestConfigDto getGlobalConfig(@Context UriInfo uriInfo) {
    PermissionCheck.checkReadGlobalkConfig();
    return globalConfigMapper.map(configService.getGlobalPullRequestConfig(), uriInfo);
  }

  @PUT
  @Path("config")
  @Consumes(MediaType.APPLICATION_JSON)
  public void setGlobalConfig(@Valid GlobalPullRequestConfigDto configDto) {
    PermissionCheck.checkWriteGlobalkConfig();
    configService.setGlobalPullRequestConfig(globalConfigMapper.map(configDto));
  }
}
