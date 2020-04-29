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

import com.cloudogu.scm.review.workflow.EngineConfigurator.RuleInstance;
import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.google.common.collect.ImmutableList.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EngineConfiguratorTest {

  private EngineConfigurator createEngineConfigurator(AvailableRules availableRules) {
    return new EngineConfigurator(availableRules, getClass().getClassLoader()) {

    };
  }

  @Test
  void shouldReturnEmptyListIfWorkflowEngineDisabled() {
    final EngineConfigurator engineConfigurator = createEngineConfigurator(AvailableRules.of(new SuccessRule()));
    final ImmutableList<AppliedRule> appliedRules = of(new AppliedRule(SuccessRule.class.getSimpleName()));
    EngineConfiguration configuration = new EngineConfiguration(appliedRules, false);
    List<RuleInstance> rules = engineConfigurator.getRules(configuration);

    assertThat(rules.isEmpty()).isTrue();
  }

  @Test
  void shouldInstantiateRules() {
    AvailableRules availableRules = AvailableRules.of(new SuccessRule(), new FailureRule());
    final EngineConfigurator engineConfigurator = createEngineConfigurator(availableRules);
    List<AppliedRule> appliedRules = of(FailureRule.class.getSimpleName(), SuccessRule.class.getSimpleName()).stream().map(AppliedRule::new).collect(Collectors.toList());

    EngineConfiguration configuration = new EngineConfiguration(appliedRules, true);
    List<RuleInstance> rules = engineConfigurator.getRules(configuration);

    assertThat(rules.size()).isEqualTo(2);
    assertThat(rules.get(0).getRule()).isInstanceOfAny(FailureRule.class, SuccessRule.class);
  }

  @Test
  void shouldThrowUnknownRuleException() {
    AvailableRules availableRules = AvailableRules.of(new SuccessRule());
    final EngineConfigurator engineConfigurator = createEngineConfigurator(availableRules);
    List<AppliedRule> appliedRules = of(FailureRule.class.getSimpleName(), SuccessRule.class.getSimpleName()).stream().map(AppliedRule::new).collect(Collectors.toList());

    EngineConfiguration configuration = new EngineConfiguration(appliedRules, true);
    assertThrows(UnknownRuleException.class, () -> engineConfigurator.getRules(configuration));
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
