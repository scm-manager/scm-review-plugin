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

package com.cloudogu.scm.review.workflow;

import com.cloudogu.scm.review.TestData;
import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import org.junit.jupiter.api.Test;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryTestData;

import static org.assertj.core.api.Assertions.assertThat;

class AllReviewerApprovedRuleTest {

  private static final Repository REPOSITORY = RepositoryTestData.createHeartOfGold();

  private AllReviewerApprovedRule rule = new AllReviewerApprovedRule();

  @Test
  void shouldReturnSuccess() {
    PullRequest pullRequest = TestData.createPullRequest();
    pullRequest.addApprover("trillian");
    pullRequest.addApprover("dent");
    Context context = new Context(REPOSITORY, pullRequest);

    Result result = rule.validate(context);

    assertThat(result.isSuccess()).isTrue();
  }

  @Test
  void shouldReturnSuccessWithoutReviewers() {
    PullRequest pullRequest = TestData.createPullRequest();
    Context context = new Context(REPOSITORY, pullRequest);

    Result result = rule.validate(context);

    assertThat(result.isSuccess()).isTrue();
  }

  @Test
  void shouldReturnFailedIfReviewerHaveNotApproved() {
    PullRequest pullRequest = TestData.createPullRequest();
    pullRequest.addReviewer("trillian");
    pullRequest.addReviewer("dent");
    Context context = new Context(REPOSITORY, pullRequest);

    Result result = rule.validate(context);

    assertThat(result.isFailed()).isTrue();
  }

  @Test
  void shouldReturnFailedIfSingleReviewerHasNotApproved() {
    PullRequest pullRequest = TestData.createPullRequest();
    pullRequest.addApprover("trillian");
    pullRequest.addReviewer("dent");
    Context context = new Context(REPOSITORY, pullRequest);

    Result result = rule.validate(context);

    assertThat(result.isFailed()).isTrue();
  }
}
