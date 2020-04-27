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
import com.google.inject.Guice;
import com.google.inject.Injector;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junitpioneer.jupiter.TempDirectory;
import sonia.scm.store.ConfigurationStore;

import javax.inject.Inject;
import javax.xml.bind.JAXB;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(TempDirectory.class)
class EngineConfiguratorTest {

  private EngineConfigurator configurator;

  @BeforeEach
  void setupStore(@TempDirectory.TempDir Path directory) {
    Injector injector = Guice.createInjector();
    AvailableRules availableRules = AvailableRules.of(new RuleWithInjection(new ResultService()), new RuleWithConfiguration());
    ConfigurationStore<EngineConfiguration> store = new SimpleJaxbStore(directory.resolve("store.xml").toFile());

    configurator = new EngineConfigurator(injector, availableRules, store, getClass().getClassLoader());
  }

  @Test
  void shouldCreateRule() {
    configurator.setEngineConfiguration(config(true));
    List<EngineConfigurator.RuleInstance> rules = configurator.getRules();

    final Rule ruleWithInjection = rules.get(0).getRule();
    assertThat(ruleWithInjection.getClass()).isEqualTo(RuleWithInjection.class);
    assertThat(ruleWithInjection.validate(null).isSuccess()).isTrue();
  }

  @Test
  void shouldCreateConfiguredRule() {
    configurator.setEngineConfiguration(config(true));
    List<EngineConfigurator.RuleInstance> rules = configurator.getRules();

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
    configurator.setEngineConfiguration(config(false));
    List<EngineConfigurator.RuleInstance> rules = configurator.getRules();

    assertThat(rules.isEmpty()).isTrue();
  }

  public static class SimpleJaxbStore implements ConfigurationStore<EngineConfiguration> {

    private final File file;

    public SimpleJaxbStore(File file) {
      this.file = file;
    }

    @Override
    public EngineConfiguration get() {
      return JAXB.unmarshal(file, EngineConfiguration.class);
    }

    @Override
    public void set(EngineConfiguration object) {
      JAXB.marshal(object, file);
    }
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
