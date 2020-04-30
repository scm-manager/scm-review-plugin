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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.otto.edison.hal.Links;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ObjectFactory;
import sonia.scm.api.v2.resources.BaseMapper;

import javax.inject.Inject;
import javax.ws.rs.core.UriInfo;

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
