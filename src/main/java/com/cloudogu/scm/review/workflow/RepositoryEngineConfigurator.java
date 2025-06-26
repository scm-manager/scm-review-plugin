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
import sonia.scm.repository.Repository;
import sonia.scm.store.ConfigurationStore;
import sonia.scm.store.ConfigurationStoreFactory;

import java.util.List;
import java.util.stream.Collectors;

public class RepositoryEngineConfigurator extends EngineConfigurator {

  private static final String STORE_NAME = "workflow-engine";

  private final ConfigurationStoreFactory storeFactory;

  @Inject
  RepositoryEngineConfigurator(AvailableRules availableRules, ConfigurationStoreFactory storeFactory, PluginLoader pluginLoader) {
    super(availableRules, pluginLoader.getUberClassLoader());
    this.storeFactory = storeFactory;
  }

  public EngineConfiguration getEngineConfiguration(Repository repository) {
    return withUberClassLoader(() -> createStore(repository).getOptional().orElse(new EngineConfiguration()));
  }

  public void setEngineConfiguration(Repository repository, EngineConfiguration engineConfiguration) {
    List<AppliedRule> rules = engineConfiguration.getRules().stream().filter(r -> r.rule != null).collect(Collectors.toList());
    createStore(repository).set(new EngineConfiguration(rules, engineConfiguration.isEnabled()));
  }

  public List<RuleInstance> getRules(Repository repository) {
    return getRules(getEngineConfiguration(repository));
  }

  private ConfigurationStore<EngineConfiguration> createStore(Repository repository) {
    return storeFactory
      .withType(EngineConfiguration.class)
      .withName(STORE_NAME)
      .forRepository(repository)
      .build();
  }
}
