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

import com.cloudogu.mcp.ToolSearchExtension;
import jakarta.inject.Inject;
import sonia.scm.plugin.Extension;
import sonia.scm.repository.RepositoryManager;
import sonia.scm.search.Hit;

import java.util.Map;
import java.util.Optional;

@Extension
public class PullRequestSearchExtension implements ToolSearchExtension {

  private final RepositoryManager repositoryManager;

  @Inject
  public PullRequestSearchExtension(RepositoryManager repositoryManager) {
    this.repositoryManager = repositoryManager;
  }

  @Override
  public String getSearchType() {
    return "pullRequest";
  }

  @Override
  public String getSummary() {
    return "Pull requests (corresponding type is 'pullRequest')";
  }

  @Override
  public String getDescription() {
    return """
      Search fields of type 'pullRequest':
      
      1. id - The id of the pull request
      2. source - The source branch of the pull request
      3. target - The target branch of the pull request
      4. title - The title of the pull request
      5. description - The description of the pull request
      6. creationDate - The time the pull request was created as a unix epoch timestamp
      6. closeDate - The time the pull request was closed as a unix epoch timestamp""";
  }

  @Override
  public String[] tableColumnHeader() {
    return new String[]{"Repository", "Pull Request Id", "Source Branch", "Target Branch", "Title"};
  }

  @Override
  public String[] transformHitToTableFields(Hit hit) {
    return new String[] {
      extractRepository(hit).orElse("unknown"),
      extractValue(hit, "id"),
      extractValue(hit, "source"),
      extractValue(hit, "target"),
      extractValue(hit, "title")
    };
  }

  @Override
  public Map<String, Object> transformHitToStructuredAnswer(Hit hit) {
    Map<String, Object> result = ToolSearchExtension.super.transformHitToStructuredAnswer(hit);
    extractRepository(hit)
      .ifPresent(repository -> result.put("repository", repository));
    return result;
  }

  private Optional<String> extractRepository(Hit hit) {
    return hit.getRepositoryId()
      .map(
        repositoryId -> repositoryManager.get(repositoryId).getNamespaceAndName().toString()
      );
  }
}
