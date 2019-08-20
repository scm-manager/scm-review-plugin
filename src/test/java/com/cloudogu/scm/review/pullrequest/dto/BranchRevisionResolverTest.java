package com.cloudogu.scm.review.pullrequest.dto;

import com.cloudogu.scm.review.TestData;
import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.repository.Changeset;
import sonia.scm.repository.ChangesetPagingResult;
import sonia.scm.repository.NamespaceAndName;
import sonia.scm.repository.api.LogCommandBuilder;
import sonia.scm.repository.api.RepositoryService;
import sonia.scm.repository.api.RepositoryServiceFactory;

import java.io.IOException;

import static java.util.Collections.singletonList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
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

  @InjectMocks
  BranchRevisionResolver branchRevisionResolver;

  @Test
  void shouldGetRevisionsForPullRequest() throws IOException {
    when(repositoryServiceFactory.create(NAMESPACE_AND_NAME)).thenReturn(repositoryService);
    PullRequest pullRequest = TestData.createPullRequest();
    mockSingleChangeset(pullRequest.getSource());
    mockSingleChangeset(pullRequest.getTarget());

    BranchRevisionResolver.RevisionResult revisions = branchRevisionResolver.getRevisions(NAMESPACE_AND_NAME, pullRequest);

    Assertions.assertThat(revisions.getSourceRevision()).isEqualTo(pullRequest.getSource() + "Id");
    Assertions.assertThat(revisions.getTargetRevision()).isEqualTo(pullRequest.getTarget() + "Id");
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
