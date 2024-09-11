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
