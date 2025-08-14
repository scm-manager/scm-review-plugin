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

import React, { FC, useMemo } from "react";
import { useTranslation } from "react-i18next";
import { Select } from "@scm-manager/ui-forms";
import { useGeneratedId } from "@scm-manager/ui-components";

type Props = {
  handleTypeChange: (p: string) => void;
  status: string;
  className?: string;
};

const StatusSelector: FC<Props> = ({ handleTypeChange, status, className }) => {
  const [t] = useTranslation("plugins");
  const types = ["IN_PROGRESS", "MINE", "REVIEWER", "ALL", "REJECTED", "MERGED"];
  const selectOptions = useMemo(
    () =>
      types.map(singleStatus => ({
        label: t(`scm-review-plugin.pullRequest.selector.${singleStatus}`),
        value: singleStatus
      })),
    [t, types]
  );
  const selectId = useGeneratedId();

  return (
    <span className="is-flex">
      <label className="is-flex is-align-items-center mr-2" htmlFor={selectId}>
        {t("scm-review-plugin.pullRequest.selectorLabel")}
      </label>
      <Select
        id={selectId}
        onChange={e => handleTypeChange(e.target.value)}
        value={status ? status : "IN_PROGRESS"}
        options={selectOptions}
        className={className}
      />
    </span>
  );
};

export default StatusSelector;
