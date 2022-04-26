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

import javax.inject.Inject;
import javax.xml.bind.annotation.XmlRootElement;
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
