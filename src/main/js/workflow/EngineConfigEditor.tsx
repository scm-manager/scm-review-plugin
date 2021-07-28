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

import React, { FC, useState } from "react";
import { AppliedRule, EngineConfiguration } from "../types/EngineConfig";
import { useTranslation } from "react-i18next";
import {
  AddButton,
  Checkbox,
  ErrorNotification,
  Level,
  Loading,
  Notification,
  Select,
  Title
} from "@scm-manager/ui-components";
import EngineConfigTable from "./EngineConfigTable";
import styled from "styled-components";
import { ExtensionPoint } from "@scm-manager/ui-extensions";
import { useEngineConfigRules } from "./config";

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

const EngineConfigEditor: FC<Props> = ({ onConfigurationChange, initialConfiguration, global }) => {
  const [t] = useTranslation("plugins");
  const [config, setConfig] = useState(initialConfiguration);
  const [selectedRule, setSelectedRule] = useState("");
  const [ruleConfiguration, setRuleConfiguration] = useState<any>(undefined);
  const [ruleConfigurationValid, setRuleConfigurationValid] = useState(true);
  const { error, isLoading, data } = useEngineConfigRules(config);

  const onChangeDisableRepositoryConfiguration = () => {
    const newConfig = { ...config, disableRepositoryConfiguration: !config.disableRepositoryConfiguration };
    setConfig(newConfig);
    onConfigurationChange(newConfig, true);
  };

  const onChangeToggleEnableEngine = () => {
    const newConfig = { ...config, enabled: !config.enabled };
    setConfig(newConfig);
    onConfigurationChange(newConfig, true);
  };

  const addRuleToConfig = (value: AppliedRule) => {
    if (value) {
      const newConfig = { ...config, rules: [...config.rules, value] };
      setConfig(newConfig);
      setSelectedRule("");
      onConfigurationChange(newConfig, true);
    }
  };

  const deleteRule = (rule: AppliedRule) => {
    const newRules = [...config?.rules];
    const index = newRules.indexOf(rule);
    newRules.splice(index, 1);
    const newConfig = { ...config, rules: newRules };
    setConfig(newConfig);
    onConfigurationChange(newConfig, true);
  };

  const selectRule = (value: string) => {
    setSelectedRule(value);
    setRuleConfiguration(null);
    setRuleConfigurationValid(true);
  };

  const createOptions = () => {
    let options = [{ label: "", value: "" }];

    if (data) {
      options = [
        ...options,
        ...data?.rules
          .filter(
            availableRule =>
              availableRule.applicableMultipleTimes || !config.rules.find(r => r.rule === availableRule.name)
          )
          .sort((a, b) => bySortKey(a.name, b.name, t.bind(t)))
          .map(rule => ({ label: t(`workflow.rule.${rule.name}.name`), value: rule.name }))
      ];
    }
    return options;
  };

  const renderAddRuleForm = () => {
    if (isLoading) {
      return <Loading />;
    }

    const options = createOptions();

    if (options.length > 1) {
      return (
        <Select
          label={t("scm-review-plugin.workflow.newRule.label")}
          helpText={t("scm-review-plugin.workflow.newRule.helpText")}
          onChange={selectRule}
          options={options}
          value={selectedRule}
        />
      );
    }

    return <Notification type={"info"}>{t("scm-review-plugin.workflow.noMoreRulesAvailable")}</Notification>;
  };

  if (error) {
    return <ErrorNotification error={error} />;
  }

  const ruleConfigurationChanged = (newRuleConfiguration: any, valid: boolean) => {
    setRuleConfiguration(newRuleConfiguration);
    setRuleConfigurationValid(valid);
  };

  return (
    <>
      {global && (
        <>
          <Title title={t("scm-review-plugin.workflow.globalConfig.title")} />
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
            <EngineConfigTable configuration={config} deleteRule={deleteRule} />
          ) : (
            <Notification type="info">{t("scm-review-plugin.workflow.noRulesConfigured", { a: "B" })}</Notification>
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
                      action={() =>
                        addRuleToConfig({
                          rule: selectedRule,
                          configuration: ruleConfiguration
                        })
                      }
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

export const bySortKey = (
  ruleNameA: string,
  ruleNameB: string,
  translateFn: (key: string) => string | null
): number => {
  const [ruleASortKey, ruleATranslatedName, aSortValue] = getRuleSortKey(ruleNameA, translateFn);
  const [ruleBSortKey, ruleBTranslatedName, bSortValue] = getRuleSortKey(ruleNameB, translateFn);
  if (!aSortValue && !bSortValue) {
    return 0;
  } else if (!aSortValue) {
    return 1;
  } else if (!bSortValue) {
    return -1;
  } else if (ruleASortKey === ruleBSortKey && ruleATranslatedName !== null && ruleBTranslatedName !== null) {
    return ruleATranslatedName.localeCompare(ruleBTranslatedName);
  } else {
    return aSortValue.localeCompare(bSortValue);
  }
};

const getRuleSortKey = (
  name: string,
  translateFn: (key: string) => string | null
): [string | null, string | null, string | null, boolean] => {
  const sortKey = translateFn(`workflow.rule.${name}.sortKey`);
  const translatedName = translateFn(`workflow.rule.${name}.name`);
  const sortValue = sortKey || translatedName;
  return [sortKey, translatedName, sortValue, !!sortValue];
};

export default EngineConfigEditor;
