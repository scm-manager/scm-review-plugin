/*
 * Copyright (c) 2020 - present Cloudogu GmbH
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Affero General Public License as published by the Free
 * Software Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see https://www.gnu.org/licenses/.
 */

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
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;
import sonia.scm.api.v2.resources.ErrorDto;
import sonia.scm.web.VndMediaType;

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
  @Operation(
    summary = "Global pull request configuration",
    description = "Returns the global pull request configuration.",
    tags = "Pull Request Configuration",
    operationId = "review_get_global_config"
  )
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
    PermissionCheck.checkReadGlobalConfig();
    return globalConfigMapper.map(configService.getGlobalPullRequestConfig(), uriInfo);
  }

  @PUT
  @Path("config")
  @Consumes(MediaType.APPLICATION_JSON)
  @Operation(
    summary = "Update global pull request configuration",
    description = "Modifies the global pull request configuration.",
    tags = "Pull Request Configuration",
    operationId = "review_put_global_config"

  )
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
    PermissionCheck.checkWriteGlobalConfig();
    configService.setGlobalPullRequestConfig(globalConfigMapper.map(configDto));
  }
}
