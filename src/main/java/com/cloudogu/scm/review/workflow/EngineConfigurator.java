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

import lombok.Getter;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public abstract class EngineConfigurator {

  private final AvailableRules availableRules;
  private final ClassLoader uberClassLoader;

  protected EngineConfigurator(AvailableRules availableRules, ClassLoader uberClassLoader) {
    this.availableRules = availableRules;
    this.uberClassLoader = uberClassLoader;
  }

  protected final <T> T withUberClassLoader(Supplier<T> runnable) {
    ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
    Thread.currentThread().setContextClassLoader(uberClassLoader);
    try {
      return runnable.get();
    } finally {
      Thread.currentThread().setContextClassLoader(contextClassLoader);
    }
  }

  protected final List<RuleInstance> getRules(EngineConfiguration configuration) {
    if (!configuration.isEnabled()) {
      return Collections.emptyList();
    }

    return configuration.getRules()
      .stream()
      .map(this::createRuleInstance)
      .filter(Optional::isPresent)
      .map(Optional::get)
      .collect(Collectors.toList());
  }

  private Optional<RuleInstance> createRuleInstance(AppliedRule appliedRule) {
    Optional<Rule> rule = availableRules.ruleOf(appliedRule.getRule());
    Object configuration = appliedRule.getConfiguration();
    return rule.map(r -> new RuleInstance(r, configuration));
  }

  @Getter
  static class RuleInstance {
    private final Rule rule;
    private final Object configuration;

    RuleInstance(Rule rule, Object configuration) {
      this.rule = rule;
      this.configuration = configuration;
    }
  }
}
