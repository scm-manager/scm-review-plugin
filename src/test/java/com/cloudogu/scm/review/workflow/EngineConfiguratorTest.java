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
 *
 */

package com.cloudogu.scm.review.workflow;

import com.google.inject.Guice;
import org.junit.jupiter.api.Test;
import sonia.scm.store.InMemoryConfigurationStore;

import javax.inject.Inject;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EngineConfiguratorTest {


  @Test
  void shouldCreateRuleWithInjection() {
    EngineConfigurator configurator = new EngineConfigurator(Guice.createInjector(), new InMemoryConfigurationStore<>());

    configurator.addRule(RuleWithInjection.class);
    List<Rule> rules = configurator.getRules();

    assertThat(rules.get(0).validate(null).isSuccess()).isTrue();
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
      return Result.success();
    }

  }

}