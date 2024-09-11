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

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.plugin.PluginLoader;
import sonia.scm.store.ConfigurationStore;
import sonia.scm.store.ConfigurationStoreFactory;

import jakarta.inject.Inject;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalEngineConfiguratorTest {

  @Mock
  private PluginLoader pluginLoader;

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private ConfigurationStoreFactory configurationStoreFactory;

  private GlobalEngineConfigurator configurator;

  @BeforeEach
  void setupStore(@TempDir Path directory) {
    AvailableRules availableRules = AvailableRules.of(new RepositoryEngineConfiguratorTest.RuleWithInjection(new RepositoryEngineConfiguratorTest.ResultService()), new RepositoryEngineConfiguratorTest.RuleWithConfiguration());
    ConfigurationStore<GlobalEngineConfiguration> store = new SimpleJaxbStore<>(GlobalEngineConfiguration.class, directory.resolve("store.xml").toFile(), new GlobalEngineConfiguration());
    when(configurationStoreFactory.withType(GlobalEngineConfiguration.class).withName(any()).build()).thenReturn(store);

    configurator = new GlobalEngineConfigurator(availableRules, configurationStoreFactory, pluginLoader);
  }

  @Test
  void shouldCreateRuleWithInjection() {
    configurator.setEngineConfiguration(config(true));
    List<EngineConfigurator.RuleInstance> rules = configurator.getRules();

    assertThat(rules.get(0).getRule().validate(null).isSuccess()).isTrue();
  }

  private GlobalEngineConfiguration config(boolean enabled) {
    return new GlobalEngineConfiguration(ImmutableList.of(new AppliedRule(RuleWithInjection.class.getSimpleName())), enabled, false);
  }

  @Test
  void shouldReturnEmptyListIfEngineDisabled() {
    configurator.setEngineConfiguration(config(false));
    List<EngineConfigurator.RuleInstance> rules = configurator.getRules();

    assertThat(rules).isEmpty();
  }

  @Test
  void shouldReturnEmptyListIfNoConfigurationExistsYet() {
    EngineConfiguration engineConfiguration = configurator.getEngineConfiguration();

    assertThat(engineConfiguration.getRules()).isEmpty();
  }

  @Test
  void shouldRemoveUnknownRulesOnStore() {
    ImmutableList<AppliedRule> rules = ImmutableList.of(new AppliedRule(null, null));
    configurator.setEngineConfiguration(new GlobalEngineConfiguration(rules, true, true));

    List<EngineConfigurator.RuleInstance> storedRules = configurator.getRules();

    assertThat(storedRules).isEmpty();
  }

  public static class RuleWithInjection implements Rule {

    private final ResultService service;

    @Inject
    public RuleWithInjection(ResultService service) {
      this.service = service;
    }

    @Override
    public Result validate(Context context) {
      return service.getResult();
    }
  }

  public static class ResultService {

    public Result getResult() {
      return Result.success(RuleWithInjection.class);
    }
  }
}
