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
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.plugin.PluginLoader;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryTestData;
import sonia.scm.store.ConfigurationStore;
import sonia.scm.store.ConfigurationStoreFactory;

import jakarta.inject.Inject;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RepositoryEngineConfiguratorTest {

  private final Repository repository = RepositoryTestData.createHeartOfGold();

  private RepositoryEngineConfigurator configurator;

  @Mock
  private PluginLoader pluginLoader;

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private ConfigurationStoreFactory configurationStoreFactory;

  @BeforeEach
  void setupStore(@TempDir Path directory) {
    AvailableRules availableRules = AvailableRules.of(new RuleWithInjection(new ResultService()), new RuleWithConfiguration());
    ConfigurationStore<EngineConfiguration> store = new SimpleJaxbStore<>(EngineConfiguration.class, directory.resolve("store.xml").toFile(), new EngineConfiguration());
    when(configurationStoreFactory.withType(EngineConfiguration.class).withName(any()).forRepository(any(Repository.class)).build()).thenReturn(store);

    configurator = new RepositoryEngineConfigurator(availableRules, configurationStoreFactory, pluginLoader);
  }

  @Test
  void shouldCreateConfiguredRule() {
    configurator.setEngineConfiguration(repository, config(true));
    List<EngineConfigurator.RuleInstance> rules = configurator.getRules(repository);

    final EngineConfigurator.RuleInstance ruleInstance = rules.get(1);
    final Rule ruleWithConfiguration = ruleInstance.getRule();
    assertThat(ruleWithConfiguration.getClass()).isEqualTo(RuleWithConfiguration.class);
    assertThat(ruleWithConfiguration.validate(new Context(null, null, ruleInstance.getConfiguration())).isSuccess()).isTrue();
  }

  private EngineConfiguration config(boolean enabled) {
    return new EngineConfiguration(
      ImmutableList.of(
        new AppliedRule(RuleWithInjection.class.getSimpleName()),
        new AppliedRule(RuleWithConfiguration.class.getSimpleName(), new TestConfiguration(42))
      ), enabled);
  }

  @Test
  void shouldReturnEmptyListIfEngineDisabled() {
    configurator.setEngineConfiguration(repository, config(false));
    List<EngineConfigurator.RuleInstance> rules = configurator.getRules(repository);

    assertThat(rules).isEmpty();
  }

  @Test
  void shouldReturnEmptyListIfNoConfigurationExistYet() {
    EngineConfiguration engineConfiguration = configurator.getEngineConfiguration(repository);

    assertThat(engineConfiguration.getRules()).isEmpty();
  }

  @Test
  void shouldRemoveUnknownRulesOnStore() {
    ImmutableList<AppliedRule> rules = ImmutableList.of(new AppliedRule(null, null));
    configurator.setEngineConfiguration(repository, new EngineConfiguration(rules, true));

    List<EngineConfigurator.RuleInstance> storedRules = configurator.getRules(repository);

    assertThat(storedRules).isEmpty();
  }

  public static class RuleWithConfiguration implements Rule {
    @Override
    public Optional<Class<?>> getConfigurationType() {
      return Optional.of(TestConfiguration.class);
    }

    @Override
    public Result validate(Context context) {
      return ((TestConfiguration) context.getConfiguration()).number == 42 ? success() : failed();
    }
  }

  @Data
  @AllArgsConstructor
  @NoArgsConstructor
  @XmlRootElement
  public static class TestConfiguration {
    int number;
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
