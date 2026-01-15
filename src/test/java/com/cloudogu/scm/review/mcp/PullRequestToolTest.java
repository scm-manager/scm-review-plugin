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
import com.cloudogu.scm.review.comment.service.Comment;
import com.cloudogu.scm.review.comment.service.CommentStoreFactory;
import com.cloudogu.scm.review.comment.service.CommentType;
import com.cloudogu.scm.review.pullrequest.service.MergeObstacle;
import com.cloudogu.scm.review.pullrequest.service.MergeService;
import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import com.cloudogu.scm.review.pullrequest.service.PullRequestStatus;
import com.cloudogu.scm.review.pullrequest.service.PullRequestStoreFactory;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryManager;
import sonia.scm.store.QueryableMutableStore;
import sonia.scm.store.QueryableStoreExtension;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith({QueryableStoreExtension.class, MockitoExtension.class})
@QueryableStoreExtension.QueryableTypes({PullRequest.class, Comment.class})
class PullRequestToolTest {

  @Mock
  private RepositoryManager repositoryManager;
  @Mock
  private PullRequestMcpMapper pullRequestMapper;
  @Mock
  private MergeService mergeService;
  private PullRequestTool tool;

  private final Map<String, PullRequest> pullRequests = new HashMap<>();
  private final PullRequestListInput input = new PullRequestListInput();

  private Repository repository1;
  private Repository repository2;
  private PullRequest pullRequest1;
  private PullRequest pullRequest2;
  private PullRequest pullRequest3;

  @BeforeEach
  void setUpTool(PullRequestStoreFactory pullRequestStoreFactory, CommentStoreFactory commentStoreFactory) {
    tool = new PullRequestTool(pullRequestStoreFactory, commentStoreFactory, repositoryManager, pullRequestMapper, mergeService);
  }

  @BeforeEach
  void prepareData(PullRequestStoreFactory pullRequestStoreFactory, CommentStoreFactory commentStoreFactory) {
    repository1 = new Repository("42", "git", "hitchhiker", "hog");
    mockRepository(repository1);
    repository2 = new Repository("23", "git", "hacker", "secret");
    mockRepository(repository2);
    lenient().when(repositoryManager.getAll()).thenReturn(List.of(repository1, repository2));

    pullRequest1 = new PullRequest("1", "feature/hog", "develop");
    pullRequest1.setAuthor("dent");
    pullRequest1.setReviewer(Map.of("trillian", true));
    pullRequest1.setTitle("Heart Of Gold");
    pullRequest1.setDescription("A spacecraft equipped with Infinite Improbability Drive");
    pullRequest1.setStatus(PullRequestStatus.MERGED);
    pullRequest1.setCreationDate(Instant.parse("1978-01-01T00:00:00Z"));
    pullRequest1.setLastModified(Instant.parse("2020-01-01T00:00:00Z"));
    mockPullRequest(pullRequestStoreFactory, repository1, pullRequest1);
    try (QueryableMutableStore<Comment> commentStore = commentStoreFactory.getMutable(repository1.getId(), pullRequest1.getId())) {
      Comment comment = Comment.createComment("42", "Hijack Heart of Gold", "trillian", null);
      comment.setType(CommentType.TASK_DONE);
      commentStore.put(comment);
    }

    pullRequest2 = new PullRequest("2", "develop", "main");
    pullRequest2.setAuthor("trillian");
    pullRequest2.setTitle("Vogons");
    pullRequest2.setDescription("A race of unpleasant and bureaucratic aliens");
    pullRequest2.setStatus(PullRequestStatus.OPEN);
    pullRequest2.setCreationDate(Instant.parse("2001-01-01T00:00:00Z"));
    pullRequest2.setLastModified(Instant.parse("2001-01-01T00:00:00Z"));
    mockPullRequest(pullRequestStoreFactory, repository1, pullRequest2);
    try (QueryableMutableStore<Comment> commentStore = commentStoreFactory.getMutable(repository1.getId(), pullRequest2.getId())) {
      Comment comment = Comment.createComment("42", "Destroy Earth", "trillian", null);
      comment.setType(CommentType.TASK_TODO);
      commentStore.put(comment);
    }

    pullRequest3 = new PullRequest("1", "feature/neo", "main");
    pullRequest3.setTitle("Neo");
    pullRequest3.setAuthor("smith");
    pullRequest3.setStatus(PullRequestStatus.DRAFT);
    pullRequest3.setCreationDate(Instant.parse("1979-03-24T00:00:00Z"));
    pullRequest3.setLastModified(Instant.parse("2021-01-01T00:00:00Z"));
    mockPullRequest(pullRequestStoreFactory, repository2, pullRequest3);
  }

