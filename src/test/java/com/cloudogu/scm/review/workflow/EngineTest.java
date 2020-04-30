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
