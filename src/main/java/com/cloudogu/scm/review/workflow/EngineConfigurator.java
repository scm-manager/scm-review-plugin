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
import sonia.scm.store.ConfigurationStore;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


public class EngineConfigurator {

  private final Injector injector;
  private final ConfigurationStore<EngineConfiguration> store;

  EngineConfigurator(Injector injector, ConfigurationStore<EngineConfiguration> store) {
    this.injector = injector;
    this.store = store;
  }

  public void addRule(Class<? extends Rule> rule) {
    EngineConfiguration engineConfiguration = store.getOptional().orElse(new EngineConfiguration());
    engineConfiguration.getRules().add(rule);
    store.set(engineConfiguration);
  }

  public List<Rule> getRules() {
    Optional<EngineConfiguration> configuration = store.getOptional();

    return configuration
      .map(engineConfiguration -> engineConfiguration.getRules()
        .stream()
        .map(this::createRuleInstance)
        .collect(Collectors.toList()))
      .orElse(Collections.emptyList());

  }

  private Rule createRuleInstance(Class<? extends Rule> aClass) {

    return injector.getInstance(aClass);
  }

  public void removeRule(Class<? extends Rule> rule) {

  }
}