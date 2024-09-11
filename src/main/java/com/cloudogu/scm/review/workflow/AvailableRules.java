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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;

import jakarta.inject.Inject;
import java.util.Optional;
import java.util.Set;

public class AvailableRules {

  private final Set<Rule> rules;

  @Inject
  public AvailableRules(Set<Rule> rules) {
    this.rules = rules;
  }

  @SafeVarargs
  @VisibleForTesting
  static AvailableRules of(Rule... rules) {
    return new AvailableRules(ImmutableSet.copyOf(rules));
  }

  public Optional<Rule> ruleOf(String name) {
    return rules.stream()
      .filter(rule -> rule.getClass().getSimpleName().equals(name))
      .findFirst();
  }

  public static String nameOf(Rule rule) {
    return nameOf(rule.getClass());
  }

  public static String nameOf(Class<? extends Rule> ruleClass) {
    return ruleClass.getSimpleName();
  }

}
