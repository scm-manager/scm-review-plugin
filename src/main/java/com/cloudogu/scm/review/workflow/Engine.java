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

import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import jakarta.inject.Inject;
import sonia.scm.repository.Repository;

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
