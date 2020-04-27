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

import com.google.inject.Injector;
import lombok.Getter;
import sonia.scm.store.ConfigurationStore;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;


public class EngineConfigurator {

  private final Injector injector;
  private final AvailableRules availableRules;
  private final ConfigurationStore<EngineConfiguration> store;
  private final ClassLoader uberClassLoader;

  EngineConfigurator(Injector injector, AvailableRules availableRules, ConfigurationStore<EngineConfiguration> store, ClassLoader uberClassLoader) {
    this.injector = injector;
    this.availableRules = availableRules;
    this.store = store;
    this.uberClassLoader = uberClassLoader;
  }

  private <T> T withUberClassLoader(Supplier<T> runnable) {
    ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
    Thread.currentThread().setContextClassLoader(uberClassLoader);
    try {
      return runnable.get();
    } finally {
      Thread.currentThread().setContextClassLoader(contextClassLoader);
    }
  }

  public EngineConfiguration getEngineConfiguration() {
    Optional<EngineConfiguration> optional = withUberClassLoader(store::getOptional);
    return optional.orElse(new EngineConfiguration());
  }

  public void setEngineConfiguration(EngineConfiguration engineConfiguration) {
    store.set(engineConfiguration);
  }

  public List<RuleInstance> getRules() {
    Optional<EngineConfiguration> configuration = withUberClassLoader(store::getOptional);

    if (configuration.isPresent() && !configuration.get().isEnabled()) {
      return Collections.emptyList();
    }

    return configuration
      .map(engineConfiguration -> engineConfiguration.getRules()
        .stream()
        .map(this::createRuleInstance)
        .collect(Collectors.toList()))
      .orElse(Collections.emptyList());
  }

  private RuleInstance createRuleInstance(AppliedRule appliedRule) {
    Rule rule = availableRules.ruleOf(appliedRule.getRule());
    Object configuration = appliedRule.getConfiguration();
    return new RuleInstance(rule, configuration);
  }

  @Getter
  class RuleInstance {
    private final Rule rule;
    private final Object configuration;

    RuleInstance(Rule rule, Object configuration) {
      this.rule = rule;
      this.configuration = configuration;
    }
  }
}
