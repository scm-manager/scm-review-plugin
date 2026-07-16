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

package com.cloudogu.scm.review.mcp;

import com.cloudogu.mcp.CreateFrontendLinkInput;
import com.cloudogu.mcp.FrontendLinkParameter;
import com.cloudogu.mcp.FrontendLinkResolver;
import com.cloudogu.mcp.FrontendLinkResult;
import jakarta.inject.Inject;
import sonia.scm.plugin.Extension;
import sonia.scm.plugin.Requires;
import sonia.scm.repository.api.RepositoryServiceFactory;

import java.util.Map;
import java.util.Set;

@Extension
@Requires("scm-mcp-plugin")
class PullRequestFrontendLinkResolver implements FrontendLinkResolver {

  private final RepositoryServiceFactory repositoryServiceFactory;

  @Inject
  PullRequestFrontendLinkResolver(RepositoryServiceFactory repositoryServiceFactory) {
    this.repositoryServiceFactory = repositoryServiceFactory;
  }

  @Override
  public String getTargetType() {
    return "pullRequest";
  }

  @Override
  public String getDescription() {
    return "Link to a pull request in a repository.";
  }

  @Override
  public Set<FrontendLinkParameter> getRequiredParameters() {
    return Set.of(
      FrontendLinkParameter.NAMESPACE,
      FrontendLinkParameter.NAME,
      FrontendLinkParameter.ID
    );
  }

  @Override
  public FrontendLinkResult createLink(CreateFrontendLinkInput input) {
    String url = findRepositoryUrl(repositoryServiceFactory, input) + "/pull-request/" + input.getId();

    return FrontendLinkResult.of(
      getTargetType(),
      String.format("%s/%s#%s", input.getNamespace(), input.getName(), input.getId()),
      url,
      createMetadata(input)
    );
  }

  private Map<String, Object> createMetadata(CreateFrontendLinkInput input) {
    Map<String, Object> metadata = repositoryMetadata(input);
    metadata.put("id", input.getId());
    return metadata;
  }
}
