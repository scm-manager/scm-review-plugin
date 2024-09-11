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

import com.cloudogu.scm.review.workflow.EngineConfigurator.RuleInstance;
import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;

import java.util.List;
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

    assertThat(rules).isEmpty();
  }

  @Test
  void shouldInstantiateRules() {
    AvailableRules availableRules = AvailableRules.of(new SuccessRule(), new FailureRule());
    final EngineConfigurator engineConfigurator = createEngineConfigurator(availableRules);
    List<AppliedRule> appliedRules = of(FailureRule.class.getSimpleName(), SuccessRule.class.getSimpleName()).stream().map(AppliedRule::new).collect(Collectors.toList());

    EngineConfiguration configuration = new EngineConfiguration(appliedRules, true);
    List<RuleInstance> rules = engineConfigurator.getRules(configuration);

    assertThat(rules).hasSize(2);
    assertThat(rules.get(0).getRule()).isInstanceOfAny(FailureRule.class, SuccessRule.class);
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
