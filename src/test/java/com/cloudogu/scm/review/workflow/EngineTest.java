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

import com.cloudogu.scm.review.TestData;
import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import com.google.common.collect.ImmutableList;
import com.google.inject.Guice;
import org.assertj.core.api.Fail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryTestData;
import sonia.scm.store.InMemoryConfigurationStoreFactory;

import static org.assertj.core.api.Assertions.assertThat;

class EngineTest {

  private static final Repository REPOSITORY = RepositoryTestData.createHeartOfGold();
  private static final PullRequest PULL_REQUEST = TestData.createPullRequest();

  private Engine engine;

  @BeforeEach
  void setupEngine() {
    InMemoryConfigurationStoreFactory storeFactory = new InMemoryConfigurationStoreFactory();
    AvailableRules rules = AvailableRules.of(SuccessRule.class, FailedRule.class);
    engine = new Engine(Guice.createInjector(), rules, storeFactory);
  }

  @Test
  void shouldReturnSuccess() {
    EngineConfigurator configurator = engine.configure(REPOSITORY);
    configurator.setEngineConfiguration(config(SuccessRule.class));

    Results result = engine.validate(REPOSITORY, PULL_REQUEST);

    assertThat(result.isValid()).isTrue();
  }

  private EngineConfiguration config(Class<? extends Rule> rule) {
    return new EngineConfiguration(ImmutableList.of(rule.getSimpleName()), true);
  }

  @Test
  void shouldReturnFailed() {
    EngineConfigurator configurator = engine.configure(REPOSITORY);
    configurator.setEngineConfiguration(config(FailedRule.class));

    Results result = engine.validate(REPOSITORY, PULL_REQUEST);

    assertThat(result.isValid()).isFalse();
  }

  public static class SuccessRule implements Rule {

    @Override
    public Result validate(Context context) {
      return success();
    }
  }

  public static class FailedRule implements Rule {

    @Override
    public Result validate(Context context) {
      return failed();
    }
  }

}
