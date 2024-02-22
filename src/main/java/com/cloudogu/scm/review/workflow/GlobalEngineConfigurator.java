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

import sonia.scm.plugin.PluginLoader;
import sonia.scm.store.ConfigurationStore;
import sonia.scm.store.ConfigurationStoreFactory;

import jakarta.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;

public class GlobalEngineConfigurator extends EngineConfigurator {

  private static final String STORE_NAME = "workflow-engine";

  private final ConfigurationStoreFactory storeFactory;

  @Inject
  GlobalEngineConfigurator(AvailableRules availableRules, ConfigurationStoreFactory storeFactory, PluginLoader pluginLoader) {
    super(availableRules, pluginLoader.getUberClassLoader());
    this.storeFactory = storeFactory;
  }

  public GlobalEngineConfiguration getEngineConfiguration() {
    return withUberClassLoader(() -> createStore().getOptional().orElse(new GlobalEngineConfiguration()));
  }

  public void setEngineConfiguration(GlobalEngineConfiguration engineConfiguration) {
    List<AppliedRule> rules = engineConfiguration.getRules().stream().filter(r -> r.rule != null).collect(Collectors.toList());
    createStore().set(new GlobalEngineConfiguration(rules, engineConfiguration.isEnabled(), engineConfiguration.isDisableRepositoryConfiguration()));
  }

  public List<RuleInstance> getRules() {
    return getRules(getEngineConfiguration());
  }

  private ConfigurationStore<GlobalEngineConfiguration> createStore() {
    return storeFactory
      .withType(GlobalEngineConfiguration.class)
      .withName(STORE_NAME)
      .build();
  }
}
