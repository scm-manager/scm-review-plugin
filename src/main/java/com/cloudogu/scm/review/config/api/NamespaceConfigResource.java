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
import sonia.scm.api.v2.resources.ErrorDto;
import sonia.scm.web.VndMediaType;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;

@OpenAPIDefinition(tags = {
  @Tag(name = "Pull Request Configuration", description = "Pull request configuration endpoints provided by review-plugin")
})
@Path(PullRequestRootResource.PULL_REQUESTS_PATH_V2)
public class NamespaceConfigResource {

  private final ConfigService configService;
  private final NamespaceConfigMapper namespaceConfigMapper;

  @Inject
  public NamespaceConfigResource(ConfigService configService, NamespaceConfigMapper namespaceConfigMapper) {
    this.configService = configService;
    this.namespaceConfigMapper = namespaceConfigMapper;
  }

  @GET
  @Path("{namespace}/config")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
    summary = "Namespace pull request configuration",
    description = "Returns the namespace pull request configuration.",
    tags = "Pull Request Configuration",
    operationId = "review_get_namespace_config"
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
  public NamespacePullRequestConfigDto getNamespaceConfig(@Context UriInfo uriInfo, @PathParam("namespace") String namespace) {
    PermissionCheck.checkModify(namespace);
    return namespaceConfigMapper.map(configService.getNamespacePullRequestConfig(namespace), namespace, uriInfo);
  }

  @PUT
  @Path("{namespace}/config")
  @Consumes(MediaType.APPLICATION_JSON)
  @Operation(
    summary = "Update namespace pull request configuration",
    description = "Modifies the namespace pull request configuration.",
    tags = "Pull Request Configuration",
    operationId = "review_put_namespace_config"

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
  public void setNamespaceConfig(@Valid NamespacePullRequestConfigDto configDto, @PathParam("namespace") String namespace) {
    PermissionCheck.checkModify(namespace);
    configService.setNamespacePullRequestConfig(namespace, namespaceConfigMapper.map(configDto));
  }
}
