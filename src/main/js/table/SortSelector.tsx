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
import { useTranslation } from "react-i18next";
import { Select } from "@scm-manager/ui-forms";

type Props = {
  handleTypeChange: (p: string) => void;
  sortBy: string;
  label?: string;
  helpText?: string;
  loading?: boolean;
};

const SortSelector: FC<Props> = ({ handleTypeChange, sortBy, label, helpText, loading }) => {
  const [t] = useTranslation("plugins");
  const types = ["ID_ASC", "ID_DESC", "STATUS_ASC", "STATUS_DESC", "LAST_MOD_ASC", "LAST_MOD_DESC"];

  const createSelectOptions = () => {
    return types.map(sortFilter => {
      return {
        label: t(`scm-review-plugin.pullRequest.sortSelector.${sortFilter}`),
        value: sortFilter
      };
    });
  };

  return (
    <>
      <label className="is-flex is-align-items-center">{t("scm-review-plugin.pullRequest.sortSelectorLabel")}</label>
      <Select
        onChange={handleTypeChange}
        value={sortBy ? sortBy : "LAST_MOD_ASC"}
        options={createSelectOptions()}
        loading={loading}
        label={label}
        helpText={helpText}
      />
    </>
  );
};

export default SortSelector;
