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

package com.cloudogu.scm.review.workflow;

import com.cloudogu.scm.review.PullRequestResourceLinks;
import de.otto.edison.hal.HalRepresentation;
import de.otto.edison.hal.Links;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import sonia.scm.api.v2.resources.ErrorDto;
import sonia.scm.web.VndMediaType;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.UriInfo;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.cloudogu.scm.review.workflow.GlobalEngineConfigResource.WORKFLOW_CONFIG_PATH;
import static com.cloudogu.scm.review.workflow.RepositoryEngineConfigResource.WORKFLOW_MEDIA_TYPE;

@Path(WORKFLOW_CONFIG_PATH)
public class GlobalEngineConfigResource {

  public static final String WORKFLOW_CONFIG_PATH = "v2/workflow";

  private final GlobalEngineConfigMapper mapper;
  private final EngineConfigService engineConfigService;
  private final Set<Rule> availableRules;

  @Inject
  public GlobalEngineConfigResource(GlobalEngineConfigMapper mapper, EngineConfigService engineConfigService, Set<Rule> availableRules) {
    this.mapper = mapper;
    this.engineConfigService = engineConfigService;
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
    return mapper.map(engineConfigService.getGlobalEngineConfig(), uriInfo);
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
    engineConfigService.setGlobalEngineConfig(mapper.map(configDto));
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
  public AvailableRulesDto getAvailableRules(@Context UriInfo uriInfo) {
    final String selfLink = new PullRequestResourceLinks(uriInfo::getBaseUri).workflowEngineGlobalConfigLinks().availableRules();
    return new AvailableRulesDto(
      new Links.Builder().self(selfLink).build(),
      availableRules.stream().map(RuleDto::new).collect(Collectors.toList())
    );
  }

  @Getter
  static class AvailableRulesDto extends HalRepresentation {
    public AvailableRulesDto(Links links, List<RuleDto> rules) {
      super(links);
      this.rules = rules;
    }

    private final List<RuleDto> rules;
  }

  @Getter
  static class RuleDto {
    RuleDto(Rule rule) {
      this.name = AvailableRules.nameOf(rule);
      this.applicableMultipleTimes = rule.isApplicableMultipleTimes();
    }
    private final String name;
    private final boolean applicableMultipleTimes;
  }
}
