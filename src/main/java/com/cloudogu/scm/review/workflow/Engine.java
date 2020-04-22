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

import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import com.google.inject.Injector;
import sonia.scm.repository.Repository;
import sonia.scm.store.ConfigurationStore;
import sonia.scm.store.ConfigurationStoreFactory;

import javax.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;

public final class Engine {

  private static final String STORE_NAME = "workflow-engine";

  private final Injector injector;
  private final ConfigurationStoreFactory storeFactory;

  @Inject
  public Engine(Injector injector, ConfigurationStoreFactory storeFactory) {
    this.injector = injector;
    this.storeFactory = storeFactory;
  }

  public Results validate(Repository repository, PullRequest pullRequest) {
    List<Rule> rules = configure(repository).getRules();
    Context context = new Context(repository, pullRequest);
    List<Result> results = rules.stream().map(rule -> rule.validate(context)).collect(Collectors.toList());

    return new Results(results);
  }

  public EngineConfigurator configure(Repository repository) {
    ConfigurationStore<EngineConfiguration> store =
      storeFactory
        .withType(EngineConfiguration.class)
        .withName(STORE_NAME)
        .forRepository(repository)
        .build();
    return new EngineConfigurator(injector, store);
  }

}