  @Test
  void shouldListPullRequestsWithoutData() {
    ToolResult result = tool.execute(input);

    assertThat(result.getContent().get(0))
      .isEqualTo("""
        STATUS: [SUCCESS] Found 3 pull requests.
        ---------------------------------------------------------
        Repository | Pull Request ID | Status | Author | Source Branch | Target Branch | Title
        ---|---|---|---|---|---|---
        hitchhiker/hog | 1 | MERGED | dent | feature/hog | develop | Heart Of Gold
        hitchhiker/hog | 2 | OPEN | trillian | develop | main | Vogons
        hacker/secret | 1 | DRAFT | smith | feature/neo | main | Neo
        """
      );
    Map<String, Object> structuredContent = result.getStructuredContent();
    assertThat(structuredContent).isNullOrEmpty();
  }

  @Nested
  class WithoutDetailData {

    @BeforeEach
    void setForDetails() {
      input.setDetailLevel(DETAIL_LEVEL.FULL);
    }

    @Test
    void shouldListAllPullRequests() {
      ToolResult result = tool.execute(input);

      assertResultWith(result, true, "hitchhiker/hog#1 [MERGED]", "hitchhiker/hog#2 [OPEN]", "hacker/secret#1 [DRAFT]");
    }

    @Test
    void shouldListPullRequestsByNamespace() {
      input.setRepositoryNamespace("hitchhiker");

      ToolResult result = tool.execute(input);

      assertResultWith(result, true, "hitchhiker/hog#1 [MERGED]", "hitchhiker/hog#2 [OPEN]");
    }

    @Test
    void shouldListPullRequestsByName() {
      input.setRepositoryName("hog");

      ToolResult result = tool.execute(input);

      assertResultWith(result, true, "hitchhiker/hog#1 [MERGED]", "hitchhiker/hog#2 [OPEN]");
    }

    @Test
    void shouldListPullRequestsByNamespaceAndName() {
      input.setRepositoryNamespace("hitchhiker");
      input.setRepositoryName("hog");

      ToolResult result = tool.execute(input);

      assertResultWith(result, true, "hitchhiker/hog#1 [MERGED]", "hitchhiker/hog#2 [OPEN]");
    }

    @Test
    void shouldListPullRequestsByAuthor() {
      input.setAuthorUserId("dent");

      ToolResult result = tool.execute(input);

      assertResultWith(result, true, "hitchhiker/hog#1 [MERGED]");
    }

    @Test
    void shouldListPullRequestsByReviewer() {
      input.setReviewerUserId("trillian");

      ToolResult result = tool.execute(input);

      assertResultWith(result, true, "hitchhiker/hog#1 [MERGED]");
    }

    @Test
    void shouldListPullRequestsByTitle() {
      input.setTitleContains("heart");

      ToolResult result = tool.execute(input);

      assertResultWith(result, true, "hitchhiker/hog#1 [MERGED]");
    }

    @Test
    void shouldListPullRequestsByDescription() {
      input.setDescriptionContains("Bureaucratic");

      ToolResult result = tool.execute(input);

      assertResultWith(result, true, "hitchhiker/hog#2 [OPEN]");
    }

    @Test
    void shouldListPullRequestsByTitleOrDescription() {
      input.setTitleOrDescriptionContains("of");

      ToolResult result = tool.execute(input);

      assertResultWith(result, true, "hitchhiker/hog#1 [MERGED]", "hitchhiker/hog#2 [OPEN]");
    }

    @Test
    void shouldListPullRequestsByStatus() {
      input.setStatus(new PullRequestStatus[]{PullRequestStatus.DRAFT, PullRequestStatus.OPEN});

      ToolResult result = tool.execute(input);

      assertResultWith(result, true, "hitchhiker/hog#2 [OPEN]", "hacker/secret#1 [DRAFT]");
    }

    @Test
    void shouldListPullRequestsBySourceBranch() {
      input.setSourceBranch("develop");

      ToolResult result = tool.execute(input);

      assertResultWith(result, true, "hitchhiker/hog#2 [OPEN]");
    }

