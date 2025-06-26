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

import jakarta.inject.Inject;
import sonia.scm.plugin.PluginLoader;
import sonia.scm.store.ConfigurationStore;
import sonia.scm.store.ConfigurationStoreFactory;

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
