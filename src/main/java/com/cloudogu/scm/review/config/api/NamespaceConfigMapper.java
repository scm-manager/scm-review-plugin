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
import com.cloudogu.scm.review.config.service.NamespacePullRequestConfig;
import de.otto.edison.hal.Link;
import de.otto.edison.hal.Links;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.ObjectFactory;
import sonia.scm.api.v2.resources.LinkBuilder;

import jakarta.ws.rs.core.UriInfo;
import java.util.List;

@Mapper
public abstract class NamespaceConfigMapper {

  public abstract NamespacePullRequestConfigDto map(NamespacePullRequestConfig config, @Context String namespace, @Context UriInfo uriInfo);

  public abstract NamespacePullRequestConfig map(NamespacePullRequestConfigDto configDto);

  abstract List<BasePullRequestConfig.ProtectionBypass> map(List<BasePullRequestConfigDto.ProtectionBypassDto> bypassDtos);
  abstract BasePullRequestConfig.ProtectionBypass map(BasePullRequestConfigDto.ProtectionBypassDto bypassDto);

  abstract BasePullRequestConfigDto.ProtectionBypassDto map(BasePullRequestConfig.ProtectionBypass bypass);
  abstract List<BasePullRequestConfigDto.ProtectionBypassDto> mapToDto(List<BasePullRequestConfig.ProtectionBypass> bypasses);

  @ObjectFactory
  NamespacePullRequestConfigDto createForNamespace(@Context String namespace, @Context UriInfo uriInfo) {
    LinkBuilder linkBuilder = new LinkBuilder(uriInfo::getBaseUri, NamespaceConfigResource.class);
    Links.Builder halLinks = new Links.Builder();

    halLinks.self(linkBuilder.method("getNamespaceConfig").parameters(namespace).href());

    if (PermissionCheck.mayConfigure(namespace)) {
      halLinks.single(Link.link("update", linkBuilder.method("setNamespaceConfig").parameters(namespace).href()));
    }
    return new NamespacePullRequestConfigDto(halLinks.build());
  }
}
