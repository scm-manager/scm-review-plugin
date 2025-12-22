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

import com.cloudogu.mcp.ToolResult;
import com.cloudogu.mcp.TypedTool;
import com.cloudogu.scm.review.comment.service.Comment;
import com.cloudogu.scm.review.comment.service.CommentQueryFields;
import com.cloudogu.scm.review.comment.service.CommentStoreFactory;
import com.cloudogu.scm.review.comment.service.CommentType;
import com.cloudogu.scm.review.pullrequest.service.MergeService;
import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import com.cloudogu.scm.review.pullrequest.service.PullRequestQueryFields;
import com.cloudogu.scm.review.pullrequest.service.PullRequestStatus;
import com.cloudogu.scm.review.pullrequest.service.PullRequestStoreFactory;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.google.common.base.Function;
import com.google.common.base.Strings;
import jakarta.inject.Inject;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import sonia.scm.plugin.Extension;
import sonia.scm.repository.NamespaceAndName;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryManager;
import sonia.scm.store.Condition;
import sonia.scm.store.Conditions;
import sonia.scm.store.QueryableStore;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Extension
class PullRequestTool implements TypedTool<PullRequestListInput> {
  private final PullRequestStoreFactory pullRequestStoreFactory;
  private final CommentStoreFactory commentStoreFactory;
  private final RepositoryManager repositoryManager;
  private final PullRequestMcpMapper pullRequestMapper;
  private final MergeService mergeService;

  @Inject
  PullRequestTool(PullRequestStoreFactory pullRequestStoreFactory,
                  CommentStoreFactory commentStoreFactory,
                  RepositoryManager repositoryManager,
                  PullRequestMcpMapper pullRequestMapper,
                  MergeService mergeService) {
    this.pullRequestStoreFactory = pullRequestStoreFactory;
    this.commentStoreFactory = commentStoreFactory;
    this.repositoryManager = repositoryManager;
    this.pullRequestMapper = pullRequestMapper;
    this.mergeService = mergeService;
  }

  @Override
  public Class<? extends PullRequestListInput> getInputClass() {
    return PullRequestListInput.class;
  }

