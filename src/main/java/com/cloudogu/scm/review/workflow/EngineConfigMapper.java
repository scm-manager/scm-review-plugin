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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import sonia.scm.api.v2.resources.BaseMapper;

import javax.inject.Inject;
import java.lang.reflect.InvocationTargetException;
import java.util.Optional;

public abstract class EngineConfigMapper<A extends EngineConfiguration, B extends RepositoryEngineConfigDto> extends BaseMapper<A, B> {

  @Inject
  AvailableRules availableRules;

  @Inject
  ConfigurationValidator configurationValidator;

  AppliedRuleDto map(AppliedRule appliedRule) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
    AppliedRuleDto dto = new AppliedRuleDto();
    dto.setRule(appliedRule.getRule());
    Optional<Rule> rule = availableRules.ruleOf(appliedRule.getRule());
    if (rule.isPresent() && rule.get().getConfigurationType().isPresent()) {
      Object configuration = appliedRule.getConfiguration();
      if (configuration == null) {
        configuration = rule.get().getConfigurationType().get().getConstructor().newInstance();
      }
      dto.setConfiguration(new ObjectMapper().valueToTree(configuration));
    }
    return dto;
  }

  AppliedRule map(AppliedRuleDto dto) {
    AppliedRule appliedRule = new AppliedRule();
    Optional<Rule> rule = availableRules.ruleOf(dto.getRule());
    if (rule.isPresent()) {
      rule.get().getConfigurationType()
        .ifPresent(configurationType -> appliedRule.configuration = parseConfiguration(dto, rule.get(), configurationType));
      appliedRule.rule = dto.getRule();
    }
    return appliedRule;
  }

  private Object parseConfiguration(AppliedRuleDto dto, Rule rule, Class<?> configurationType) {
    Object configuration;
    try {
      configuration = new ObjectMapper().treeToValue(dto.getConfiguration(), configurationType);
      if (configuration == null) {
        configuration = rule.getConfigurationType().get().getConstructor().newInstance();
      }
      configurationValidator.validate(configuration);
      return configuration;
    } catch (JsonProcessingException | InvocationTargetException | InstantiationException | IllegalAccessException |
             NoSuchMethodException e) {
      throw new InvalidConfigurationException(rule, e);
    }
  }

}
