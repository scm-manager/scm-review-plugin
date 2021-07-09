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
import React, { FC } from "react";
import { Autocomplete, InputField, TagGroup, Textarea } from "@scm-manager/ui-components";
import { useTranslation } from "react-i18next";
import { DisplayedUser, SelectValue } from "@scm-manager/ui-types";
import { useUserSuggestions } from "@scm-manager/ui-api";
import { PullRequest } from "./types/PullRequest";

type Props = {
  handleFormChange: (pr: PullRequest) => void;
  pullRequest: PullRequest;
};

const EditForm: FC<Props> = ({ handleFormChange, pullRequest }) => {
  const [t] = useTranslation("plugins");
  const userSuggestions = useUserSuggestions();

  const removeReviewer = (users: DisplayedUser[]) => {
    if (pullRequest.reviewer) {
      const newList = pullRequest.reviewer.filter(item => users.includes(item));
      handleFormChange({ ...pullRequest, reviewer: newList });
    }
  };

  const selectName = (selection: SelectValue) => {
    const newList = pullRequest.reviewer || [];
    newList.push({ id: selection.value.id, displayName: selection.value.displayName, mail: "", approved: false });
    handleFormChange({ ...pullRequest, reviewer: newList });
  };

  return (
    <>
      <InputField
        name="title"
        value={pullRequest?.title}
        label={t("scm-review-plugin.pullRequest.title")}
        validationError={pullRequest?.title === ""}
        errorMessage={t("scm-review-plugin.pullRequest.validation.title")}
        onChange={value => handleFormChange({ ...pullRequest, title: value })}
      />
      <Textarea
        name="description"
        value={pullRequest?.description}
        label={t("scm-review-plugin.pullRequest.description")}
        onChange={value => handleFormChange({ ...pullRequest, description: value })}
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
          />
        </div>
      </div>
    </>
  );
};
export default EditForm;