  @Override
  public ToolResult execute(PullRequestListInput input) {
    log.trace("executing request {}", input);
    Collection<Condition<PullRequest>> conditions = new ArrayList<>();
    if (!Strings.isNullOrEmpty(input.getPullRequestId())) {
      conditions.add(PullRequestQueryFields.ID.eq(input.getPullRequestId()));
    }

    if (!Strings.isNullOrEmpty(input.getRepositoryNamespace()) && !Strings.isNullOrEmpty(input.getRepositoryName())) {
      Repository repository = findRepository(input.getRepositoryNamespace(), input.getRepositoryName());
      if (repository == null) {
        return ToolResult.error("Could not find repository with namespace " + input.getRepositoryNamespace() + " and name " + input.getRepositoryName());
      }
      conditions.add(PullRequestQueryFields.REPOSITORY_ID.eq(repository.getId()));
    } else if (!Strings.isNullOrEmpty(input.getRepositoryNamespace())) {
      Collection<String> ids = repositoryManager.getAll()
        .stream()
        .filter(repository -> repository.getNamespace().equals(input.getRepositoryNamespace()))
        .map(Repository::getId)
        .collect(Collectors.toSet());
      conditions.add(PullRequestQueryFields.REPOSITORY_ID.in(ids));
    } else if (!Strings.isNullOrEmpty(input.getRepositoryName())) {
      Collection<String> ids = repositoryManager.getAll()
        .stream()
        .filter(repository -> repository.getName().equals(input.getRepositoryName()))
        .map(Repository::getId)
        .collect(Collectors.toSet());
      conditions.add(PullRequestQueryFields.REPOSITORY_ID.in(ids));
    }

    if (!Strings.isNullOrEmpty(input.getAuthorUserId())) {
      conditions.add(PullRequestQueryFields.AUTHOR.eq(input.getAuthorUserId()));
    }

    if (!Strings.isNullOrEmpty(input.getReviewerUserId())) {
      conditions.add(PullRequestQueryFields.REVIEWER.containsKey(input.getReviewerUserId()));
    }

    if (!Strings.isNullOrEmpty(input.getTitleContains())) {
      conditions.add(PullRequestQueryFields.TITLE.contains(input.getTitleContains()));
    }

    if (!Strings.isNullOrEmpty(input.getDescriptionContains())) {
      conditions.add(PullRequestQueryFields.DESCRIPTION.contains(input.getDescriptionContains()));
    }

    if (!Strings.isNullOrEmpty(input.getTitleOrDescriptionContains())) {
      conditions.add(
        Conditions.or(
          PullRequestQueryFields.TITLE.contains(input.getTitleOrDescriptionContains()),
          PullRequestQueryFields.DESCRIPTION.contains(input.getTitleOrDescriptionContains())
        )
      );
    }

    if (input.getStatus() != null && input.getStatus().length > 0) {
      conditions.add(PullRequestQueryFields.STATUS.in(input.getStatus()));
    }

    if (!Strings.isNullOrEmpty(input.getSourceBranch())) {
      conditions.add(PullRequestQueryFields.SOURCE.eq(input.getSourceBranch()));
    }

    if (!Strings.isNullOrEmpty(input.getTargetBranch())) {
      conditions.add(PullRequestQueryFields.TARGET.eq(input.getTargetBranch()));
    }

    if (!Strings.isNullOrEmpty(input.getAffectedBranch())) {
      conditions.add(
        Conditions.or(
          PullRequestQueryFields.SOURCE.eq(input.getAffectedBranch()),
          PullRequestQueryFields.TARGET.eq(input.getAffectedBranch())
        )
      );
    }

    try (QueryableStore<PullRequest> store = pullRequestStoreFactory.getOverall()) {
      QueryableStore.Query query = store.query(conditions.toArray(new Condition[0])).withIds();

      if (input.getOrderBy() != null) {
        QueryableStore.Order order = input.getOrder() == Order.DESCENDING ? QueryableStore.Order.DESC : QueryableStore.Order.ASC;
        switch (input.getOrderBy()) {
          case ID -> query = query.orderBy(PullRequestQueryFields.INTERNAL_ID, order);
          case CREATION_DATE -> query = query.orderBy(PullRequestQueryFields.CREATIONDATE, order);
          case LAST_MODIFICATION -> query = query.orderBy(PullRequestQueryFields.LASTMODIFIED, order);
        }
      }

      List<QueryableStore.Result<PullRequest>> all = query.findAll(0, input.getLimit() == 0 ? Integer.MAX_VALUE : input.getLimit());

      all = all.stream()
        .filter(result -> !input.isWithOpenTasksOnly() || hasOpenTasks(result))
        .filter(result -> !input.isWithObstaclesOnly() || filterForObstacles(result))
        .toList();

      String pullRequestIds = all
        .stream()
        .map(this::formatPullRequestAsContentString)
        .collect(Collectors.joining("\n"));

      if (input.getDetailLevel() == DETAIL_LEVEL.NONE) {
        return ToolResult.ok(List.of(String.format("I found %s pull requests.", all.size()), pullRequestIds), null);
      }

      Function<QueryableStore.Result<PullRequest>, Object> converter =
        switch (input.getDetailLevel()) {
          case OVERVIEW -> this::convertToOverview;
          case WITH_OBSTACLES -> this::convertWithObstacles;
          default -> this::convertWithDetails;
        };

      Map<String, Object> structuredContent = all
        .stream()
        .collect(Collectors.toMap(
          this::formatPullRequestAsKeyString,
          converter
        ));

      log.trace("found {} pull requests", all.size());

      List<String> content = List.of(
        String.format("I found %s pull requests.", all.size()),
        """
          Detailed metadata (author, description, source and target branches, status and so on) for each is available
          in the structured data block under their respective names.
          """,
        pullRequestIds
      );

      return ToolResult.ok(content, structuredContent);
    }
  }

  private boolean filterForObstacles(QueryableStore.Result<PullRequest> result) {
    Repository repository = repositoryManager.get(result.getParentId(Repository.class).get());
    return !mergeService.getObstacles(repository, result.getEntity()).isEmpty();
  }

  private boolean hasOpenTasks(QueryableStore.Result<PullRequest> result) {
    String repositoryId = result.getParentId(Repository.class).get();
    String pullRequestId = result.getId();
    try (QueryableStore<Comment> commentStore = commentStoreFactory.get(repositoryId, pullRequestId)) {
      return commentStore.query(CommentQueryFields.TYPE.eq(CommentType.TASK_TODO)).count() > 0;
    }
  }

  private PullRequestOverviewMcp convertToOverview(QueryableStore.Result<PullRequest> result) {
    Repository repository = repositoryManager.get(result.getParentId(Repository.class).get());
    return pullRequestMapper.mapOverview(result.getEntity(), repository);
  }

  private PullRequestOverviewMcp convertWithDetails(QueryableStore.Result<PullRequest> result) {
    Repository repository = repositoryManager.get(result.getParentId(Repository.class).get());
    return pullRequestMapper.mapDetails(result.getEntity(), repository);
  }

  private PullRequestOverviewMcp convertWithObstacles(QueryableStore.Result<PullRequest> result) {
    Repository repository = repositoryManager.get(result.getParentId(Repository.class).get());
    return pullRequestMapper.mapWithObstacles(result.getEntity(), repository);
  }

  private String formatPullRequestAsContentString(QueryableStore.Result<PullRequest> result) {
    Repository repository = repositoryManager.get(result.getParentId(Repository.class).get());
    return String.format("* %s/%s#%s [%s]", repository.getNamespace(), repository.getName(), result.getEntity().getId(), result.getEntity().getStatus());
  }

