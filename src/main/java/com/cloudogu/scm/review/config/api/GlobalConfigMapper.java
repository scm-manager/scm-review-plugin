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
import com.cloudogu.scm.review.config.service.BasePullRequestConfig;
import com.cloudogu.scm.review.config.service.GlobalPullRequestConfig;
import de.otto.edison.hal.Link;
import de.otto.edison.hal.Links;
import jakarta.ws.rs.core.UriInfo;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.ObjectFactory;
import sonia.scm.api.v2.resources.LinkBuilder;

import java.util.List;

@Mapper
public abstract class GlobalConfigMapper {

  public abstract GlobalPullRequestConfigDto map(GlobalPullRequestConfig globalPullRequestConfig, @Context UriInfo uriInfo);

  public abstract GlobalPullRequestConfig map(GlobalPullRequestConfigDto configDto);

  abstract List<BasePullRequestConfig.ProtectionBypass> map(List<BasePullRequestConfigDto.ProtectionBypassDto> bypassDtos);
  abstract BasePullRequestConfig.ProtectionBypass map(BasePullRequestConfigDto.ProtectionBypassDto bypassDto);

  abstract BasePullRequestConfigDto.ProtectionBypassDto map(BasePullRequestConfig.ProtectionBypass bypass);
  abstract List<BasePullRequestConfigDto.ProtectionBypassDto> mapToDto(List<BasePullRequestConfig.ProtectionBypass> bypasses);

  @ObjectFactory
  GlobalPullRequestConfigDto createForGlobal(@Context UriInfo uriInfo) {
    LinkBuilder linkBuilder = new LinkBuilder(uriInfo::getBaseUri, GlobalConfigResource.class);
    Links.Builder halLinks = new Links.Builder();

    halLinks.self(linkBuilder.method("getGlobalConfig").parameters().href());

    if (PermissionCheck.mayWriteGlobalConfig()) {
      halLinks.single(Link.link("update", linkBuilder.method("setGlobalConfig").parameters().href()));
    }
    return new GlobalPullRequestConfigDto(halLinks.build());
  }
}
