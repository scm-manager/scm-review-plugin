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
import {
  Checkbox,
  Title,
  Select,
  apiClient,
  Notification,
  AddButton,
  ErrorNotification,
  Loading
} from "@scm-manager/ui-components";
import EngineConfigTable from "./EngineConfigTable";
import { Link } from "@scm-manager/ui-types";

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
  const [error, setError] = useState<Error | undefined>(undefined);
  const [loading, setLoading] = useState(false);

  const availableRulesHref = (config._links.availableRules as Link).href;

  useEffect(() => {
    setLoading(true);
    apiClient
      .get(availableRulesHref)
      .then(r => r.json())
      .then(setRules)
      .then(() => setLoading(false))
      .catch(setError);
  }, [availableRulesHref]);

  const onChangeDisableRepositoryConfiguration = () => {
    const newConfig ={ ...config, disableRepositoryConfiguration: !config.disableRepositoryConfiguration };
    setConfig(newConfig);
    onConfigurationChange(newConfig, true);
  };

  const onChangeToggleEnableEngine = () => {
    const newConfig = { ...config, enabled: !config.enabled };
    setConfig(newConfig);
    onConfigurationChange(newConfig, true);
  };

  const addRuleToConfig = (value: string) => {
    if (value) {
      const newConfig = { ...config, rules: [...config.rules, value] };
      setConfig(newConfig);
      setSelectedRule("");
      onConfigurationChange(newConfig, true);
    }
  };

  const deleteRule = (rule: string) => {
    const newRules = [...config.rules];
    const index = newRules.indexOf(rule);
    newRules.splice(index, 1);
    const newConfig = { ...config, rules: newRules };
    setConfig(newConfig);
    onConfigurationChange(newConfig, true);
  };

  const selectRule = (value: string) => {
    setSelectedRule(value);
  };

  const options = [
    { label: "", value: "" },
    ...rules
      .filter(rule => !config.rules.includes(rule))
      .map(rule => ({ label: t("scm-review-plugin.workflow.rule." + rule + ".name"), value: rule }))
  ];

  const renderAddRuleForm = () => {
    if (loading) {
      return <Loading />;
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
    return <ErrorNotification error={error} />;
  }

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
          {renderAddRuleForm()}
          {selectedRule && (
            <div>
              {t("scm-review-plugin.workflow.rule." + selectedRule + ".description")}
              <AddButton
                label={t("scm-review-plugin.workflow.addRule.label")}
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
