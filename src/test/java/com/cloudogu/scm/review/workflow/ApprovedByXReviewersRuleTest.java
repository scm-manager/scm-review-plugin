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
import com.cloudogu.scm.review.workflow.ApprovedByXReviewersRule.Configuration;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryTestData;

import static sonia.scm.store.SerializationTestUtil.toAndFromJsonAndXml;
import static com.google.common.collect.ImmutableMap.of;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ApprovedByXReviewersRuleTest {

  private Repository repository;
  private PullRequest pullRequest;

  @InjectMocks
  private ApprovedByXReviewersRule rule;

  @BeforeEach
  void setUp() {
    pullRequest = TestData.createPullRequest();
    repository = RepositoryTestData.create42Puzzle();
  }

  @Test
  void shouldFailWithContextIfNotEnoughReviewersApproved() {
    pullRequest.setReviewer(of("Homer Simpson", false, "Totoro", true));
    Result result = rule.validate(new Context(repository, pullRequest, new Configuration(2)));
    assertThat(result.isFailed()).isTrue();
    assertThat(result.getContext()).isNotNull();
    assertThat(result.getContext()).isInstanceOf(ApprovedByXReviewersRule.ErrorContext.class);
    ApprovedByXReviewersRule.ErrorContext errorContext = (ApprovedByXReviewersRule.ErrorContext) result.getContext();
    assertThat(errorContext.getMissing()).isEqualTo(1);
    assertThat(errorContext.getActual()).isEqualTo(1);
    assertThat(errorContext.getExpected()).isEqualTo(2);
  }

  @Test
  void shouldSucceedIfEnoughReviewersApproved() {
    pullRequest.setReviewer(of("Homer Simpson", true));
    Result result = rule.validate(new Context(repository, pullRequest, new Configuration(1)));
    assertThat(result.isSuccess()).isTrue();
  }

  @Test
  void shouldSerializeAndDeserializeConfig() throws JsonProcessingException {
    final Configuration input = new Configuration(42);
    final Configuration output = toAndFromJsonAndXml(Configuration.class, input);
    assertThat(output).isNotNull();
    assertThat(output).isInstanceOf(Configuration.class);
    assertThat(output.getNumberOfReviewers()).isEqualTo(42);
  }
}
