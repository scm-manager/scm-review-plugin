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

import React, { FC, useRef, useState } from "react";
import { useTranslation } from "react-i18next";
import { ChipInputField } from "@scm-manager/ui-forms";

const LabelInput: FC<{ labels: string[]; onChange: (newValues: string[]) => void }> = ({ labels, onChange }) => {
  const [t] = useTranslation("plugins");
  const chipInputRef = useRef<HTMLInputElement>(null);
  const duplicateWarning = t("scm-review-plugin.config.availableLabels.duplicateWarning");
  const [warning, setWarning] = useState<string | undefined>(undefined);

  return (
    <>
      <ChipInputField
        value={labels.map((label) => ({ value: label, label }))}
        onChange={(newValues) => {
          setWarning(undefined);
          onChange(newValues.map(({ value }) => value));
        }}
        label={t("scm-review-plugin.config.availableLabels.label")}
        placeholder={t("scm-review-plugin.config.availableLabels.placeholder")}
        aria-label={t("scm-review-plugin.config.availableLabels.ariaLabel")}
        ref={chipInputRef}
        information={t("scm-review-plugin.config.availableLabels.information")}
        isNewItemDuplicate={(a, b) => {
          if (a.value === b.value) {
            setWarning(duplicateWarning);
            return true;
          }

          return false;
        }}
        warning={warning}
      />
      <ChipInputField.AddButton inputRef={chipInputRef} className="is-align-self-flex-end">
        {t("scm-review-plugin.config.availableLabels.addButton.label")}
      </ChipInputField.AddButton>
    </>
  );
};

export default LabelInput;
