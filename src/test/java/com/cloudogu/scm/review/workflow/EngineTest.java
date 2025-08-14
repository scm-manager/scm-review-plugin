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
import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryTestData;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EngineTest {

  private static final Repository REPOSITORY = RepositoryTestData.createHeartOfGold();
  private static final PullRequest PULL_REQUEST = TestData.createPullRequest();

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private RepositoryEngineConfigurator repositoryEngineConfigurator;
  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private GlobalEngineConfigurator globalEngineConfigurator;

  @InjectMocks
  private Engine engine;

  @Test
  void shouldUseLocalConfig() {
    when(globalEngineConfigurator.getEngineConfiguration().isDisableRepositoryConfiguration()).thenReturn(false);
    when(repositoryEngineConfigurator.getEngineConfiguration(REPOSITORY).isEnabled()).thenReturn(true);

    engine.validate(REPOSITORY, PULL_REQUEST);

    verify(repositoryEngineConfigurator).getRules(REPOSITORY);
  }

  @Test
  void shouldUseGlobalConfigIfLocalConfigDisabled() {
    when(repositoryEngineConfigurator.getEngineConfiguration(REPOSITORY).isEnabled()).thenReturn(false);
    when(globalEngineConfigurator.getEngineConfiguration().isEnabled()).thenReturn(true);

    engine.validate(REPOSITORY, PULL_REQUEST);

    verify(globalEngineConfigurator).getRules();
  }

  @Test
  void shouldUseGlobalConfigIfLocalConfigNotPermitted() {
    when(globalEngineConfigurator.getEngineConfiguration().isDisableRepositoryConfiguration()).thenReturn(true);
    when(repositoryEngineConfigurator.getEngineConfiguration(REPOSITORY).isEnabled()).thenReturn(true);
    when(globalEngineConfigurator.getEngineConfiguration().isEnabled()).thenReturn(true);

    engine.validate(REPOSITORY, PULL_REQUEST);

    verify(globalEngineConfigurator).getRules();
  }

  @Test
  void shouldReturnEmptyListIfBothConfigsNotEnabled() {
    when(repositoryEngineConfigurator.getEngineConfiguration(REPOSITORY).isEnabled()).thenReturn(false);
    when(globalEngineConfigurator.getEngineConfiguration().isEnabled()).thenReturn(false);

    engine.validate(REPOSITORY, PULL_REQUEST);

    verify(repositoryEngineConfigurator, never()).getRules(REPOSITORY);
    verify(globalEngineConfigurator, never()).getRules();
  }

  @Test
  void shouldReturnSuccess() {
    EngineConfigurator.RuleInstance ruleInstance = new EngineConfigurator.RuleInstance(new SuccessRule(), null);
    EngineConfiguration config = createConfig();
    when(repositoryEngineConfigurator.getEngineConfiguration(REPOSITORY)).thenReturn(config);
    when(repositoryEngineConfigurator.getRules(REPOSITORY)).thenReturn(ImmutableList.of(ruleInstance));

    Results result = engine.validate(REPOSITORY, PULL_REQUEST);

    assertThat(result.isValid()).isTrue();
  }

  private EngineConfiguration createConfig() {
    return new EngineConfiguration(Collections.emptyList(), true);
  }

  @Test
  void shouldReturnFailed() {
    EngineConfigurator.RuleInstance ruleInstance = new EngineConfigurator.RuleInstance(new FailedRule(), null);
    EngineConfiguration config = createConfig();
    when(repositoryEngineConfigurator.getEngineConfiguration(REPOSITORY)).thenReturn(config);
    when(repositoryEngineConfigurator.getRules(REPOSITORY)).thenReturn(ImmutableList.of(ruleInstance));

    Results result = engine.validate(REPOSITORY, PULL_REQUEST);

    assertThat(result.isValid()).isFalse();
  }

  public static class SuccessRule implements Rule {

    @Override
    public Result validate(Context context) {
      return success();
    }
  }

  public static class FailedRule implements Rule {

    @Override
    public Result validate(Context context) {
      return failed();
    }
  }
}
