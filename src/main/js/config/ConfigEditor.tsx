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
import React, { FC, useCallback, useEffect, useMemo, useState } from "react";
import { useTranslation, WithTranslation } from "react-i18next";
import { AutocompleteAddEntryToTableField, Checkbox, Subtitle, TagGroup, Title } from "@scm-manager/ui-components";
import BranchList from "./BranchList";
import { Config } from "../types/Config";
import BypassList from "./BypassList";
import { useUserSuggestions } from "@scm-manager/ui-api";
import { DisplayedUser } from "@scm-manager/ui-types";

type OnChangeFunction = <K extends keyof Config>(prop: K, val: Config[K]) => void;

const UserList: FC<{ values: string[]; onChange: OnChangeFunction }> = ({ values, onChange }) => {
  const [t] = useTranslation("plugins");
  const userSuggestions = useUserSuggestions();
  const userValues = useMemo(() => values.map(id => ({ id, displayName: id, mail: "" } as DisplayedUser)), [values]);

  return (
    <>
      <TagGroup
        items={userValues}
        onRemove={(newUsers: DisplayedUser[]) =>
          onChange(
            "defaultReviewers",
            newUsers.map(user => user.id)
          )
        }
        label={t("scm-review-plugin.config.defaultReviewers.label")}
      />
      <AutocompleteAddEntryToTableField
        addEntry={val => onChange("defaultReviewers", [...values, val.value.id])}
        buttonLabel={t("scm-review-plugin.config.defaultReviewers.addButton")}
        loadSuggestions={userSuggestions}
        placeholder={t("scm-review-plugin.config.defaultReviewers.placeholder")}
        loadingMessage={t("scm-review-plugin.config.defaultReviewers.loading")}
        noOptionsMessage={t("scm-review-plugin.config.defaultReviewers.noOptions")}
      />
    </>
  );
};

type Props = WithTranslation & {
  onConfigurationChange: (config: State, valid: boolean) => void;
  initialConfiguration: Config;
  global: boolean;
};

type State = Config;

const ConfigEditor: FC<Props> = ({ onConfigurationChange, initialConfiguration, global }) => {
  const [t] = useTranslation("plugins");
  const [state, setState] = useState(initialConfiguration);
  const onChange = useCallback(<K extends keyof Config>(prop: K, val: Config[K]) => {
    setState(prevState => ({
      ...prevState,
      [prop]: val
    }));
  }, []);

  useEffect(() => onConfigurationChange(state, true), [onConfigurationChange, state]);

  const {
    restrictBranchWriteAccess,
    protectedBranchPatterns,
    branchProtectionBypasses,
    preventMergeFromAuthor,
    disableRepositoryConfiguration,
    defaultReviewers
  } = state;

  return (
    <>
      {global && (
        <>
          <Title title={t("scm-review-plugin.config.title")} />
          <Checkbox
            checked={!!disableRepositoryConfiguration}
            onChange={val => onChange("disableRepositoryConfiguration", val)}
            label={t("scm-review-plugin.config.disableRepositoryConfiguration.label")}
            helpText={t("scm-review-plugin.config.disableRepositoryConfiguration.helpText")}
          />
        </>
      )}
      {!global && <Subtitle>{t("scm-review-plugin.config.title")}</Subtitle>}
      <UserList values={defaultReviewers} onChange={onChange} />
      <Checkbox
        checked={preventMergeFromAuthor}
        onChange={val => onChange("preventMergeFromAuthor", val)}
        label={t("scm-review-plugin.config.preventMergeFromAuthor.label")}
        helpText={t("scm-review-plugin.config.preventMergeFromAuthor.helpText")}
      />
      <Checkbox
        checked={restrictBranchWriteAccess}
        onChange={val => onChange("restrictBranchWriteAccess", val)}
        label={t("scm-review-plugin.config.restrictBranchWriteAccess.label")}
        helpText={t("scm-review-plugin.config.restrictBranchWriteAccess.helpText")}
      />
      {restrictBranchWriteAccess && (
        <>
          <hr />
          <Subtitle subtitle={t("scm-review-plugin.config.branchProtection.branches.subtitle")} />
          <p className="mb-4">{t("scm-review-plugin.config.branchProtection.branches.note")}</p>
          <BranchList branches={protectedBranchPatterns} onChange={val => onChange("protectedBranchPatterns", val)} />
          <Subtitle subtitle={t("scm-review-plugin.config.branchProtection.bypasses.subtitle")} />
          <p className="mb-4">{t("scm-review-plugin.config.branchProtection.bypasses.note")}</p>
          <BypassList bypasses={branchProtectionBypasses} onChange={val => onChange("branchProtectionBypasses", val)} />
        </>
      )}
    </>
  );
};

export default ConfigEditor;
