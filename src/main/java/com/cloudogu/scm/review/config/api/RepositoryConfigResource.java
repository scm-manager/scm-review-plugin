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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import sonia.scm.api.v2.resources.ErrorDto;
import sonia.scm.repository.NamespaceAndName;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryManager;
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

import static sonia.scm.ContextEntry.ContextBuilder.entity;
import static sonia.scm.NotFoundException.notFound;

@Path(PullRequestRootResource.PULL_REQUESTS_PATH_V2)
public class RepositoryConfigResource {

  private final ConfigService configService;
  private final RepositoryConfigMapper repositoryConfigMapper;
  private final RepositoryManager repositoryManager;

  @Inject
  public RepositoryConfigResource(ConfigService configService,
                                  RepositoryConfigMapper repositoryConfigMapper,
                                  RepositoryManager repositoryManager) {
    this.configService = configService;
    this.repositoryConfigMapper = repositoryConfigMapper;
    this.repositoryManager = repositoryManager;
  }

  @GET
  @Path("{namespace}/{name}/config")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
    summary = "Repository pull request configuration",
    description = "Returns the repository-specific pull request configuration.",
    tags = "Pull Request Configuration",
    operationId = "review_get_repo_config"
  )
  @ApiResponse(
    responseCode = "200",
    description = "success",
    content = @Content(
      mediaType = MediaType.APPLICATION_JSON,
      schema = @Schema(implementation = RepositoryPullRequestConfigDto.class)
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
  public RepositoryPullRequestConfigDto getRepositoryConfig(@Context UriInfo uriInfo, @PathParam("namespace") String namespace, @PathParam("name") String name) {
    Repository repository = repositoryManager.get(new NamespaceAndName(namespace, name));
    if (repository == null) {
      throw notFound(entity(new NamespaceAndName(namespace, name)));
    }
    PermissionCheck.checkConfigure(repository);
    return repositoryConfigMapper.map(configService.getRepositoryPullRequestConfig(repository), repository, uriInfo);
  }

  @PUT
  @Path("{namespace}/{name}/config")
  @Consumes(MediaType.APPLICATION_JSON)
  @Operation(
    summary = "Update Repository pull request configuration",
    description = "Modifies the repository-specific pull request configuration.",
    tags = "Pull Request Configuration",
    operationId = "review_put_repo_config"
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
  public void setRepositoryConfig(@PathParam("namespace") String namespace, @PathParam("name") String name, @Valid RepositoryPullRequestConfigDto configDto) {
    Repository repository = repositoryManager.get(new NamespaceAndName(namespace, name));
    if (repository == null) {
      throw notFound(entity(new NamespaceAndName(namespace, name)));
    }
    PermissionCheck.checkConfigure(repository);
    configService.setRepositoryPullRequestConfig(repository, repositoryConfigMapper.map(configDto));
  }
}
