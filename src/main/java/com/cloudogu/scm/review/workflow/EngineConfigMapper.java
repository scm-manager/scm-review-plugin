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

public abstract class EngineConfigMapper<A extends EngineConfiguration, B extends RepositoryEngineConfigDto> extends BaseMapper<A, B> {

  @Inject
  AvailableRules availableRules;

  @Inject
  ConfigurationValidator configurationValidator;

  AppliedRuleDto map(AppliedRule appliedRule) {
    AppliedRuleDto dto = new AppliedRuleDto();
    dto.setRule(appliedRule.getRule());
    Rule rule = availableRules.ruleOf(appliedRule.getRule());
    if (rule.getConfigurationType().isPresent()) {
      dto.setConfiguration(new ObjectMapper().valueToTree(appliedRule.getConfiguration()));
    }
    return dto;
  }

  AppliedRule map(AppliedRuleDto dto) {
    AppliedRule appliedRule = new AppliedRule();
    Rule rule = availableRules.ruleOf(dto.getRule());
    rule.getConfigurationType()
      .ifPresent(configurationType -> appliedRule.configuration = parseConfiguration(dto, rule, configurationType));
    appliedRule.rule = dto.getRule();
    return appliedRule;
  }

  private Object parseConfiguration(AppliedRuleDto dto, Rule rule, Class<?> configurationType) {
    Object configuration;
    try {
      configuration = new ObjectMapper().treeToValue(dto.getConfiguration(), configurationType);
      configurationValidator.validate(configuration);
      return configuration;
    } catch (JsonProcessingException e) {
      throw new InvalidConfigurationException(rule, e);
    }
  }

}
