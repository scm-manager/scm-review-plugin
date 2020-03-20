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
import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.repository.Branch;
import sonia.scm.repository.Branches;
import sonia.scm.repository.Changeset;
import sonia.scm.repository.ChangesetPagingResult;
import sonia.scm.repository.NamespaceAndName;
import sonia.scm.repository.api.BranchesCommandBuilder;
import sonia.scm.repository.api.LogCommandBuilder;
import sonia.scm.repository.api.RepositoryService;
import sonia.scm.repository.api.RepositoryServiceFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Collections.singletonList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BranchRevisionResolverTest {

  public static final NamespaceAndName NAMESPACE_AND_NAME = new NamespaceAndName("space", "X");
  @Mock
  RepositoryServiceFactory repositoryServiceFactory;
  @Mock
  RepositoryService repositoryService;
  @Mock
  LogCommandBuilder logCommandBuilder;
  @Mock
  BranchesCommandBuilder branchesCommandBuilder;

  @InjectMocks
  BranchRevisionResolver branchRevisionResolver;

  @Test
  void shouldGetRevisionsForPullRequest() throws IOException {
    when(repositoryServiceFactory.create(NAMESPACE_AND_NAME)).thenReturn(repositoryService);

    PullRequest pullRequest = TestData.createPullRequest();
    setUpBranches(pullRequest);

    mockSingleChangeset(pullRequest.getSource());
    mockSingleChangeset(pullRequest.getTarget());

    BranchRevisionResolver.RevisionResult revisions = branchRevisionResolver.getRevisions(NAMESPACE_AND_NAME, pullRequest);

    Assertions.assertThat(revisions.getSourceRevision()).isEqualTo(pullRequest.getSource() + "Id");
    Assertions.assertThat(revisions.getTargetRevision()).isEqualTo(pullRequest.getTarget() + "Id");
  }

  @Test
  void shouldGetEmptyRevisionsIfBranchDoesntExist() throws IOException {
    when(repositoryServiceFactory.create(NAMESPACE_AND_NAME)).thenReturn(repositoryService);
    setUpBranches("featureBranch");

    PullRequest pullRequest = TestData.createPullRequest();
    pullRequest.setSource("notExisting");
    pullRequest.setTarget("featureBranch");

    mockSingleChangeset(pullRequest.getSource());
    mockSingleChangeset(pullRequest.getTarget());

    BranchRevisionResolver.RevisionResult revisions = branchRevisionResolver.getRevisions(NAMESPACE_AND_NAME, pullRequest);

    // verify that get branches is called only once, because it is expensive
    verify(branchesCommandBuilder).getBranches();

    Assertions.assertThat(revisions.getSourceRevision()).isEqualTo("");
    Assertions.assertThat(revisions.getTargetRevision()).isEqualTo("featureBranchId");
  }

  private void setUpBranches(PullRequest pullRequest) throws IOException {
    setUpBranches(pullRequest.getSource(), pullRequest.getTarget());
  }

  private void setUpBranches(String... names) throws IOException {
    when(repositoryService.getBranchesCommand()).thenReturn(branchesCommandBuilder);
    List<Branch> branches = Arrays.stream(names).map(name -> Branch.normalBranch(name, name)).collect(Collectors.toList());

    when(branchesCommandBuilder.getBranches()).thenReturn(new Branches(branches));
  }

  private void mockSingleChangeset(String branch) throws IOException {
    when(repositoryService.getLogCommand()).thenReturn(logCommandBuilder);
    LogCommandBuilder subLogCommandBuilder = mock(LogCommandBuilder.class);
    lenient().when(logCommandBuilder.setBranch(branch)).thenReturn(subLogCommandBuilder);
    lenient().when(subLogCommandBuilder.setPagingStart(0)).thenReturn(subLogCommandBuilder);
    lenient().when(subLogCommandBuilder.setPagingLimit(1)).thenReturn(subLogCommandBuilder);
    Changeset changeset = new Changeset();
    changeset.setId(branch + "Id");
    lenient().when(subLogCommandBuilder.getChangesets()).thenReturn(new ChangesetPagingResult(1, singletonList(changeset)));
  }
}
