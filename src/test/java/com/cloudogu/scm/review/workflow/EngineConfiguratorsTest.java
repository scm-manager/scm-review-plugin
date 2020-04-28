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

import com.google.common.collect.ImmutableList;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EngineConfiguratorsTest {

  private AvailableRules availableRules;
  private final Injector injector = Guice.createInjector();

  @Test
  void shouldReturnEmptyListIfConfigurationMissing() {
    availableRules = AvailableRules.of();
    List<Rule> rules = EngineConfigurators.getRules(injector, availableRules, Optional.empty());

    assertThat(rules.isEmpty()).isTrue();
  }

  @Test
  void shouldReturnEmptyListIfWorkflowEngineDisabled() {
    availableRules = AvailableRules.of(SuccessRule.class);
    EngineConfiguration configuration = new EngineConfiguration(ImmutableList.of(SuccessRule.class.getSimpleName()), false);
    List<Rule> rules = EngineConfigurators.getRules(injector, availableRules, Optional.of(configuration));

    assertThat(rules.isEmpty()).isTrue();
  }

  @Test
  void shouldInstantiateRules() {
    availableRules = AvailableRules.of(SuccessRule.class, FailureRule.class);

    EngineConfiguration configuration = new EngineConfiguration(ImmutableList.of(FailureRule.class.getSimpleName(), SuccessRule.class.getSimpleName()), true);
    List<Rule> rules = EngineConfigurators.getRules(injector, availableRules, Optional.of(configuration));

    assertThat(rules.size()).isEqualTo(2);
    assertThat(rules.get(0)).isInstanceOfAny(FailureRule.class, SuccessRule.class);
  }

  @Test
  void shouldThrowUnknownRuleException() {
    availableRules = AvailableRules.of(SuccessRule.class);

    EngineConfiguration configuration = new EngineConfiguration(ImmutableList.of(FailureRule.class.getSimpleName(), SuccessRule.class.getSimpleName()), true);
    assertThrows(UnknownRuleException.class, () -> EngineConfigurators.getRules(injector, availableRules, Optional.of(configuration)));
  }

  static class SuccessRule implements Rule {

    @Override
    public Result validate(Context context) {
      return success();
    }
  }

  static class FailureRule implements Rule {

    @Override
    public Result validate(Context context) {
      return failed();
    }
  }
}