    @Test
    void shouldListPullRequestsByTargetBranch() {
      input.setTargetBranch("main");

      ToolResult result = tool.execute(input);

      assertResultWith(result, true, "hitchhiker/hog#2 [OPEN]", "hacker/secret#1 [DRAFT]");
    }

    @Test
    void shouldListPullRequestsByAffectedBranch() {
      input.setAffectedBranch("develop");

      ToolResult result = tool.execute(input);

      assertResultWith(result, true, "hitchhiker/hog#1 [MERGED]", "hitchhiker/hog#2 [OPEN]");
    }

    @Test
    void shouldListPullRequestsOrderedById() {
      input.setOrderBy(OrderBy.ID);

      ToolResult result = tool.execute(input);

      assertResultWith(result, true, "hitchhiker/hog#1 [MERGED]", "hacker/secret#1 [DRAFT]", "hitchhiker/hog#2 [OPEN]")
        .inExactOrder();
    }

    @Test
    void shouldListPullRequestsOrderedByDescendingId() {
      input.setOrderBy(OrderBy.ID);
      input.setOrder(Order.DESCENDING);

      ToolResult result = tool.execute(input);

      assertResultWith(result, true, "hitchhiker/hog#2 [OPEN]", "hitchhiker/hog#1 [MERGED]", "hacker/secret#1 [DRAFT]")
        .inExactOrder();
    }

    @Test
    void shouldListPullRequestsOrderedByCreationDate() {
      input.setOrderBy(OrderBy.CREATION_DATE);

      ToolResult result = tool.execute(input);

      assertResultWith(result, true, "hitchhiker/hog#1 [MERGED]", "hacker/secret#1 [DRAFT]", "hitchhiker/hog#2 [OPEN]")
        .inExactOrder();
    }

    @Test
    void shouldListPullRequestsOrderedByModificationDate() {
      input.setOrderBy(OrderBy.LAST_MODIFICATION);

      ToolResult result = tool.execute(input);

      assertResultWith(result, true, "hitchhiker/hog#2 [OPEN]", "hitchhiker/hog#1 [MERGED]", "hacker/secret#1 [DRAFT]")
        .inExactOrder();
    }

    @Test
    void shouldLimitPullRequests() {
      input.setOrderBy(OrderBy.ID);
      input.setLimit(2);

      ToolResult result = tool.execute(input);

      assertResultWith(result, true, "hitchhiker/hog#1 [MERGED]", "hacker/secret#1 [DRAFT]")
        .inExactOrder();
    }

    @Test
    void shouldListPullRequestsWithOpenTasksOnly() {
      input.setWithOpenTasksOnly(true);

      ToolResult result = tool.execute(input);

      assertResultWith(result, true, "hitchhiker/hog#2 [OPEN]");
    }

    @Test
    void shouldListPullRequestsWithFullDetails() {
      input.setDetailLevel(DETAIL_LEVEL.FULL);

      ToolResult result = tool.execute(input);

      assertResultWith(result, true, "hitchhiker/hog#1 [MERGED]", "hitchhiker/hog#2 [OPEN]", "hacker/secret#1 [DRAFT]");
      assertThat(result.getStructuredContent().values().stream().flatMap(m -> ((Map) m).values().stream()))
        .hasOnlyElementsOfType(PullRequestDetailMcp.class);
    }

    @Nested
    class WithObstacles {

      private PullRequestDetailMcp mappedPRWithObstacle;

      @BeforeEach
      void mockMapperWithObstacles() {
        mappedPRWithObstacle = pullRequestMapper.mapDetails(pullRequest1, repository1);
        mappedPRWithObstacle.setMergeObstacles(List.of("no humans"));
        when(pullRequestMapper.mapWithObstacles(pullRequest1, repository1))
          .thenReturn(mappedPRWithObstacle);
      }

      @Test
      void shouldListPullRequestsWithObstacles() {
        input.setDetailLevel(DETAIL_LEVEL.WITH_OBSTACLES);
        input.setLimit(1);

        ToolResult result = tool.execute(input);

        assertResultWith(result, true, "hitchhiker/hog#1 [MERGED]");
        assertThat(result.getStructuredContent())
          .values()
          .hasOnlyElementsOfType(Map.class);
        assertThat(result.getStructuredContent().get("hitchhiker/hog"))
          .asInstanceOf(InstanceOfAssertFactories.map(String.class, Map.class))
          .extracting("1")
          .extracting("mergeObstacles")
          .asList()
          .containsExactly("no humans");
      }

