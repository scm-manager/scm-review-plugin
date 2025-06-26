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

import com.cloudogu.scm.review.config.service.RepositoryPullRequestConfig;
import de.otto.edison.hal.Link;
import de.otto.edison.hal.Links;
import jakarta.ws.rs.core.UriInfo;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.ObjectFactory;
import sonia.scm.api.v2.resources.LinkBuilder;
import sonia.scm.repository.Repository;

@Mapper
public abstract class RepositoryConfigMapper {
 public abstract RepositoryPullRequestConfigDto map(RepositoryPullRequestConfig repositoryPullRequestConfig, @Context Repository repository, @Context  UriInfo uriInfo);

  public abstract RepositoryPullRequestConfig map(RepositoryPullRequestConfigDto repositoryPullRequestConfig);

  @ObjectFactory
  RepositoryPullRequestConfigDto createForRepository(@Context Repository repository, @Context UriInfo uriInfo) {
    LinkBuilder linkBuilder = new LinkBuilder(uriInfo::getBaseUri, RepositoryConfigResource.class);
    Links links = new Links.Builder()
      .self(linkBuilder.method("getRepositoryConfig").parameters(repository.getNamespace(), repository.getName()).href())
      .single(Link.link("update", linkBuilder.method("setRepositoryConfig").parameters(repository.getNamespace(), repository.getName()).href()))
      .build();
    return new RepositoryPullRequestConfigDto(links);
  }
}
