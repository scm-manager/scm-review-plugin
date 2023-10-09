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
import React, { ChangeEvent, FC, useCallback } from "react";
import { Autocomplete, InputField, TagGroup, Textarea } from "@scm-manager/ui-components";
import { Checkbox } from "@scm-manager/ui-forms";
import { useTranslation } from "react-i18next";
import { DisplayedUser, SelectValue } from "@scm-manager/ui-types";
import { useUserSuggestions } from "@scm-manager/ui-api";
import { PullRequest } from "./types/PullRequest";

type Props = {
  handleFormChange: (pr: Partial<PullRequest>) => void;
  pullRequest: PullRequest;
  disabled?: boolean;
  availableLabels: string[];
};

const EditForm: FC<Props> = ({ handleFormChange, pullRequest, disabled, availableLabels }) => {
  const [t] = useTranslation("plugins");
  const userSuggestions = useUserSuggestions();

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

  const removeReviewer = (users: DisplayedUser[]) => {
    if (pullRequest.reviewer) {
      const newList = pullRequest.reviewer.filter(item => users.includes(item));
      handleFormChange({ reviewer: newList });
    }
  };

  const selectName = (selection: SelectValue) => {
    const newList = pullRequest.reviewer || [];
    newList.push({ id: selection.value.id, displayName: selection.value.displayName || "", mail: "", approved: false });
    handleFormChange({ reviewer: newList });
  };

  return (
    <>
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
      <TagGroup
        items={pullRequest?.reviewer || []}
        label={t("scm-review-plugin.pullRequest.reviewer")}
        onRemove={removeReviewer}
      />
      <div className="field">
        <div className="control">
          <Autocomplete
            creatable={false}
            loadSuggestions={userSuggestions}
            valueSelected={selectName}
            placeholder={t("scm-review-plugin.pullRequest.addReviewer")}
            disabled={disabled}
          />
        </div>
      </div>
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
