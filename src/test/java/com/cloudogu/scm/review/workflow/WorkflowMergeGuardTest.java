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

import com.cloudogu.scm.review.pullrequest.service.MergeObstacle;
import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.repository.Repository;

import java.util.Collection;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkflowMergeGuardTest {

  private static final Repository REPOSITORY = new Repository("1", "git", "space", "X");
  private static final PullRequest PULL_REQUEST = new PullRequest("1-1", "feature", "develop");

  @Mock
  Engine engine;

  @InjectMocks
  WorkflowMergeGuard guard;

  @BeforeEach
  void prepareEngine() {
  }

  @Test
  void shouldReturnEmptyListWithoutRules() {
    when(engine.validate(REPOSITORY, PULL_REQUEST)).thenReturn(new Results(emptyList()));

    Collection<MergeObstacle> obstacles = guard.getObstacles(REPOSITORY, PULL_REQUEST);

    assertThat(obstacles).isEmpty();
  }

  @Test
  void shouldReturnEmptyListWithoutFailingRules() {
    when(engine.validate(REPOSITORY, PULL_REQUEST)).thenReturn(new Results(asList(Result.success(Rule.class))));

    Collection<MergeObstacle> obstacles = guard.getObstacles(REPOSITORY, PULL_REQUEST);

    assertThat(obstacles).isEmpty();
  }

  @Test
  void shouldReturnListWithObstacleForFailingRules() {
    when(engine.validate(REPOSITORY, PULL_REQUEST)).thenReturn(new Results(asList(Result.success(Rule.class), Result.failed(Rule.class))));

    Collection<MergeObstacle> obstacles = guard.getObstacles(REPOSITORY, PULL_REQUEST);

    assertThat(obstacles)
      .extracting("key")
      .containsExactly("workflow.rule.Rule.obstacle");
  }
}
