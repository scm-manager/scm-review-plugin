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
import jakarta.inject.Inject;
import jakarta.ws.rs.core.UriInfo;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ObjectFactory;
import sonia.scm.repository.Repository;

import static de.otto.edison.hal.Link.link;

@Mapper
public abstract class RepositoryEngineConfigMapper extends EngineConfigMapper<EngineConfiguration, RepositoryEngineConfigDto> {

  @Inject
  GlobalEngineConfigurator globalEngineConfigurator;

  @Mapping(target = "attributes", ignore = true) // We do not map HAL attributes
  public abstract RepositoryEngineConfigDto map(EngineConfiguration engineConfiguration, @Context Repository repository, @Context UriInfo uriInfo);

  @ObjectFactory
  RepositoryEngineConfigDto create(@Context Repository repository, @Context UriInfo uriInfo) {
    final Links.Builder linksBuilder = new Links.Builder();
    PullRequestResourceLinks links = new PullRequestResourceLinks(uriInfo::getBaseUri);
    PullRequestResourceLinks.WorkflowEngineConfigLinks workflowEngineConfigLinks = links.workflowEngineConfigLinks();
    linksBuilder.self(workflowEngineConfigLinks.getConfig(repository.getNamespace(), repository.getName()));
    if (!globalEngineConfigurator.getEngineConfiguration().isDisableRepositoryConfiguration()) {
      if (PermissionCheck.mayConfigureWorkflowConfig(repository)) {
        linksBuilder.single(link("update", workflowEngineConfigLinks.setConfig(repository.getNamespace(), repository.getName())));
      }
      if (PermissionCheck.mayReadWorkflowConfig(repository) || PermissionCheck.mayConfigureWorkflowConfig(repository)) {
        linksBuilder.single(link("availableRules", links.workflowEngineGlobalConfigLinks().availableRules()));
      }
    }
    return new RepositoryEngineConfigDto(linksBuilder.build());
  }

  public abstract EngineConfiguration map(RepositoryEngineConfigDto engineConfigurationDto);

}
