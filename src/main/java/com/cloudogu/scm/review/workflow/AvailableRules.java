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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import sonia.scm.plugin.PluginLoader;

import javax.inject.Inject;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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

  public List<String> getRuleNames() {
    return rules.stream().map(AvailableRules::nameOf).collect(Collectors.toList());
  }

  public Class<? extends Rule> classOf(String name) {
    // TODO create UnknownRuleException or something ...
    return rules.stream()
      .map(Rule::getClass)
      .filter(ruleClass -> ruleClass.getSimpleName().equals(name))
      .findFirst()
      .orElseThrow(() -> new RuleConfigurationException("unknown rule: " + name));
  }

  public Rule ruleOf(String name) {
    return rules.stream()
      .filter(rule -> rule.getClass().getSimpleName().equals(name))
      .findFirst()
      .orElseThrow(() -> new RuleConfigurationException("unknown rule: " + name));
  }

  public static String nameOf(Rule rule) {
    return nameOf(rule.getClass());
  }

  public static String nameOf(Class<? extends Rule> ruleClass) {
    return ruleClass.getSimpleName();
  }

}
