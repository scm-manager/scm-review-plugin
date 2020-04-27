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

import React, {FC, useEffect, useState} from "react";
import {AppliedRule, AvailableRules, EngineConfiguration, Rule} from "../types/EngineConfig";
import {useTranslation} from "react-i18next";
import {
  Checkbox,
  Title,
  Select,
  apiClient,
  Notification,
  AddButton,
  ErrorNotification,
  Loading,
  Level
} from "@scm-manager/ui-components";
import EngineConfigTable from "./EngineConfigTable";
import {Link} from "@scm-manager/ui-types";
import styled from "styled-components";
import {ExtensionPoint} from "@scm-manager/ui-extensions";

type Props = {
  onConfigurationChange: (config: EngineConfiguration, valid: boolean) => void;
  initialConfiguration: EngineConfiguration;
  global: boolean;
};

const AddRuleLevel = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;
`;

const RuleDetails = styled.p`
  flex: 1;
  padding: 1rem;
  background: #f5f5f5;
  border-radius: 4px;
`;

const EngineConfigEditor: FC<Props> = ({onConfigurationChange, initialConfiguration, global}) => {
  const [t] = useTranslation("plugins");

  const [config, setConfig] = useState(initialConfiguration);
  const [availableRules, setAvailableRules] = useState<Rule[]>([]);
  const [selectedRule, setSelectedRule] = useState("");
  const [ruleConfiguration, setRuleConfiguration] = useState<any>(undefined);
  const [ruleConfigurationValid, setRuleConfigurationValid] = useState(true);
  const [error, setError] = useState<Error | undefined>(undefined);
  const [loading, setLoading] = useState(false);

  const availableRulesHref = (config._links.availableRules as Link).href;

  useEffect(() => {
    setLoading(true);
    apiClient
      .get(availableRulesHref)
      .then(r => r.json() as Promise<AvailableRules>)
      .then(body => body.rules)
      .then(setAvailableRules)
      .then(() => setLoading(false))
      .catch(err => {
        setError(err);
        setLoading(false);
      });
  }, [availableRulesHref]);

  const onChangeDisableRepositoryConfiguration = () => {
    const newConfig = {...config, disableRepositoryConfiguration: !config.disableRepositoryConfiguration};
    setConfig(newConfig);
    onConfigurationChange(newConfig, true);
  };

  const onChangeToggleEnableEngine = () => {
    const newConfig = {...config, enabled: !config.enabled};
    setConfig(newConfig);
    onConfigurationChange(newConfig, true);
  };

  const addRuleToConfig = (value: AppliedRule) => {
    if (value) {
      const newConfig = {...config, rules: [...config.rules, value]};
      setConfig(newConfig);
      setSelectedRule("");
      onConfigurationChange(newConfig, true);
    }
  };

  const deleteRule = (rule: AppliedRule) => {
    const newRules = [...config.rules];
    const index = newRules.indexOf(rule);
    newRules.splice(index, 1);
    const newConfig = {...config, rules: newRules};
    setConfig(newConfig);
    onConfigurationChange(newConfig, true);
  };

  const selectRule = (value: string) => {
    setSelectedRule(value);
    setRuleConfiguration(null);
    setRuleConfigurationValid(true);
  };

  const options = [
    {label: "", value: ""},
    ...availableRules
      .filter(availableRule => availableRule.applicableMultipleTimes || !config.rules.find(r => r.rule === availableRule.name))
      .map(rule => ({label: t(`workflow.rule.${rule.name}.name`), value: rule.name}))
  ];

  const renderAddRuleForm = () => {
    if (loading) {
      return <Loading/>;
    }

    if (options.length > 1) {
      return (
        <Select
          label={t("scm-review-plugin.workflow.newRule.label")}
          helpText={t("scm-review-plugin.workflow.newRule.helpText")}
          onChange={selectRule}
          options={options}
        />
      );
    }

    return <Notification type={"info"}>{t("scm-review-plugin.workflow.noMoreRulesAvailable")}</Notification>;
  };

  if (error) {
    return <ErrorNotification error={error}/>;
  }

  const ruleConfigurationChanged = (newRuleConfiguration: any, valid: boolean) => {
    setRuleConfiguration(newRuleConfiguration);
    setRuleConfigurationValid(valid);
  }

  return (
    <>
      {global && (
        <>
          <Title title={t("scm-review-plugin.engineConfig.title")}/>
          <Checkbox
            checked={!!config.disableRepositoryConfiguration}
            onChange={onChangeDisableRepositoryConfiguration}
            label={t("scm-review-plugin.workflow.disableRepositoryConfiguration.label")}
            helpText={t("scm-review-plugin.workflow.disableRepositoryConfiguration.helpText")}
          />
        </>
      )}
      <Checkbox
        checked={config.enabled}
        onChange={onChangeToggleEnableEngine}
        label={t("scm-review-plugin.workflow.enableEngine.label")}
        helpText={t("scm-review-plugin.workflow.enableEngine.helpText")}
      />
      {config.enabled && (
        <>
          {config.rules.length > 0 ? (
            <EngineConfigTable configuration={config} deleteRule={deleteRule}/>
          ) : (
            <Notification type="info">{t("scm-review-plugin.workflow.noRulesConfigured", {a: "B"})}</Notification>
          )}

          {renderAddRuleForm()}
          {selectedRule && (
            <AddRuleLevel>
              <RuleDetails>
                <h4>{t("workflow.rule." + selectedRule + ".description", ruleConfiguration)}</h4>
                <ExtensionPoint
                  name={`reviewPlugin.workflow.config.${selectedRule}`}
                  renderAll={true}
                  props={{
                    configurationChanged: ruleConfigurationChanged
                  }}
                />
                <Level
                  right={
                    <AddButton
                      label={t("scm-review-plugin.workflow.addRule.label")}
                      action={() => addRuleToConfig({
                        rule: selectedRule,
                        configuration: ruleConfiguration
                      })}
                      disabled={!ruleConfigurationValid}
                    />
                  }
                />

              </RuleDetails>

            </AddRuleLevel>
          )}
        </>
      )}
    </>
  );
};

export default EngineConfigEditor;