  private String formatPullRequestAsKeyString(QueryableStore.Result<PullRequest> result) {
    Repository repository = repositoryManager.get(result.getParentId(Repository.class).get());
    return String.format("%s/%s#%s", repository.getNamespace(), repository.getName(), result.getEntity().getId());
  }

  private Repository findRepository(String namespace, String name) {
    return repositoryManager.get(new NamespaceAndName(namespace, name));
  }

  @Override
  public String getName() {
    return "list-pull-requests";
  }

  @Override
  public String getDescription() {
    return """
      Use this to list pull requests with optional filters. Filters can be combined.
      For example, to get pull requests for a single repository namespace, set this namespace in
      the filter. To get pull requests for a single repository only, set the namespace and name
      of this repository. To get a specific pull request of this repository, add the id of the pull request.
      All filters can be applied alone or in combination with other filters, if not stated otherwise for the filter.
      Ids of pull requests are unique only in the context of a single repository. So to get a concrete pull request,
      the repository namespace, the repository name, and the pull request id have to be set.
      """;
  }
}

@Getter
@Setter(AccessLevel.PACKAGE)
@ToString
class PullRequestListInput {
  @JsonPropertyDescription("Find pull requests only with this ID.")
  private String pullRequestId;
  @JsonPropertyDescription("Find pull requests only that belong to a repository with this namespace.")
  private String repositoryNamespace;
  @JsonPropertyDescription("Find pull requests only that belong to a repository with this name.")
  private String repositoryName;
  @JsonPropertyDescription("Find pull requests only that have this author.")
  private String authorUserId;
  @JsonPropertyDescription("Find pull requests only that have this reviewer.")
  private String reviewerUserId;
  @JsonPropertyDescription("Find pull requests only whose title contain this sub string. This check is case insensitive.")
  private String titleContains;
  @JsonPropertyDescription("Find pull requests only whose description contain this sub string. This check is case insensitive.")
  private String descriptionContains;
  @JsonPropertyDescription("Find pull requests only whose title or description contain this sub string. This check is case insensitive.")
  private String titleOrDescriptionContains;
  @JsonPropertyDescription("""
    Find pull requests with one of the given status
    (to find all closed pull requests set this to ['REJECTED', 'MERGED'];
     to find all pull requests that are in progress set this to ['OPEN', 'DRAFT']).
    """)
  private PullRequestStatus[] status;
  @JsonPropertyDescription("Find pull requests only with this target branch.")
  private String targetBranch;
  @JsonPropertyDescription("Find pull requests only with this source branch.")
  private String sourceBranch;
  @JsonPropertyDescription("Find pull requests only with this branch as source branch or target branch.")
  private String affectedBranch;
  @JsonPropertyDescription("""
    Return the pull requests ordered by the given field.
    By default, the result will be in ascending order.
    Use `order` if a descending order is needed.
    """)
  private OrderBy orderBy;
  @JsonPropertyDescription("""
    Specifies, whether to return the pull requests ordered ascending or descending.
    To make this work, `orderBy` has to be set, too.
    The default order is ascending.
    """)
  private Order order = Order.ASCENDING;
  @JsonPropertyDescription("If set to `true`, only pull requests with open tasks will be returned.")
  private boolean withOpenTasksOnly = false;
  @JsonPropertyDescription("""
    Return only the first n pull requests that match the other given filters.
    If this is set to 0, the result is not limited at all.
    By default, only the first 10 pull requests are returned.
    """)
  private int limit = 10;
  @JsonPropertyDescription("""
    If this is set to `true`, only pull requests with obstacles will be returned.
    If this is selected, the obstacles will be included no matter how `includeObstacles` is set.
    """)
  private boolean withObstaclesOnly;
  @JsonPropertyDescription("""
    This selects, what kind of information detail will be returned for each pull request. The default
    ("NONE") will omit the structured data and only list the pull requests in form of repository namespace and
    name and the repository id. For "OVERVIEW", there will be structured data for each pull request containing
    the id, the repository, the date of creation, the date when the pull request was closed (if it is closed),
    the source branch, the target branch, the title, and the status ("DRAFT", "OPEN", "MERGED", or "REJECTED").
    For the detail level "FULL", additional information like the author, the reviser, the description, and
    more will be added.
    The last detail level "WITH_OBSTACLES" includes all details from level "FULL", but also computes all obstacles
    and includes them in the results. If `withObstaclesOnly` is set to `true`, the detail level of the results
    is always "WITH_OBSTACLES".
    """)
  private DETAIL_LEVEL detailLevel = DETAIL_LEVEL.NONE;

  public DETAIL_LEVEL getDetailLevel() {
    return isWithObstaclesOnly() ? DETAIL_LEVEL.WITH_OBSTACLES : detailLevel;
  }
}

enum OrderBy {
  ID, CREATION_DATE, LAST_MODIFICATION
}

enum Order {
  ASCENDING, DESCENDING
}

enum DETAIL_LEVEL {
  NONE, OVERVIEW, FULL, WITH_OBSTACLES
}
