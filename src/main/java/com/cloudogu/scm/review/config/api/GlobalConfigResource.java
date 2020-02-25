package com.cloudogu.scm.review.config.api;

import com.cloudogu.scm.review.PermissionCheck;
import com.cloudogu.scm.review.config.service.ConfigService;
import com.cloudogu.scm.review.pullrequest.api.PullRequestRootResource;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import sonia.scm.api.v2.resources.ErrorDto;
import sonia.scm.web.VndMediaType;

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

@OpenAPIDefinition(tags = {
  @Tag(name = "Pull Request Configuration", description = "Pull request configuration endpoints provided by review-plugin")
})
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
  @Operation(summary = "Global pull request configuration", description = "Returns the global pull request configuration.", tags = "Pull Request Configuration")
  @ApiResponse(
    responseCode = "200",
    description = "success",
    content = @Content(
      mediaType = MediaType.APPLICATION_JSON,
      schema = @Schema(implementation = GlobalPullRequestConfigDto.class)
    )
  )
  @ApiResponse(responseCode = "401", description = "not authenticated / invalid credentials")
  @ApiResponse(responseCode = "403", description = "not authorized, the current user does not have the \"configurePullRequest\" privilege")
  @ApiResponse(
    responseCode = "500",
    description = "internal server error",
    content = @Content(
      mediaType = VndMediaType.ERROR_TYPE,
      schema = @Schema(implementation = ErrorDto.class)
    )
  )
  public GlobalPullRequestConfigDto getGlobalConfig(@Context UriInfo uriInfo) {
    PermissionCheck.checkReadGlobalkConfig();
    return globalConfigMapper.map(configService.getGlobalPullRequestConfig(), uriInfo);
  }

  @PUT
  @Path("config")
  @Consumes(MediaType.APPLICATION_JSON)
  @Operation(summary = "Update global pull request configuration", description = "Modifies the global pull request configuration.", tags = "Pull Request Configuration")
  @ApiResponse(responseCode = "204", description = "update success")
  @ApiResponse(responseCode = "401", description = "not authenticated / invalid credentials")
  @ApiResponse(responseCode = "403", description = "not authorized, the current user does not have the \"configurePullRequest\" privilege")
  @ApiResponse(
    responseCode = "500",
    description = "internal server error",
    content = @Content(
      mediaType = VndMediaType.ERROR_TYPE,
      schema = @Schema(implementation = ErrorDto.class)
    )
  )
  public void setGlobalConfig(@Valid GlobalPullRequestConfigDto configDto) {
    PermissionCheck.checkWriteGlobalkConfig();
    configService.setGlobalPullRequestConfig(globalConfigMapper.map(configDto));
  }
}