      @Test
      void shouldListPullRequestsWithObstaclesOnly() {
        when(mergeService.getObstacles(repository1, pullRequest1))
          .thenReturn(List.of(new MergeObstacle() {
            @Override
            public String getMessage() {
              return "no humans";
            }

            @Override
            public String getKey() {
              return "test";
            }
          }));
        input.setWithObstaclesOnly(true);

        ToolResult result = tool.execute(input);

        assertResultWith(result, true, "hitchhiker/hog#1 [MERGED]");
        assertThat(
          result
            .getStructuredContent()
            .get("hitchhiker/hog")
        ).asInstanceOf(InstanceOfAssertFactories.map(String.class, Object.class))
          .extracting("1")
          .isSameAs(mappedPRWithObstacle);
      }
    }
  }

  private FurtherChecks assertResultWith(ToolResult result, boolean withDetails, String... pullRequestStrings) {
    String content = result.getContent().get(0);
    assertThat(content)
      .contains("Found " + pullRequestStrings.length);
    if (withDetails) {
      assertThat(content)
        .contains("Detailed metadata ");
    }
    String table = content.substring(content.indexOf("---|"));
    table = table.substring(table.indexOf("\n") + 1);
    String[] actualPullRequestListContent =
      Arrays.stream(table.split("\n"))
        .map(line -> {
          String[] fields = line.split("\\W\\|\\W");
          return String.format("%s#%s [%s]", fields[0], fields[1], fields[2]);
        })
        .toArray(String[]::new);
    assertThat(actualPullRequestListContent).hasSize(pullRequestStrings.length);

    assertThat(actualPullRequestListContent)
      .containsExactlyInAnyOrder(pullRequestStrings);
    Map<String, Object> structuredContent = result.getStructuredContent();
    assertThat(structuredContent.values().stream().mapToInt(m -> ((Map<String, Object>) m).size()).sum()).isEqualTo(pullRequestStrings.length);
    Arrays.stream(pullRequestStrings).forEach(
      pullRequestString ->
      {
        String repositoryKey = pullRequestString.substring(0, pullRequestString.indexOf('#'));
        String prId = pullRequestString.substring(pullRequestString.indexOf('#') + 1, pullRequestString.indexOf(' '));
        assertThat(((Map<String, Object>) structuredContent.get(repositoryKey)).get(prId))
          .isInstanceOf(PullRequestOverviewMcp.class)
          .extracting("id")
          .isEqualTo(pullRequests.get(pullRequestString.substring(0, pullRequestString.indexOf(' '))).getId());
      }
    );
    return () -> assertThat(actualPullRequestListContent).containsExactly(pullRequestStrings);
  }

  private interface FurtherChecks {
    void inExactOrder();
  }

  private void mockRepository(Repository repository1) {
    lenient().when(repositoryManager.get(repository1.getId())).thenReturn(repository1);
    lenient().when(repositoryManager.get(repository1.getNamespaceAndName())).thenReturn(repository1);
  }

  private void mockPullRequest(PullRequestStoreFactory storeFactory, Repository repository, PullRequest pullRequest) {
    try (QueryableMutableStore<PullRequest> store = storeFactory.getMutable(repository)) {
      store.put(pullRequest);
    }
    PullRequestOverviewMcp overview = new PullRequestOverviewMcp();
    overview.setRepository(new PullRequestOverviewMcp.Repository(repository.getNamespace(), repository.getName()));
    overview.setId(pullRequest.getId());
    lenient().when(pullRequestMapper.mapOverview(pullRequest, repository))
      .thenReturn(overview);
    PullRequestDetailMcp detail = new PullRequestDetailMcp();
    detail.setRepository(new PullRequestOverviewMcp.Repository(repository.getNamespace(), repository.getName()));
    detail.setId(pullRequest.getId());
    lenient().when(pullRequestMapper.mapDetails(pullRequest, repository))
      .thenReturn(detail);
    pullRequests.put(String.format("%s#%s", repository.getNamespaceAndName(), pullRequest.getId()), pullRequest);
  }
}
