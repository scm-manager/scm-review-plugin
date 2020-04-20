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
