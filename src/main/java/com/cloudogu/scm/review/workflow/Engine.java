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
import sonia.scm.repository.Repository;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public final class Engine {

  private final RepositoryEngineConfigurator repositoryEngineConfigurator;
  private final GlobalEngineConfigurator globalEngineConfigurator;

  @Inject
  public Engine(RepositoryEngineConfigurator repositoryEngineConfigurator, GlobalEngineConfigurator globalEngineConfigurator) {
    this.repositoryEngineConfigurator = repositoryEngineConfigurator;
    this.globalEngineConfigurator = globalEngineConfigurator;
  }

  public Results validate(Repository repository, PullRequest pullRequest) {
    List<EngineConfigurator.RuleInstance> rules;
    if (shouldUseLocalRules(repository)) {
      rules = repositoryEngineConfigurator.getRules(repository);
    } else if (globalEngineConfigurator.getEngineConfiguration().isEnabled()) {
      rules = globalEngineConfigurator.getRules();
    } else {
      rules = new ArrayList<>();
    }
    List<Result> results = rules.stream().map(ruleInstance ->
      ruleInstance.getRule().validate(new Context(repository, pullRequest, ruleInstance.getConfiguration()))).collect(Collectors.toList());

    return new Results(results);
  }

  private boolean shouldUseLocalRules(Repository repository) {
    return repositoryEngineConfigurator.getEngineConfiguration(repository).isEnabled() && !globalEngineConfigurator.getEngineConfiguration().isDisableRepositoryConfiguration();
  }
}
