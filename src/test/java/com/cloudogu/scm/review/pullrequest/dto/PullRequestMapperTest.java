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

package com.cloudogu.scm.review.pullrequest.dto;

import com.cloudogu.scm.review.BranchResolver;
import com.cloudogu.scm.review.TestData;
import com.cloudogu.scm.review.comment.service.CommentService;
import com.cloudogu.scm.review.config.service.BasePullRequestConfig;
import com.cloudogu.scm.review.config.service.ConfigService;
import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import com.cloudogu.scm.review.pullrequest.service.PullRequestService;
import com.cloudogu.scm.review.pullrequest.service.PullRequestStatus;
import com.google.common.collect.ImmutableSet;
import de.otto.edison.hal.Link;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.api.v2.resources.BranchLinkProvider;
import sonia.scm.repository.Branch;
import sonia.scm.repository.NamespaceAndName;
import sonia.scm.repository.Repository;
import sonia.scm.repository.api.Command;
import sonia.scm.repository.api.MergeStrategy;
import sonia.scm.repository.api.RepositoryService;
import sonia.scm.repository.api.RepositoryServiceFactory;
import sonia.scm.user.UserDisplayManager;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PullRequestMapperTest {

  private static final Repository REPOSITORY = new Repository("id-1", "git", "space", "x");

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private Subject subject;

  @Mock
  private BranchLinkProvider branchLinkProvider;
  @Mock
  private ConfigService configService;
  @Mock
  private UserDisplayManager userDisplayManager;
  @Mock
  private PullRequestService pullRequestService;
  @Mock
  private CommentService commentService;
  @Mock
  private RepositoryServiceFactory serviceFactory;
  @Mock
  private BranchResolver branchResolver;
  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private RepositoryService service;

  @InjectMocks
  private PullRequestMapperImpl mapper;

  @BeforeEach
  void initSubject() {
    ThreadContext.bind(subject);
  }

  @AfterEach
  void tearDownSubject() {
    ThreadContext.unbindSubject();
  }

  @Nested
  class WithDefaults {

    @BeforeEach
    void mockDefaults() {
      when(subject.getPrincipals().getPrimaryPrincipal()).thenReturn("dent");
      when(branchLinkProvider.get(any(NamespaceAndName.class), anyString())).thenReturn("link");
      when(configService.evaluateConfig(REPOSITORY)).thenReturn(new BasePullRequestConfig());
    }

    @Test
    void shouldMapConvertToPrLinkForCreator() {
      when(subject.isPermitted(any(String.class))).thenReturn(false);

      PullRequest pullRequest = TestData.createPullRequest();
      pullRequest.setStatus(PullRequestStatus.DRAFT);
      pullRequest.setAuthor("dent");
      PullRequestDto dto = mapper.map(pullRequest, REPOSITORY);

      assertThat(dto.getLinks().getLinkBy("convertToPR")).isPresent();
    }

    @Test
    void shouldNotMapConvertToPrLink() {
      when(subject.isPermitted(any(String.class))).thenReturn(false);

      PullRequest pullRequest = TestData.createPullRequest();
      pullRequest.setStatus(PullRequestStatus.DRAFT);
      pullRequest.setAuthor("someone else");
      PullRequestDto dto = mapper.map(pullRequest, REPOSITORY);

      assertThat(dto.getLinks().getLinkBy("convertToPR")).isEmpty();
    }

    @Test
    void shouldMapRejectForAdmin() {
      PullRequest pullRequest = TestData.createPullRequest();
      PullRequestDto dto = mapper.map(pullRequest, REPOSITORY);

      assertThat(dto.getLinks().getLinkBy("reject")).isPresent();
    }

    @Test
    void shouldMapRejectForAuthor() {
      when(subject.getPrincipals().getPrimaryPrincipal()).thenReturn("trillian");

      PullRequest pullRequest = TestData.createPullRequest();
      pullRequest.setAuthor("trillian");
      PullRequestDto dto = mapper.map(pullRequest, REPOSITORY);

      assertThat(dto.getLinks().getLinkBy("reject")).isPresent();
    }

    @Test
    void shouldMapNotReject() {
      when(subject.getPrincipals().getPrimaryPrincipal()).thenReturn("trillian");

      PullRequest pullRequest = TestData.createPullRequest();
      PullRequestDto dto = mapper.map(pullRequest, REPOSITORY);

      assertThat(dto.getLinks().getLinkBy("reject")).isEmpty();
    }

    @Test
    void shouldMapConvertToPrLinkForMerger() {
      when(subject.isPermitted("repository:commentPullRequest:id-1")).thenReturn(true);
      when(subject.isPermitted("repository:push:id-1")).thenReturn(true);
      when(subject.isPermitted("repository:mergePullRequest:id-1")).thenReturn(true);

      PullRequest pullRequest = TestData.createPullRequest();
      pullRequest.setStatus(PullRequestStatus.DRAFT);
      PullRequestDto dto = mapper.map(pullRequest, REPOSITORY);

      assertThat(dto.getLinks().getLinkBy("convertToPR")).isPresent();
    }

    @Test
    void shouldMapPullRequestWithMergeLink() {
      when(serviceFactory.create(REPOSITORY)).thenReturn(service);
      when(service.isSupported(Command.MERGE)).thenReturn(true);
      when(service.getMergeCommand().getSupportedMergeStrategies()).thenReturn(ImmutableSet.of(MergeStrategy.MERGE_COMMIT));
      when(subject.isPermitted("repository:commentPullRequest:id-1")).thenReturn(true);
      when(subject.isPermitted("repository:push:id-1")).thenReturn(true);
      when(subject.isPermitted("repository:mergePullRequest:id-1")).thenReturn(true);

      PullRequest pullRequest = TestData.createPullRequest();
      pullRequest.addLabel("preview");
      PullRequestDto dto = mapper.map(pullRequest, REPOSITORY);

      assertThat(dto.getLinks().isEmpty()).isFalse();
      assertThat(dto.getLinks().getLinkBy("merge")).isPresent();
      assertThat(dto.getLinks().getLinkBy("defaultCommitMessage")).isPresent();

      PullRequestMapper.DefaultConfigDto defaultConfig = dto.getEmbedded().getItemsBy("defaultConfig", PullRequestMapper.DefaultConfigDto.class).get(0);
      assertThat(defaultConfig.getMergeStrategy()).isEqualTo("MERGE_COMMIT");
      assertThat(defaultConfig.isDeleteBranchOnMerge()).isFalse();

    }

    @Test
    void shouldMapPullRequestWithEmergencyMergeLink() {
      when(serviceFactory.create(REPOSITORY)).thenReturn(service);
      when(service.isSupported(Command.MERGE)).thenReturn(true);
      when(service.getMergeCommand().getSupportedMergeStrategies()).thenReturn(ImmutableSet.of(MergeStrategy.MERGE_COMMIT));
      when(subject.isPermitted("repository:commentPullRequest:id-1")).thenReturn(true);
      when(subject.isPermitted("repository:push:id-1")).thenReturn(true);
      when(subject.isPermitted("repository:mergePullRequest:id-1")).thenReturn(true);
      when(subject.isPermitted("repository:performEmergencyMerge:id-1")).thenReturn(true);

      PullRequest pullRequest = TestData.createPullRequest();
      PullRequestDto dto = mapper.map(pullRequest, REPOSITORY);

      assertThat(dto.getLinks().isEmpty()).isFalse();
      assertThat(dto.getLinks().getLinkBy("emergencyMerge")).isPresent();
    }

    @Test
    void shouldAppendSourceAndTargetBranchLinks() {
      String sourceLink = "/api/v2/source";
      String targetLink = "/api/v2/target";
      when(branchLinkProvider.get(REPOSITORY.getNamespaceAndName(), "develop")).thenReturn(sourceLink);
      when(branchLinkProvider.get(REPOSITORY.getNamespaceAndName(), "master")).thenReturn(targetLink);

      PullRequest pullRequest = TestData.createPullRequest();
      PullRequestDto dto = mapper.map(pullRequest, REPOSITORY);

      assertThat(dto.getLinks().isEmpty()).isFalse();
      Optional<Link> sourceBranch = dto.getLinks().getLinkBy("sourceBranch");
      assertThat(sourceBranch).isPresent();
      assertThat(sourceBranch.get().getHref()).isEqualTo(sourceLink);
      Optional<Link> targetBranch = dto.getLinks().getLinkBy("targetBranch");
      assertThat(targetBranch).isPresent();
      assertThat(targetBranch.get().getHref()).isEqualTo(targetLink);
    }

    @Test
    void shouldAppendConflictsLinks() {
      PullRequest pullRequest = TestData.createPullRequest();
      PullRequestDto dto = mapper.map(pullRequest, REPOSITORY);

      assertThat(dto.getLinks().isEmpty()).isFalse();
      Optional<Link> conflictsLink = dto.getLinks().getLinkBy("mergeConflicts");
      assertThat(conflictsLink).isPresent();
      assertThat(conflictsLink.get().getHref()).isEqualTo("/v2/merge/space/x/id/conflicts");
    }

    @Test
    void shouldAppendAvailableLabelsAsEmbedded() {
      BasePullRequestConfig config = new BasePullRequestConfig();
      config.setLabels(Set.of("1", "2"));
      when(configService.evaluateConfig(REPOSITORY)).thenReturn(config);
      PullRequestDto dto = mapper.map(TestData.createPullRequest(), REPOSITORY);

      assertThat(dto.getEmbedded().hasItem("availableLabels")).isTrue();
      LabelsDto labelsDto = (LabelsDto) dto.getEmbedded().getItemsBy("availableLabels").get(0);
      assertThat(labelsDto.getAvailableLabels()).hasSize(2);
    }

    @Test
    void shouldMapPullRequestApprover() {
      PullRequest pullRequest = TestData.createPullRequest();
      pullRequest.addApprover("deletedUser");

      PullRequestDto dto = mapper.map(pullRequest, REPOSITORY);

      assertThat(dto.getReviewer()).extracting("id").containsExactly("deletedUser");
    }

    @Test
    void shouldMapPullRequestReviser() {
      PullRequest pullRequest = TestData.createPullRequest();
      pullRequest.setReviser("trillian");

      PullRequestDto dto = mapper.map(pullRequest, REPOSITORY);

      assertThat(dto.getReviser().getDisplayName()).isEqualTo("trillian");
    }

    @Test
    void shouldAddLinkForWorkflowResultIfPullRequestIsOpen() {
      PullRequest pullRequest = TestData.createPullRequest();

      PullRequestDto dto = mapper.map(pullRequest, REPOSITORY);

      assertThat(dto.getLinks().getLinkBy("workflowResult"))
        .get()
        .extracting("href")
        .isEqualTo("/v2/pull-requests/space/x/id/workflow/");
    }

    @Test
    void shouldAddLinkForWorkflowResultIfPullRequestIsDraft() {
      PullRequest pullRequest = TestData.createPullRequest();
      pullRequest.setStatus(PullRequestStatus.DRAFT);

      PullRequestDto dto = mapper.map(pullRequest, REPOSITORY);

      assertThat(dto.getLinks().getLinkBy("workflowResult"))
        .get()
        .extracting("href")
        .isEqualTo("/v2/pull-requests/space/x/id/workflow/");
    }

    @Test
    void shouldNotAddLinkForWorkflowResultIfPullRequestIsRejected() {
      PullRequest pullRequest = TestData.createPullRequest();
      pullRequest.setStatus(PullRequestStatus.REJECTED);

      PullRequestDto dto = mapper.map(pullRequest, REPOSITORY);

      assertThat(dto.getLinks().getLinkBy("workflowResult"))
        .isEmpty();
    }

    @Test
    void shouldNotAddLinkForWorkflowResultIfPullRequestIsMerged() {
      PullRequest pullRequest = TestData.createPullRequest();
      pullRequest.setStatus(PullRequestStatus.MERGED);

      PullRequestDto dto = mapper.map(pullRequest, REPOSITORY);

      assertThat(dto.getLinks().getLinkBy("workflowResult"))
        .isEmpty();
    }

    @Test
    void shouldAddReopenLink() {
      lenient().when(subject.isPermitted("repository:createPullRequest:id-1")).thenReturn(true);
      PullRequest pullRequest = TestData.createPullRequest();
      pullRequest.setStatus(PullRequestStatus.REJECTED);

      when(pullRequestService.checkIfPullRequestIsValid(REPOSITORY, pullRequest.getSource(), pullRequest.getTarget()))
        .thenReturn(PullRequestCheckResultDto.PullRequestCheckStatus.PR_VALID);
      when(branchResolver.find(REPOSITORY, pullRequest.getSource())).thenReturn(Optional.of(mock(Branch.class)));
      when(branchResolver.find(REPOSITORY, pullRequest.getTarget())).thenReturn(Optional.of(mock(Branch.class)));

      PullRequestDto dto = mapper.map(pullRequest, REPOSITORY);

      assertThat(dto.getLinks().getLinkBy("reopen"))
              .get()
              .extracting("href")
              .isEqualTo("/v2/pull-requests/space/x/id/reopen");
    }

    @Test
    void shouldNotAddReopenLinkWithoutPermission() {
      PullRequest pullRequest = TestData.createPullRequest();
      pullRequest.setStatus(PullRequestStatus.REJECTED);

      PullRequestDto dto = mapper.map(pullRequest, REPOSITORY);

      assertThat(dto.getLinks().getLinkBy("reopen"))
              .isEmpty();
    }

    @Test
    void shouldNotAddReopenLinkIfNotRejected() {
      lenient().when(subject.isPermitted("repository:createPullRequest:*")).thenReturn(true);
      PullRequest pullRequest = TestData.createPullRequest();
      pullRequest.setStatus(PullRequestStatus.MERGED);

      PullRequestDto dto = mapper.map(pullRequest, REPOSITORY);

      assertThat(dto.getLinks().getLinkBy("reopen"))
              .isEmpty();
    }

    @Test
    void shouldNotAddReopenLinkIfSourceBranchIsAbsent() {
      lenient().when(subject.isPermitted("repository:createPullRequest:*")).thenReturn(true);
      PullRequest pullRequest = TestData.createPullRequest();
      pullRequest.setStatus(PullRequestStatus.REJECTED);

      when(branchResolver.find(REPOSITORY, pullRequest.getSource())).thenReturn(Optional.empty());
      when(branchResolver.find(REPOSITORY, pullRequest.getTarget())).thenReturn(Optional.of(mock(Branch.class)));

      PullRequestDto dto = mapper.map(pullRequest, REPOSITORY);

      assertThat(dto.getLinks().getLinkBy("reopen"))
        .isEmpty();
    }

    @Test
    void shouldNotAddReopenLinkIfTargetBranchIsAbsent() {
      lenient().when(subject.isPermitted("repository:createPullRequest:*")).thenReturn(true);
      PullRequest pullRequest = TestData.createPullRequest();
      pullRequest.setStatus(PullRequestStatus.REJECTED);

      when(branchResolver.find(REPOSITORY, pullRequest.getSource())).thenReturn(Optional.of(mock(Branch.class)));
      when(branchResolver.find(REPOSITORY, pullRequest.getTarget())).thenReturn(Optional.empty());

      PullRequestDto dto = mapper.map(pullRequest, REPOSITORY);

      assertThat(dto.getLinks().getLinkBy("reopen"))
        .isEmpty();
    }

    @Test
    void shouldNotAddReopenLinkIfThereAreNoChangesInPr() {
      lenient().when(subject.isPermitted("repository:createPullRequest:*")).thenReturn(true);
      PullRequest pullRequest = TestData.createPullRequest();
      pullRequest.setStatus(PullRequestStatus.REJECTED);
      pullRequest.setTarget("develop");
      pullRequest.setSource("develop");

      when(branchResolver.find(REPOSITORY, pullRequest.getSource())).thenReturn(Optional.of(mock(Branch.class)));
      when(branchResolver.find(REPOSITORY, pullRequest.getTarget())).thenReturn(Optional.of(mock(Branch.class)));

      PullRequestDto dto = mapper.map(pullRequest, REPOSITORY);

      assertThat(dto.getLinks().getLinkBy("reopen"))
        .isEmpty();
    }

    @Test
    void shouldEmbedDefaultConfig() {
      BasePullRequestConfig config = new BasePullRequestConfig();
      config.setDefaultMergeStrategy(MergeStrategy.REBASE);
      config.setDeleteBranchOnMerge(true);
      when(configService.evaluateConfig(REPOSITORY)).thenReturn(config);
      PullRequest pullRequest = TestData.createPullRequest();

      PullRequestDto dto = mapper.map(pullRequest, REPOSITORY);

      PullRequestMapper.DefaultConfigDto defaultConfig = dto.getEmbedded().getItemsBy("defaultConfig", PullRequestMapper.DefaultConfigDto.class).get(0);
      assertThat(defaultConfig.getMergeStrategy()).isEqualTo("REBASE");
      assertThat(defaultConfig.isDeleteBranchOnMerge()).isTrue();
    }

    @Test
    void shouldAddCheckLink() {
      PullRequest pullRequest = TestData.createPullRequest();

      PullRequestDto dto = mapper.map(pullRequest, REPOSITORY);

      assertThat(dto.getLinks().getLinkBy("check"))
        .get()
        .extracting("href")
        .isEqualTo("/v2/pull-requests/space/x/id/check");
    }
  }

  @Test
  void shouldMapLabels() {
    PullRequest pullRequest = TestData.createPullRequest();
    pullRequest.addLabel("1");
    pullRequest.addLabel("2");

    PullRequestDto dto = mapper.map(pullRequest);
    assertThat(dto.getLabels()).hasSize(2);
    assertThat(dto.getLabels()).containsExactly("1", "2");

    pullRequest.removeLabel("1");

    dto = mapper.map(pullRequest);

    assertThat(dto.getLabels()).hasSize(1);
    assertThat(dto.getLabels()).contains( "2");
  }

  @Test
  void shouldNotSetReviewMarksToNull() {
    PullRequest pullRequest = mapper.map(new PullRequestDto());

    assertThat(pullRequest.getReviewMarks())
      .isEmpty();
  }
}
