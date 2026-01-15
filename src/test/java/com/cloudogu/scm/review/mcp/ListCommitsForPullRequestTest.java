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

import com.cloudogu.mcp.ToolListCommits;
import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import com.cloudogu.scm.review.pullrequest.service.PullRequestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.NotFoundException;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryTestData;
import sonia.scm.repository.api.LogCommandBuilder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListCommitsForPullRequestTest {

  private static final Repository REPOSITORY = RepositoryTestData.createHeartOfGold();

  @Mock
  private PullRequestService pullRequestService;
  @Mock(answer = Answers.RETURNS_SELF)
  private LogCommandBuilder logCommandBuilder;
  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private ToolListCommits.CompositeInput input;

  @InjectMocks
  private ListCommitsForPullRequest listCommitsForPullRequest;

  @Test
  void shouldDoNothingWithoutPRInput() {
    Optional<String> result = listCommitsForPullRequest.configure(REPOSITORY, logCommandBuilder, input);

    verifyNoInteractions(logCommandBuilder);
    assertThat(result).isEmpty();
  }

  @Nested
  class WithPullRequestId {

    @BeforeEach
    void mockRepositoryInput() {
      when(input.getBaseInput().getNamespace())
        .thenReturn("hitchhiker");
      when(input.getBaseInput().getName())
        .thenReturn("hog");
    }

    @Test
    void shouldConfigureForPullRequestId() {
      when(pullRequestService.get(REPOSITORY, "42"))
        .thenReturn(new PullRequest("42", "feature", "main"));

      ListCommitsForPullRequestInput prInput = new ListCommitsForPullRequestInput();
      prInput.setId("42");
      when(input.getExtensionInput("pullRequest"))
        .thenReturn(prInput);

      Optional<String> result = listCommitsForPullRequest.configure(REPOSITORY, logCommandBuilder, input);

      verify(logCommandBuilder).setStartChangeset("feature");
      verify(logCommandBuilder).setAncestorChangeset("main");
      assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnErrorForMissingPullRequest() {
      when(pullRequestService.get(REPOSITORY, "42"))
        .thenThrow(NotFoundException.class);

      ListCommitsForPullRequestInput prInput = new ListCommitsForPullRequestInput();
      prInput.setId("42");
      when(input.getExtensionInput("pullRequest"))
        .thenReturn(prInput);

      Optional<String> result = listCommitsForPullRequest.configure(REPOSITORY, logCommandBuilder, input);

      assertThat(result).isNotEmpty();
    }
  }
}
