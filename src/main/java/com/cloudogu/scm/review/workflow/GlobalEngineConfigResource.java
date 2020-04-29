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

import com.cloudogu.scm.review.PermissionCheck;
import de.otto.edison.hal.HalRepresentation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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
import javax.ws.rs.core.UriInfo;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.cloudogu.scm.review.workflow.GlobalEngineConfigResource.WORKFLOW_CONFIG_PATH;
import static com.cloudogu.scm.review.workflow.RepositoryEngineConfigResource.WORKFLOW_MEDIA_TYPE;

@Path(WORKFLOW_CONFIG_PATH)
public class GlobalEngineConfigResource {

  public static final String WORKFLOW_CONFIG_PATH = "v2/workflow";

  private final GlobalEngineConfigMapper mapper;
  private final GlobalEngineConfigurator configurator;
  private final Set<Rule> availableRules;

  @Inject
  public GlobalEngineConfigResource(GlobalEngineConfigMapper mapper, GlobalEngineConfigurator configurator, Set<Rule> availableRules) {
    this.mapper = mapper;
    this.configurator = configurator;
    this.availableRules = availableRules;
  }

  @GET
  @Path("config")
  @Produces(WORKFLOW_MEDIA_TYPE)
  @Operation(
    summary = "Global workflow engine configuration",
    description = "Returns the global workflow engine configuration.",
    tags = "Workflow Engine",
    operationId = "review_get_global_workflow_engine_config"
  )
  @ApiResponse(
    responseCode = "200",
    description = "success",
    content = @Content(
      mediaType = WORKFLOW_MEDIA_TYPE,
      schema = @Schema(implementation = GlobalEngineConfigDto.class)
    )
  )
  @ApiResponse(responseCode = "401", description = "not authenticated / invalid credentials")
  @ApiResponse(responseCode = "403", description = "not authorized, the current user does not have the \"readWorkflowEngineConfig\" privilege")
  @ApiResponse(
    responseCode = "500",
    description = "internal server error",
    content = @Content(
      mediaType = VndMediaType.ERROR_TYPE,
      schema = @Schema(implementation = ErrorDto.class)
    )
  )
  public GlobalEngineConfigDto getGlobalEngineConfig(@Context UriInfo uriInfo) {
    PermissionCheck.checkReadWorkflowEngineGlobalConfig();
    return mapper.map(configurator.getEngineConfiguration(), uriInfo);
  }

  @PUT
  @Path("config")
  @Consumes(WORKFLOW_MEDIA_TYPE)
  @Operation(
    summary = "Update global workflow engine configuration",
    description = "Modifies the global workflow engine configuration.",
    tags = "Workflow Engine",
    operationId = "review_put_global_workflow_engine_config"

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
  public void setGlobalEngineConfig(@Valid GlobalEngineConfigDto configDto) {
    PermissionCheck.checkWriteWorkflowEngineGlobalConfig();
    configurator.setEngineConfiguration(mapper.map(configDto));
  }

  @GET
  @Path("rules")
  @Produces(WORKFLOW_MEDIA_TYPE)
  @Operation(
    summary = "Workflow engine rules",
    description = "Returns available rules for the workflow engine.",
    tags = "Workflow Engine",
    operationId = "review_get_workflow_rules"
  )
  @ApiResponse(
    responseCode = "200",
    description = "success",
    content = @Content(
      mediaType = WORKFLOW_MEDIA_TYPE,
      schema = @Schema(implementation = HalRepresentation.class)
    )
  )
  @ApiResponse(
    responseCode = "500",
    description = "internal server error",
    content = @Content(
      mediaType = VndMediaType.ERROR_TYPE,
      schema = @Schema(implementation = ErrorDto.class)
    )
  )
  public List<String> getAvailableRules() {
    return availableRules.stream().map(Object::getClass).map(Class::getSimpleName).collect(Collectors.toList());
  }
}
