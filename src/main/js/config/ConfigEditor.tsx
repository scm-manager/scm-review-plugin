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

import React, { FC, useCallback, useEffect, useMemo, useRef, useState } from "react";
import { Trans, useTranslation } from "react-i18next";
import { Icon, Subtitle, Title, Checkbox, SelectField, TextareaField } from "@scm-manager/ui-core";
import BranchList from "./BranchList";
import { Config, MERGE_STRATEGIES, MergeStrategy } from "../types/Config";
import BypassList from "./BypassList";
import DefaultTaskEditor from "./DefaultTaskEditor";
import { isEqual } from "lodash-es";
import LabelInput from "../LabelInput";
import ReviewerInput from "../ReviewerInput";

type Props = {
  onConfigurationChange: (config: State, valid: boolean) => void;
  initialConfiguration: Config;
  configType: "global" | "namespace" | "repository";
};

type State = Config;

const ConfigEditor: FC<Props> = ({ onConfigurationChange, initialConfiguration, configType }) => {
  const [t] = useTranslation("plugins");
  const [state, setState] = useState(initialConfiguration);
  const onChange = useCallback(<K extends keyof Config>(prop: K, val: Config[K]) => {
    setState((prevState) => ({
      ...prevState,
      [prop]: val,
    }));
  }, []);
  const hasChanges = useMemo(() => !isEqual(initialConfiguration, state), [initialConfiguration, state]);
  const onBeforeUnload = useCallback(
    (e: BeforeUnloadEvent) => {
      if (hasChanges) {
        e.preventDefault();
        e.returnValue = "";
        return "";
      }
    },
    [hasChanges],
  );

  useEffect(() => onConfigurationChange(state, true), [onConfigurationChange, state]);
  useEffect(() => {
    window.addEventListener("beforeunload", onBeforeUnload);
    return () => window.removeEventListener("beforeunload", onBeforeUnload);
  }, [onBeforeUnload]);

  const {
    restrictBranchWriteAccess,
    protectedBranchPatterns,
    branchProtectionBypasses,
    preventMergeFromAuthor,
    disableRepositoryConfiguration,
    overwriteParentConfig,
    defaultReviewers,
    deleteBranchOnMerge,
    defaultMergeStrategy,
    labels,
    overwriteDefaultCommitMessage,
    commitMessageTemplate,
  } = state;

  return (
    <>
      {/* TODO: add blocker again to prevent users from leaving page when changes were made but not saved */}
      {configType === "global" ? (
        <Title title={t("scm-review-plugin.config.title")} />
      ) : (
        <Subtitle>{t("scm-review-plugin.config.title")}</Subtitle>
      )}
      {configType !== "repository" ? (
        <Checkbox
          checked={!!disableRepositoryConfiguration}
          onChange={(event) => onChange("disableRepositoryConfiguration", event.target.checked)}
          label={
            configType === "global"
              ? t("scm-review-plugin.config.disableRepositoryAndNamespaceConfiguration.label")
              : t("scm-review-plugin.config.disableRepositoryConfiguration.label")
          }
          helpText={
            configType === "global"
              ? t("scm-review-plugin.config.disableRepositoryAndNamespaceConfiguration.helpText")
              : t("scm-review-plugin.config.disableRepositoryConfiguration.helpText")
          }
        />
      ) : null}
      {configType !== "global" ? (
        <Checkbox
          checked={!!overwriteParentConfig}
          onChange={(event) => onChange("overwriteParentConfig", event.target.checked)}
          label={
            configType === "namespace"
              ? t("scm-review-plugin.config.overwriteGlobal.label")
              : t("scm-review-plugin.config.overwriteAll.label")
          }
          helpText={
            configType === "namespace"
              ? t("scm-review-plugin.config.overwriteGlobal.helpText")
              : t("scm-review-plugin.config.overwriteAll.helpText")
          }
        />
      ) : null}
      {!!overwriteParentConfig || configType === "global" ? (
        <>
          <hr className="my-4" />
          <fieldset>
            <legend className="is-size-5 mb-4">{t("scm-review-plugin.config.legends.mergeDialog")}</legend>
            <SelectField
              options={MERGE_STRATEGIES.map((strategy) => ({
                label: t("scm-review-plugin.showPullRequest.mergeStrategies." + strategy),
                value: strategy,
              }))}
              onChange={(event) => onChange("defaultMergeStrategy", event.target.value as MergeStrategy)}
              label={t("scm-review-plugin.config.defaultMergeStrategy.label")}
              helpText={t("scm-review-plugin.config.defaultMergeStrategy.helpText")}
              value={defaultMergeStrategy}
            />
            <fieldset>
              <legend className="is-size-6 mb-1 has-text-weight-bold">
                {t("scm-review-plugin.config.legends.branchOptions")}
              </legend>
              <Checkbox
                checked={deleteBranchOnMerge}
                onChange={(event) => onChange("deleteBranchOnMerge", event.target.checked)}
                label={t("scm-review-plugin.showPullRequest.mergeModal.deleteSourceBranch.explanation")}
                helpText={t("scm-review-plugin.config.deleteBranchOnMerge.helpText")}
              />
            </fieldset>
            <Checkbox
              checked={overwriteDefaultCommitMessage}
              onChange={(event) => onChange("overwriteDefaultCommitMessage", event.target.checked)}
              label={t("scm-review-plugin.config.overwriteDefaultCommitMessage.label")}
              helpText={t("scm-review-plugin.config.overwriteDefaultCommitMessage.helpText")}
            />
            {overwriteDefaultCommitMessage && (
              <>
                <TextareaField
                  label={t("scm-review-plugin.config.commitMessageTemplate.label")}
                  descriptionText={t("scm-review-plugin.config.commitMessageTemplate.helpText")}
                  value={commitMessageTemplate ?? ""}
                  onChange={(event) => onChange("commitMessageTemplate", event.target.value)}
                />
                <span>
                  <Icon className="has-text-blue-light">info-circle</Icon>
                  <Trans t={t} i18nKey="scm-review-plugin.config.commitMessageTemplate.hint">
                    <a target="_blank" href="/scm/mustacheDocs" />
                  </Trans>
                </span>
              </>
            )}
          </fieldset>
          <hr className="my-4" />
          <fieldset className="is-flex is-flex-direction-column">
            <legend className="is-size-5 mb-2">{t("scm-review-plugin.config.legends.creation")}</legend>
            <LabelInput labels={labels} onChange={(newValues) => onChange("labels", newValues)} />
            <DefaultTaskEditor
              defaultTasks={state.defaultTasks}
              addTask={(newDefaultTask) =>
                setState({ ...state, defaultTasks: [...state.defaultTasks, newDefaultTask] })
              }
              editTask={(task: string, index: number) =>
                setState((prevState) => {
                  const newTasks = [...prevState.defaultTasks];
                  newTasks[index] = task;
                  return { ...prevState, defaultTasks: newTasks };
                })
              }
              removeTask={(defaultTask) =>
                setState({ ...state, defaultTasks: state.defaultTasks.filter((t) => t !== defaultTask) })
              }
            />
            <ReviewerInput
              values={defaultReviewers}
              onChange={(newValues) => onChange("defaultReviewers", newValues)}
              label={t("scm-review-plugin.config.defaultReviewers.label")}
              placeholder={t("scm-review-plugin.config.defaultReviewers.placeholder")}
              ariaLabel={t("scm-review-plugin.config.defaultReviewers.placeholder")}
              information={t("scm-review-plugin.config.defaultReviewers.information")}
            />
          </fieldset>
          <hr className="my-4" />
          <fieldset>
            <legend className="is-size-5 mb-4">{t("scm-review-plugin.config.legends.restrictions")}</legend>
            <Checkbox
              checked={preventMergeFromAuthor}
              onChange={(event) => onChange("preventMergeFromAuthor", event.target.checked)}
              label={t("scm-review-plugin.config.preventMergeFromAuthor.label")}
              helpText={t("scm-review-plugin.config.preventMergeFromAuthor.helpText")}
            />
          </fieldset>
          <fieldset>
            <Checkbox
              checked={restrictBranchWriteAccess}
              onChange={(event) => onChange("restrictBranchWriteAccess", event.target.checked)}
              label={t("scm-review-plugin.config.restrictBranchWriteAccess.label")}
            />
            {restrictBranchWriteAccess && (
              <>
                <hr />
                <Subtitle subtitle={t("scm-review-plugin.config.branchProtection.branches.subtitle")} />
                <p className="mb-4">{t("scm-review-plugin.config.branchProtection.branches.note")}</p>
                <BranchList
                  protections={protectedBranchPatterns}
                  onChange={(val) => onChange("protectedBranchPatterns", val)}
                />
                <Subtitle subtitle={t("scm-review-plugin.config.branchProtection.bypasses.subtitle")} />
                <p className="mb-4">{t("scm-review-plugin.config.branchProtection.bypasses.note")}</p>
                <BypassList
                  bypasses={branchProtectionBypasses}
                  onChange={(val) => onChange("branchProtectionBypasses", val)}
                />
              </>
            )}
          </fieldset>
        </>
      ) : null}
    </>
  );
};

export default ConfigEditor;
