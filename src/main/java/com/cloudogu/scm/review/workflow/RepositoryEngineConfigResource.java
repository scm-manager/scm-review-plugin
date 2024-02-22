/*
 * MIT License
 *
 * Copyright (c) 2020-present Cloudogu GmbH and Contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.cloudogu.scm.review.workflow;

import de.otto.edison.hal.HalRepresentation;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@OpenAPIDefinition(tags = {
  @Tag(name = "Workflow Engine", description = "Workflow engine related endpoints provided by review-plugin")
})
@Path(RepositoryEngineConfigResource.WORKFLOW_CONFIG_PATH)
public class RepositoryEngineConfigResource {

  public static final String WORKFLOW_MEDIA_TYPE = VndMediaType.PREFIX + "workflow" + VndMediaType.SUFFIX;
  public static final String WORKFLOW_CONFIG_PATH = "v2/workflow";

  private final RepositoryManager repositoryManager;
  private final EngineConfigService engineConfigService;
  private final RepositoryEngineConfigMapper mapper;

  @Inject
  public RepositoryEngineConfigResource(RepositoryManager repositoryManager, EngineConfigService engineConfigService, RepositoryEngineConfigMapper mapper) {
    this.repositoryManager = repositoryManager;
    this.engineConfigService = engineConfigService;
    this.mapper = mapper;
  }

  @GET
  @Path("{namespace}/{name}/config")
  @Produces(WORKFLOW_MEDIA_TYPE)
  @Operation(
    summary = "Workflow engine configuration",
    description = "Returns the repository specific workflow engine configuration.",
    tags = "Workflow Engine",
    operationId = "review_get_repository_workflow_configuration"
  )
  @ApiResponse(
    responseCode = "200",
    description = "success",
    content = @Content(
      mediaType = MediaType.APPLICATION_JSON,
      schema = @Schema(implementation = HalRepresentation.class)
    )
  )
  @ApiResponse(responseCode = "401", description = "not authenticated / invalid credentials")
  @ApiResponse(responseCode = "403", description = "not authorized, the current user does not have the \"repository:readWorkflowConfig\" privilege")
  @ApiResponse(
    responseCode = "500",
    description = "internal server error",
    content = @Content(
      mediaType = VndMediaType.ERROR_TYPE,
      schema = @Schema(implementation = ErrorDto.class)
    )
  )
  public RepositoryEngineConfigDto getRepositoryEngineConfig(@Context UriInfo uriInfo,
                                                             @PathParam("namespace") String namespace,
                                                             @PathParam("name") String name) {
    Repository repository = loadRepository(namespace, name);
    return mapper.map(engineConfigService.getRepositoryEngineConfig(repository), repository, uriInfo);
  }

  @GET
  @Path("{namespace}/{name}")
  @Produces(WORKFLOW_MEDIA_TYPE)
  @Operation(
    summary = "Effective workflow engine configuration",
    description = "Returns the effective repository specific workflow engine configuration.",
    tags = "Workflow Engine",
    operationId = "review_get_effective_repository_workflow_configuration"
  )
  @ApiResponse(
    responseCode = "200",
    description = "success",
    content = @Content(
      mediaType = MediaType.APPLICATION_JSON,
      schema = @Schema(implementation = HalRepresentation.class)
    )
  )
  @ApiResponse(responseCode = "401", description = "not authenticated / invalid credentials")
  @ApiResponse(responseCode = "403", description = "not authorized, the current user does not have the \"repository:readWorkflowConfig\" privilege")
  @ApiResponse(
    responseCode = "500",
    description = "internal server error",
    content = @Content(
      mediaType = VndMediaType.ERROR_TYPE,
      schema = @Schema(implementation = ErrorDto.class)
    )
  )
  public RepositoryEngineConfigDto getEffectiveRepositoryEngineConfig(@Context UriInfo uriInfo,
                                                             @PathParam("namespace") String namespace,
                                                             @PathParam("name") String name) {
    Repository repository = loadRepository(namespace, name);
    return mapper.map(engineConfigService.getEffectiveEngineConfig(repository));
  }

  @PUT
  @Path("{namespace}/{name}/config")
  @Consumes(WORKFLOW_MEDIA_TYPE)
  @Operation(
    summary = "Update Repository workflow engine configuration",
    description = "Modifies the repository-specific workflow engine configuration.",
    tags = "Workflow Engine",
    operationId = "review_put_repository_workflow_config"
  )
  @ApiResponse(responseCode = "204", description = "update success")
  @ApiResponse(responseCode = "401", description = "not authenticated / invalid credentials")
  @ApiResponse(responseCode = "403", description = "not authorized, the current user does not have the \"repository:writeWorkflowConfig\" privilege")
  @ApiResponse(
    responseCode = "500",
    description = "internal server error",
    content = @Content(
      mediaType = VndMediaType.ERROR_TYPE,
      schema = @Schema(implementation = ErrorDto.class)
    )
  )
  public void setRepositoryEngineConfig(@PathParam("namespace") String namespace, @PathParam("name") String name, @Valid RepositoryEngineConfigDto configDto) {
    Repository repository = loadRepository(namespace, name);
    engineConfigService.setRepositoryEngineConfig(repository, mapper.map(configDto));
  }

  private Repository loadRepository(String namespace, String name) {
    Repository repository = repositoryManager.get(new NamespaceAndName(namespace, name));
    if (repository == null) {
      throw notFound(entity(new NamespaceAndName(namespace, name)));
    }
    return repository;
  }
}
