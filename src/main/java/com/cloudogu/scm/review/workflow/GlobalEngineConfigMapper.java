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

import com.cloudogu.scm.review.PermissionCheck;
import com.cloudogu.scm.review.PullRequestResourceLinks;
import de.otto.edison.hal.Links;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ObjectFactory;

import jakarta.ws.rs.core.UriInfo;

import static de.otto.edison.hal.Link.link;

@Mapper
public abstract class GlobalEngineConfigMapper extends EngineConfigMapper<GlobalEngineConfiguration, GlobalEngineConfigDto> {

  @Mapping(target = "attributes", ignore = true) // We do not map HAL attributes
  public abstract GlobalEngineConfigDto map(GlobalEngineConfiguration engineConfiguration, @org.mapstruct.Context UriInfo uriInfo);

  @ObjectFactory
  GlobalEngineConfigDto create(@Context UriInfo uriInfo) {
    final Links.Builder linksBuilder = new Links.Builder();
    PullRequestResourceLinks.WorkflowEngineGlobalConfigLinks workflowEngineGlobalConfigLinks = new PullRequestResourceLinks(uriInfo::getBaseUri).workflowEngineGlobalConfigLinks();
    linksBuilder.self(workflowEngineGlobalConfigLinks.getConfig());
    if (PermissionCheck.mayConfigureGlobalWorkflowConfig()) {
      linksBuilder.single(link("update", workflowEngineGlobalConfigLinks.setConfig()));
    }
    if (PermissionCheck.mayReadGlobalWorkflowConfig() || PermissionCheck.mayConfigureGlobalWorkflowConfig()) {
      linksBuilder.single(link("availableRules", workflowEngineGlobalConfigLinks.availableRules()));
    }
    return new GlobalEngineConfigDto(linksBuilder.build());
  }

  public abstract GlobalEngineConfiguration map(GlobalEngineConfigDto engineConfigurationDto);
}
