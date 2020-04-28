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
import com.cloudogu.scm.review.PullRequestResourceLinks;
import de.otto.edison.hal.Links;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ObjectFactory;
import sonia.scm.api.v2.resources.BaseMapper;
import sonia.scm.repository.Repository;

import javax.inject.Inject;
import javax.ws.rs.core.UriInfo;

import static de.otto.edison.hal.Link.link;

@Mapper
public abstract class RepositoryEngineConfigMapper extends BaseMapper<EngineConfiguration, RepositoryEngineConfigDto> {

  @Inject
  AvailableRules availableRules;
  @Inject
  GlobalEngineConfigurator globalEngineConfigurator;

  @Mapping(target = "attributes", ignore = true) // We do not map HAL attributes
  public abstract RepositoryEngineConfigDto map(EngineConfiguration engineConfiguration, @Context Repository repository, @Context UriInfo uriInfo);

  @ObjectFactory
  RepositoryEngineConfigDto create(@Context Repository repository, @Context UriInfo uriInfo) {
    final Links.Builder linksBuilder = new Links.Builder();
    PullRequestResourceLinks.WorkflowEngineConfigLinks workflowEngineConfigLinks = new PullRequestResourceLinks(uriInfo::getBaseUri).workflowEngineConfigLinks();
    linksBuilder.self(workflowEngineConfigLinks.getConfig(repository.getNamespace(), repository.getName()));
    if (!globalEngineConfigurator.getEngineConfiguration().isDisableRepositoryConfiguration()) {
     if (PermissionCheck.mayConfigureWorkflowConfig(repository)) {
       linksBuilder.single(link("update", workflowEngineConfigLinks.setConfig(repository.getNamespace(), repository.getName())));
     }
     if (PermissionCheck.mayReadWorkflowConfig(repository) || PermissionCheck.mayConfigureWorkflowConfig(repository)) {
       linksBuilder.single(link("availableRules", workflowEngineConfigLinks.availableRules()));
     }
    }
    return new RepositoryEngineConfigDto(linksBuilder.build());
  }

  public abstract EngineConfiguration map(RepositoryEngineConfigDto engineConfigurationDto);

}
