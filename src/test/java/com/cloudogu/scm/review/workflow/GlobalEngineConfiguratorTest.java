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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junitpioneer.jupiter.TempDirectory;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.plugin.PluginLoader;
import sonia.scm.store.ConfigurationStore;
import sonia.scm.store.ConfigurationStoreFactory;

import javax.inject.Inject;
import javax.xml.bind.JAXB;
import java.io.File;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@ExtendWith(TempDirectory.class)
class GlobalEngineConfiguratorTest {

  @Mock
  private PluginLoader pluginLoader;

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private ConfigurationStoreFactory configurationStoreFactory;

  private GlobalEngineConfigurator configurator;

  @BeforeEach
  void setupStore(@TempDirectory.TempDir Path directory) {
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

    assertThat(rules.isEmpty()).isTrue();
  }

  @Test
  void shouldReturnEmptyListIfNoConfigurationExistsYet() {
    EngineConfiguration engineConfiguration = configurator.getEngineConfiguration();

    assertThat(engineConfiguration.getRules().isEmpty()).isTrue();
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
