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
import com.cloudogu.scm.review.pullrequest.service.PullRequestService;
import jakarta.inject.Inject;
import sonia.scm.plugin.Extension;
import sonia.scm.plugin.Requires;
import sonia.scm.repository.RepositoryManager;
import sonia.scm.search.Hit;

import java.util.Map;
import java.util.Optional;

@Extension
@Requires("scm-mcp-plugin")
public class PullRequestCommentSearchExtension implements ToolSearchExtension {

  private final RepositoryManager repositoryManager;

  @Inject
  public PullRequestCommentSearchExtension(RepositoryManager repositoryManager, PullRequestService pullRequestService) {
    this.repositoryManager = repositoryManager;
  }

  @Override
  public String getSearchType() {
    return "indexedComment";
  }

  @Override
  public String getSummary() {
    return "Comments in pull requests (corresponding type is 'indexedComment')";
  }

  @Override
  public String getDescription() {
    return """
      Search fields of type 'indexedComment':
      
      1. comment - The text of the comment
      2. author - The author of the comment
      3. type - The type of the comment (either `COMMENT`, `TASK_DONE`, or `TASK_TODO`)
      4. date - The time the comment was made as a unix epoch timestamp""";
  }

  @Override
  public String[] tableColumnHeader() {
    return new String[]{"Repository", "Pull Request Id", "Type", "Comment"};
  }

  @Override
  public String[] transformHitToTableFields(Hit hit) {
    return new String[] {
      extractRepository(hit).orElse("unknown"),
      extractValue(hit, "pullRequestId"),
      extractValue(hit, "author"),
      extractValue(hit, "type"),
      extractValue(hit, "comment")
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
