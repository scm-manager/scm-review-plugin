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
import React, { ChangeEvent, FC, useCallback, useEffect, useState } from "react";
import { InputField, Textarea } from "@scm-manager/ui-components";
import { ErrorNotification, Label, Select } from "@scm-manager/ui-core";
import { Checkbox, ChipInputField, Combobox } from "@scm-manager/ui-forms";
import { useTranslation } from "react-i18next";
import { useUserSuggestions } from "@scm-manager/ui-api";
import { PullRequest, Reviewer } from "./types/PullRequest";
import { Branch } from "@scm-manager/ui-types";

type Entrypoint = "create" | "edit";

type Props = {
  handleFormChange: (pr: Partial<PullRequest>) => void;
  pullRequest: PullRequest;
  disabled?: boolean;
  availableLabels: string[];
  shouldDeleteSourceBranch: boolean;
  entrypoint: Entrypoint;
  branches?: Branch[];
  branchesError?: Error;
  branchesLoading?: boolean;
  extension?: string;
};

const EditForm: FC<Props> = ({
  handleFormChange,
  pullRequest,
  disabled,
  availableLabels,
  shouldDeleteSourceBranch,
  entrypoint,
  branches,
  branchesLoading,
  branchesError
}) => {
  const [t] = useTranslation("plugins");
  const userSuggestions = useUserSuggestions();
  const [deleteSourceBranch, setDeleteSourceBranch] = useState<boolean>(
    entrypoint === "create" ? shouldDeleteSourceBranch : pullRequest.shouldDeleteSourceBranch
  );
  const handleDeleteSourceBranch = useCallback(
    (pr: Partial<PullRequest>) => {
      setDeleteSourceBranch(pr.shouldDeleteSourceBranch || false);
      handleFormChange(pr);
    },
    [handleFormChange, deleteSourceBranch]
  );

  useEffect(() => {
    setDeleteSourceBranch(shouldDeleteSourceBranch);
  }, [shouldDeleteSourceBranch]);

  const createOptions = () => {
    return branches?.map(branch => ({
      label: branch.name,
      value: branch.name
    })).filter(branch => branch.label !== pullRequest.source);
  };

  const handleLabelSelectChange = useCallback(
    (label: string, checked: boolean) => {
      if (checked) {
        handleFormChange({ labels: [...pullRequest.labels, label] });
      } else {
        handleFormChange({ labels: pullRequest.labels.filter(prLabel => prLabel !== label) });
      }
    },
    [handleFormChange, pullRequest]
  );

  if (branchesError) {
    return <ErrorNotification error={branchesError} />;
  }

  return (
    <>
      {entrypoint === "edit" ? (
        <div className="is-clipped">
          <Label>{t("scm-review-plugin.pullRequest.targetBranch")}</Label>
          <Select
            className=""
            name="target"
            options={createOptions() || []}
            onChange={event => handleFormChange({ target: event.target.value })}
            value={pullRequest?.target}
          />
        </div>
      ) : null}

      <Checkbox
        key={t("scm-review-plugin.showPullRequest.mergeModal.deleteSourceBranch.help")}
        checked={deleteSourceBranch}
        onChange={(event: ChangeEvent<HTMLInputElement>) =>
          handleDeleteSourceBranch({ shouldDeleteSourceBranch: event.target.checked })
        }
        label={t("scm-review-plugin.showPullRequest.mergeModal.deleteSourceBranch.help")}
        labelClassName="is-align-self-flex-start mb-4"
      />
      <InputField
        name="title"
        value={pullRequest?.title}
        label={t("scm-review-plugin.pullRequest.title")}
        validationError={pullRequest?.title === ""}
        errorMessage={t("scm-review-plugin.pullRequest.validation.title")}
        onChange={value => handleFormChange({ title: value })}
        disabled={disabled}
      />
      <Textarea
        name="description"
        value={pullRequest?.description}
        label={t("scm-review-plugin.pullRequest.description")}
        onChange={value => handleFormChange({ description: value })}
        disabled={disabled}
      />
      <ChipInputField<Reviewer>
        value={pullRequest?.reviewer?.map(reviewer => ({ label: reviewer.displayName, value: reviewer })) ?? []}
        onChange={newValue => handleFormChange({ reviewer: newValue.map(({ value }) => value) })}
        label={t("scm-review-plugin.pullRequest.reviewer")}
        placeholder={t("scm-review-plugin.pullRequest.addReviewer")}
        aria-label={t("scm-review-plugin.pullRequest.addReviewer")}
        isNewItemDuplicate={(a, b) => a.value.id === b.value.id}
        disabled={disabled}
      >
        <Combobox options={userSuggestions} />
      </ChipInputField>
      {availableLabels.length > 0 ? (
        <fieldset className="is-flex is-flex-direction-column mb-4">
          <legend className="label">{t("scm-review-plugin.pullRequest.labels")}</legend>
          {availableLabels.map(label => (
            <Checkbox
              key={label}
              checked={pullRequest?.labels.includes(label)}
              onChange={(event: ChangeEvent<HTMLInputElement>) => handleLabelSelectChange(label, event.target.checked)}
              label={label}
              labelClassName="is-align-self-flex-start"
            />
          ))}
        </fieldset>
      ) : null}
    </>
  );
};
export default EditForm;
