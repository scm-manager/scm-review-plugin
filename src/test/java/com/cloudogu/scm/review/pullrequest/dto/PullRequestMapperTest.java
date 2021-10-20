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

import com.cloudogu.scm.review.TestData;
import com.cloudogu.scm.review.comment.service.CommentService;
import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import com.cloudogu.scm.review.pullrequest.service.PullRequestService;
import com.cloudogu.scm.review.pullrequest.service.PullRequestStatus;
import com.google.common.collect.ImmutableSet;
import de.otto.edison.hal.Link;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.api.v2.resources.BranchLinkProvider;
import sonia.scm.repository.NamespaceAndName;
import sonia.scm.repository.Repository;
import sonia.scm.repository.api.Command;
import sonia.scm.repository.api.MergeStrategy;
import sonia.scm.repository.api.RepositoryService;
import sonia.scm.repository.api.RepositoryServiceFactory;
import sonia.scm.user.UserDisplayManager;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PullRequestMapperTest {

  private static final Repository REPOSITORY = new Repository("id-1", "git", "space", "x");

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private Subject subject;

  @Mock
  private BranchLinkProvider branchLinkProvider;
  @Mock
  private UserDisplayManager userDisplayManager;
  @Mock
  private PullRequestService pullRequestService;
  @Mock
  private CommentService commentService;
  @Mock
  private RepositoryServiceFactory serviceFactory;
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

  @BeforeEach
  void mockDefaults() {
    when(subject.getPrincipals().getPrimaryPrincipal()).thenReturn("dent");
    when(branchLinkProvider.get(any(NamespaceAndName.class), anyString())).thenReturn("link");
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
    PullRequestDto dto = mapper.map(pullRequest, REPOSITORY);

    assertThat(dto.getLinks().isEmpty()).isFalse();
    assertThat(dto.getLinks().getLinkBy("merge")).isPresent();
    assertThat(dto.getLinks().getLinkBy("defaultCommitMessage")).isPresent();
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
}
