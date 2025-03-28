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

import React, { ChangeEvent, FC, useCallback, useEffect, useState } from "react";
import { InputField, Textarea } from "@scm-manager/ui-components";
import { Checkbox } from "@scm-manager/ui-forms";
import { useTranslation } from "react-i18next";
import { useUserSuggestions } from "@scm-manager/ui-api";
import { PullRequest } from "./types/PullRequest";
import ReviewerInput from "./ReviewerInput";

type Entrypoint = "create" | "edit";

type Props = {
  handleFormChange: (pr: Partial<PullRequest>) => void;
  pullRequest: PullRequest;
  disabled?: boolean;
  availableLabels: string[];
  shouldDeleteSourceBranch: boolean;
};

const EditForm: FC<Props> = ({
  handleFormChange,
  pullRequest,
  disabled,
  availableLabels,
  shouldDeleteSourceBranch,
}) => {
  const [t] = useTranslation("plugins");
  const userSuggestions = useUserSuggestions();
  const [deleteSourceBranch, setDeleteSourceBranch] = useState<boolean>(shouldDeleteSourceBranch);
  const handleDeleteSourceBranch = useCallback(
    (pr: Partial<PullRequest>) => {
      setDeleteSourceBranch(pr.shouldDeleteSourceBranch || false);
      handleFormChange(pr);
    },
    [handleFormChange, deleteSourceBranch],
  );

  useEffect(() => {
    setDeleteSourceBranch(shouldDeleteSourceBranch);
  }, [shouldDeleteSourceBranch]);

  const handleLabelSelectChange = useCallback(
    (label: string, checked: boolean) => {
      if (checked) {
        handleFormChange({ labels: [...pullRequest.labels, label] });
      } else {
        handleFormChange({ labels: pullRequest.labels.filter((prLabel) => prLabel !== label) });
      }
    },
    [handleFormChange, pullRequest],
  );

  return (
    <>
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
        onChange={(value) => handleFormChange({ title: value })}
        disabled={disabled}
      />
      <Textarea
        name="description"
        value={pullRequest?.description}
        label={t("scm-review-plugin.pullRequest.description")}
        onChange={(value) => handleFormChange({ description: value })}
        disabled={disabled}
      />
      <ReviewerInput
        values={pullRequest?.reviewer?.map((reviewer) => reviewer.id) ?? []}
        onChange={(newValues) =>
          handleFormChange({
            reviewer: newValues.map((reviewerId) => ({ id: reviewerId, displayName: reviewerId, approved: false })),
          })
        }
        label={t("scm-review-plugin.pullRequest.reviewer")}
        placeholder={t("scm-review-plugin.pullRequest.addReviewer")}
        ariaLabel={t("scm-review-plugin.pullRequest.addReviewer")}
      />
      {availableLabels.length > 0 ? (
        <fieldset className="is-flex is-flex-direction-column mb-4">
          <legend className="label">{t("scm-review-plugin.pullRequest.labels")}</legend>
          {availableLabels.map((label) => (
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
