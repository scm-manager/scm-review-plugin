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
import styled from "styled-components";
import { useGeneratedId } from "@scm-manager/ui-components";

const NoTextWrapLabel = styled.label`
  text-wrap: nowrap;
`;

type Props = {
  handleTypeChange: (p: string) => void;
  sortBy: string;
};

const SortSelector: FC<Props> = ({ handleTypeChange, sortBy }) => {
  const [t] = useTranslation("plugins");
  const types = ["ID_ASC", "ID_DESC", "STATUS_ASC", "STATUS_DESC", "LAST_MOD_ASC", "LAST_MOD_DESC"];
  const selectOptions = useMemo(
    () =>
      types.map(sortFilter => ({
        label: t(`scm-review-plugin.pullRequest.sortSelector.${sortFilter}`),
        value: sortFilter
      })),
    [t, types]
  );
  const selectId = useGeneratedId();

  return (
    <span className="is-flex">
      <NoTextWrapLabel className="is-flex is-align-items-center mr-2" htmlFor={selectId}>
        {t("scm-review-plugin.pullRequest.sortSelectorLabel")}
      </NoTextWrapLabel>
      <Select
        id={selectId}
        onChange={e => handleTypeChange(e.target.value)}
        value={sortBy ? sortBy : "LAST_MOD_ASC"}
        options={selectOptions}
      />
    </span>
  );
};

export default SortSelector;
