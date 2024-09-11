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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import sonia.scm.api.v2.resources.BaseMapper;

import jakarta.inject.Inject;
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
