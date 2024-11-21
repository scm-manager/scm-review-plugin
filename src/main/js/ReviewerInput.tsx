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

import React, { FC, useState } from "react";
import { useTranslation } from "react-i18next";
import { useUserOptions } from "@scm-manager/ui-api";
import { ChipInputField, Combobox } from "@scm-manager/ui-forms";
import { DisplayedUser } from "@scm-manager/ui-types";
import classNames from "classnames";

const ReviewerInput: FC<{
  values: string[];
  label: string;
  placeholder: string;
  ariaLabel: string;
  information?: string;
  onChange: (newValues: string[]) => void;
}> = ({ values, onChange, label, information, ariaLabel, placeholder }) => {
  const [t] = useTranslation("plugins");
  const duplicateWarning = t("scm-review-plugin.config.defaultReviewers.duplicateWarning");
  const userDoesNotExistWarning = t("scm-review-plugin.config.defaultReviewers.nonExistingUser");
  const [warning, setWarning] = useState<string | undefined>(undefined);
  const [query, setQuery] = useState("");
  const { data: userOptions, isLoading: userOptionsLoading } = useUserOptions(query);

  return (
    <ChipInputField<DisplayedUser>
      className="pb-4"
      value={values.map((id) => ({ value: { id, displayName: id, mail: "" }, label: id }))}
      onChange={(newValues) => {
        // Removal of an element also means a valid input, therefore the warning gets reset
        if (values.length > newValues.length) {
          setWarning(undefined);
        }

        if (newValues.length !== 0 && newValues[newValues.length - 1].isArbitraryValue) {
          setWarning(userDoesNotExistWarning);
        } else {
          setWarning(undefined);
        }

        onChange(newValues.map(({ value }) => value.id));
      }}
      label={label}
      placeholder={placeholder}
      aria-label={ariaLabel}
      information={information}
      isNewItemDuplicate={(a, b) => {
        if (a.value.id === b.value.id) {
          setWarning(duplicateWarning);
          return true;
        }

        return false;
      }}
      warning={warning}
    >
      <Combobox
        options={userOptions || []}
        className={classNames({ "is-loading": userOptionsLoading })}
        onQueryChange={setQuery}
      />
    </ChipInputField>
  );
};

export default ReviewerInput;
