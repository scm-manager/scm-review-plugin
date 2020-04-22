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

import React, { FC, useEffect, useState } from "react";
import { EngineConfiguration } from "../types/EngineConfig";
import { useTranslation } from "react-i18next";
import { Checkbox, Title, Select, apiClient, Notification, AddButton } from "@scm-manager/ui-components";
import EngineConfigTable from "./EngineConfigTable";

type Props = {
  onConfigurationChange: (config: EngineConfiguration, valid: boolean) => void;
  initialConfiguration: EngineConfiguration;
  global: boolean;
};

const EngineConfigEditor: FC<Props> = ({ onConfigurationChange, initialConfiguration, global }) => {
  const [t] = useTranslation("plugins");

  const [config, setConfig] = useState(initialConfiguration);
  const [rules, setRules] = useState<string[]>([]);
  const [selectedRule, setSelectedRule] = useState("");

  useEffect(() => {
    //TODO add link to configuration
    //TODO fetch available rules
    setRules(["AllReviewerApprovedRule"]);
  }, []);

  const onChangeDisableRepositoryConfiguration = () => {
    setConfig({ ...config, disableRepositoryConfiguration: !config.disableRepositoryConfiguration });
    onConfigurationChange(config, true);
  };

  const onChangeToggleEnableEngine = () => {
    setConfig({ ...config, enabled: !config.enabled });
    onConfigurationChange(config, true);
  }

  const addRuleToConfig = (value: string) => {
    if (value) {
      setConfig({ ...config, rules: [...config.rules, value] });
      setSelectedRule("");
      onConfigurationChange(config, true);
    }
  };

  const deleteRule = (rule: string) => {
    const newRules = [...config.rules];
    const index = newRules.indexOf(rule);
    newRules.splice(index, 1);
    setConfig({ ...config, rules: newRules });
  };

  const selectRule = (value: string) => {
    setSelectedRule(value);
  };

  const options = [
    { label: "", value: "" },
    ...rules
      .filter(rule => !config.rules.includes(rule))
      .map(rule => ({ label: t("scm-review-plugin.workflow.rule.name." + rule), value: rule }))
  ];

  return (
    <>
      {global && (
        <>
          <Title title={t("scm-review-plugin.engineConfig.title")} />
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
          <EngineConfigTable configuration={config} deleteRule={deleteRule} />
          {options.length > 1 ? (
            <Select
              label={"scm-review-plugin.workflow.addNewRule.label"}
              helpText={"scm-review-plugin.workflow.addNewRule.helpText"}
              onChange={selectRule}
              options={options}
            />
          ) : (
            <Notification type={"info"}>{t("scm-review-plugin.workflow.noMoreRulesAvailable")}</Notification>
          )}
          {selectedRule && (
            <div>
              {t("scm-review-plugin.workflow.rule.description." + selectedRule)}
              <AddButton
                label={t("scm-review-plugin.workflow.addRule.title")}
                action={() => addRuleToConfig(selectedRule)}
              />
            </div>
          )}
        </>
      )}
    </>
  );
};

export default EngineConfigEditor;
